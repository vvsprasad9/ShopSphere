import axiosClient from './axiosClient';

export const paymentApi = {
  verifyPayment: async (verificationData) => {
    const response = await axiosClient.post('/payments/verify', verificationData);
    return response.data;
  },

  processMockPayment: async ({ orderId, status = 'SUCCESS', failureReason = null }) => {
    const response = await axiosClient.post('/payments/mock-process', {
      orderId,
      status,
      failureReason,
    });
    return response.data;
  },

  getPaymentByOrderId: async (orderId) => {
    const response = await axiosClient.get(`/payments/order/${orderId}`);
    return response.data;
  },

  // Admin endpoints
  collectCodPayment: async (orderId) => {
    const response = await axiosClient.post(`/payments/admin/orders/${orderId}/collect-cod`);
    return response.data;
  },

  refundPayment: async (refundData) => {
    const response = await axiosClient.post('/payments/refund', refundData);
    return response.data;
  },
};

export default paymentApi;
