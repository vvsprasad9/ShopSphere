import axiosClient from './axiosClient';

export const wishlistApi = {
  getWishlist: async () => {
    const response = await axiosClient.get('/wishlist');
    return response.data;
  },

  addToWishlist: async (productId) => {
    const response = await axiosClient.post(`/wishlist/products/${productId}`);
    return response.data;
  },

  removeFromWishlist: async (productId) => {
    const response = await axiosClient.delete(`/wishlist/products/${productId}`);
    return response.data;
  },

  isWishlisted: async (productId) => {
    const response = await axiosClient.get(`/wishlist/products/${productId}/check`);
    return response.data;
  },
};

export default wishlistApi;
