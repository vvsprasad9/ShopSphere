import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// Single-flight refresh token mutex & queue
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Request Interceptor: Attach JWT bearer token if available
axiosClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('shopsphere_access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Handle global errors and single-flight token refresh
axiosClient.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const originalRequest = error.config;

    // Handle 401 Unauthorized (token expired) and not already retried
    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      // Do not attempt refresh on auth endpoints (login, register, refresh itself)
      const isAuthEndpoint = originalRequest.url?.includes('/auth/login') ||
                             originalRequest.url?.includes('/auth/refresh-token') ||
                             originalRequest.url?.includes('/auth/register');

      if (isAuthEndpoint) {
        return Promise.reject(extractError(error));
      }

      if (isRefreshing) {
        // Queue this request until the in-flight refresh completes
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((newToken) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return axiosClient(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = localStorage.getItem('shopsphere_refresh_token');

      try {
        // The backend supports refresh token either via cookie or request body
        const res = await axios.post(
          `${API_BASE_URL}/auth/refresh-token`,
          refreshToken ? { refreshToken } : {},
          { withCredentials: true }
        );

        const newAccessToken = res.data?.data?.accessToken;
        const newRefreshToken = res.data?.data?.refreshToken;

        if (newAccessToken) {
          localStorage.setItem('shopsphere_access_token', newAccessToken);
          if (newRefreshToken) {
            localStorage.setItem('shopsphere_refresh_token', newRefreshToken);
          }
          if (res.data?.data?.user) {
            localStorage.setItem('shopsphere_user', JSON.stringify(res.data.data.user));
          }

          processQueue(null, newAccessToken);
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          return axiosClient(originalRequest);
        } else {
          throw new Error('No access token returned from refresh endpoint');
        }
      } catch (refreshErr) {
        processQueue(refreshErr, null);
        localStorage.removeItem('shopsphere_access_token');
        localStorage.removeItem('shopsphere_refresh_token');
        localStorage.removeItem('shopsphere_user');

        // Notify any listeners/stores to clear query cache
        window.dispatchEvent(new Event('shopsphere_auth_expired'));

        // Only redirect if in browser context and not on auth pages
        if (typeof window !== 'undefined' && window.location && !window.location.pathname.startsWith('/login')) {
          const currentPath = encodeURIComponent(window.location.pathname + window.location.search);
          window.location.href = `/login?expired=true&redirect=${currentPath}`;
        }
        return Promise.reject(extractError(refreshErr));
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(extractError(error));
  }
);

function extractError(error) {
  const status = error.response?.status;
  const data = error.response?.data;
  let message = data?.message || error.message || 'An unexpected error occurred';

  // Handle Bean validation field errors map
  if (data?.data && typeof data.data === 'object' && !Array.isArray(data.data)) {
    const fieldErrors = Object.entries(data.data)
      .map(([field, msg]) => `${field}: ${msg}`)
      .join(', ');
    if (fieldErrors) {
      message = `${message} (${fieldErrors})`;
    }
  }

  return {
    status,
    message,
    data,
  };
}

export default axiosClient;
