import axiosClient from './axiosClient';

export const orderApi = {
  previewCheckout: async (previewRequest = {}) => {
    const response = await axiosClient.post('/orders/checkout/preview', previewRequest);
    return response.data;
  },

  checkout: async (checkoutRequest, idempotencyKey = null) => {
    const headers = {};
    const key = idempotencyKey || checkoutRequest.idempotencyKey;
    if (key) {
      headers['Idempotency-Key'] = key;
    }

    const payload = {
      ...checkoutRequest,
      idempotencyKey: key,
    };

    const response = await axiosClient.post('/orders/checkout', payload, { headers });
    return response.data;
  },

  getCustomerOrders: async (params = {}) => {
    const response = await axiosClient.get('/orders', { params });
    return response.data;
  },

  getOrderById: async (id) => {
    const response = await axiosClient.get(`/orders/${id}`);
    return response.data;
  },

  cancelOrder: async (id, reason = null) => {
    const response = await axiosClient.post(`/orders/${id}/cancel`, reason ? { cancellationReason: reason } : {});
    return response.data;
  },

  trackOrder: async (orderNumber) => {
    const response = await axiosClient.get(`/orders/track/${encodeURIComponent(orderNumber)}`);
    return response.data;
  },

  // Admin endpoints
  getAllOrders: async (params = {}) => {
    const response = await axiosClient.get('/orders/admin', { params });
    return response.data;
  },

  updateOrderStatus: async (id, statusData) => {
    const response = await axiosClient.patch(`/orders/admin/${id}/status`, statusData);
    return response.data;
  },
};

export default orderApi;
