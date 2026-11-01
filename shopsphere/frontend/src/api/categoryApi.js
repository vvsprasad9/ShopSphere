import axiosClient from './axiosClient';

export const categoryApi = {
  getCategoryHierarchy: async () => {
    const response = await axiosClient.get('/categories/tree');
    return response.data;
  },

  getAllCategories: async (activeOnly = false) => {
    const response = await axiosClient.get('/categories', { params: { activeOnly } });
    return response.data;
  },

  getCategoryById: async (id) => {
    const response = await axiosClient.get(`/categories/${id}`);
    return response.data;
  },

  getCategoryBySlug: async (slug) => {
    const response = await axiosClient.get(`/categories/slug/${encodeURIComponent(slug)}`);
    return response.data;
  },

  createCategory: async (categoryData) => {
    const response = await axiosClient.post('/categories', categoryData);
    return response.data;
  },

  updateCategory: async (id, categoryData) => {
    const response = await axiosClient.put(`/categories/${id}`, categoryData);
    return response.data;
  },

  deleteCategory: async (id) => {
    const response = await axiosClient.delete(`/categories/${id}`);
    return response.data;
  },
};

export default categoryApi;
