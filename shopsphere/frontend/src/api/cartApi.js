import axiosClient from './axiosClient';

export const cartApi = {
  getCart: async () => {
    const response = await axiosClient.get('/cart');
    return response.data;
  },

  addToCart: async ({ productId, variantId = null, quantity = 1 }) => {
    const response = await axiosClient.post('/cart/items', {
      productId,
      variantId,
      quantity,
    });
    return response.data;
  },

  updateCartItem: async (itemId, quantity) => {
    const response = await axiosClient.put(`/cart/items/${itemId}`, { quantity });
    return response.data;
  },

  removeCartItem: async (itemId) => {
    const response = await axiosClient.delete(`/cart/items/${itemId}`);
    return response.data;
  },

  clearCart: async () => {
    const response = await axiosClient.delete('/cart');
    return response.data;
  },
};

export default cartApi;
