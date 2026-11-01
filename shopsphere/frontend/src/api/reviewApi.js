import axiosClient from './axiosClient';

export const reviewApi = {
  createReview: async (productId, reviewData) => {
    const response = await axiosClient.post(`/products/${productId}/reviews`, reviewData);
    return response.data;
  },

  getProductReviews: async (productId, params = {}) => {
    const response = await axiosClient.get(`/products/${productId}/reviews`, { params });
    return response.data;
  },

  getProductRatingSummary: async (productId) => {
    const response = await axiosClient.get(`/products/${productId}/reviews/summary`);
    return response.data;
  },

  getCustomerReviews: async (params = {}) => {
    const response = await axiosClient.get('/reviews/my', { params });
    return response.data;
  },

  updateReview: async (reviewId, reviewData) => {
    const response = await axiosClient.put(`/reviews/${reviewId}`, reviewData);
    return response.data;
  },

  deleteReview: async (reviewId) => {
    const response = await axiosClient.delete(`/reviews/${reviewId}`);
    return response.data;
  },

  // Admin endpoints
  getAdminReviews: async (params = {}) => {
    const response = await axiosClient.get('/reviews/admin', { params });
    return response.data;
  },

  moderateReview: async (reviewId, moderationData) => {
    const response = await axiosClient.patch(`/reviews/admin/${reviewId}/moderation`, moderationData);
    return response.data;
  },
};

export default reviewApi;
