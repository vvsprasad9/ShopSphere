import axiosClient from './axiosClient';

export const authApi = {
  login: async (credentials) => {
    const response = await axiosClient.post('/auth/login', credentials);
    return response.data;
  },

  register: async (userData) => {
    const response = await axiosClient.post('/auth/register', userData);
    return response.data;
  },

  refreshToken: async (refreshToken) => {
    const response = await axiosClient.post('/auth/refresh-token', refreshToken ? { refreshToken } : {});
    return response.data;
  },

  logout: async (refreshToken) => {
    const response = await axiosClient.post('/auth/logout', refreshToken ? { refreshToken } : {});
    return response.data;
  },

  forgotPassword: async (email) => {
    const response = await axiosClient.post('/auth/forgot-password', { email });
    return response.data;
  },

  resetPassword: async (resetData) => {
    const response = await axiosClient.post('/auth/reset-password', resetData);
    return response.data;
  },

  verifyEmail: async (token) => {
    const response = await axiosClient.get(`/auth/verify-email?token=${encodeURIComponent(token)}`);
    return response.data;
  },

  getCurrentUser: async () => {
    const response = await axiosClient.get('/auth/me');
    return response.data;
  },
};

export default authApi;
