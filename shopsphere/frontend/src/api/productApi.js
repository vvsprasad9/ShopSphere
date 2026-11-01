import axiosClient from './axiosClient';

export const productApi = {
  searchProducts: async (params = {}) => {
    // Filter out null/undefined/empty string params
    const cleanParams = Object.entries(params).reduce((acc, [key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        acc[key] = value;
      }
      return acc;
    }, {});

    const response = await axiosClient.get('/products', { params: cleanParams });
    return response.data;
  },

  getProductById: async (id) => {
    const response = await axiosClient.get(`/products/${id}`);
    return response.data;
  },

  getProductBySlug: async (slug) => {
    const response = await axiosClient.get(`/products/slug/${encodeURIComponent(slug)}`);
    return response.data;
  },

  getFeaturedProducts: async (limit = 8) => {
    const response = await axiosClient.get('/products/featured', { params: { limit } });
    return response.data;
  },

  getRelatedProducts: async (id, limit = 8) => {
    const response = await axiosClient.get(`/products/${id}/related`, { params: { limit } });
    return response.data;
  },

  createProduct: async (productData) => {
    const response = await axiosClient.post('/products', productData);
    return response.data;
  },

  updateProduct: async (id, productData) => {
    const response = await axiosClient.put(`/products/${id}`, productData);
    return response.data;
  },

  deleteProduct: async (id) => {
    const response = await axiosClient.delete(`/products/${id}`);
    return response.data;
  },

  // Image operations
  addProductImage: async (productId, imageData) => {
    const response = await axiosClient.post(`/products/${productId}/images`, imageData);
    return response.data;
  },

  deleteProductImage: async (productId, imageId) => {
    const response = await axiosClient.delete(`/products/${productId}/images/${imageId}`);
    return response.data;
  },

  setPrimaryProductImage: async (productId, imageId) => {
    const response = await axiosClient.patch(`/products/${productId}/images/${imageId}/primary`);
    return response.data;
  },

  // Variant operations
  addProductVariant: async (productId, variantData) => {
    const response = await axiosClient.post(`/products/${productId}/variants`, variantData);
    return response.data;
  },

  updateProductVariant: async (productId, variantId, variantData) => {
    const response = await axiosClient.put(`/products/${productId}/variants/${variantId}`, variantData);
    return response.data;
  },

  deleteProductVariant: async (productId, variantId) => {
    const response = await axiosClient.delete(`/products/${productId}/variants/${variantId}`);
    return response.data;
  },
};

export default productApi;
