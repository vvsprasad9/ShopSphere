import axiosClient from './axiosClient';

export const userApi = {
  getProfile: async () => {
    const response = await axiosClient.get('/users/profile');
    return response.data;
  },

  updateProfile: async (profileData) => {
    const response = await axiosClient.put('/users/profile', profileData);
    return response.data;
  },

  changePassword: async (passwordData) => {
    const response = await axiosClient.put('/users/change-password', passwordData);
    return response.data;
  },
};

export default userApi;
