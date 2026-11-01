import axiosClient from './axiosClient';

export const addressApi = {
  getUserAddresses: async () => {
    const response = await axiosClient.get('/addresses');
    return response.data;
  },

  createAddress: async (addressData) => {
    const response = await axiosClient.post('/addresses', addressData);
    return response.data;
  },

  getAddressById: async (id) => {
    const response = await axiosClient.get(`/addresses/${id}`);
    return response.data;
  },

  updateAddress: async (id, addressData) => {
    const response = await axiosClient.put(`/addresses/${id}`, addressData);
    return response.data;
  },

  deleteAddress: async (id) => {
    const response = await axiosClient.delete(`/addresses/${id}`);
    return response.data;
  },

  setDefaultAddress: async (id) => {
    const response = await axiosClient.patch(`/addresses/${id}/default`);
    return response.data;
  },
};

export default addressApi;
