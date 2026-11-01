import axiosClient from './axiosClient';

const triggerFileDownload = (blob, filename) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.parentNode.removeChild(link);
  window.URL.revokeObjectURL(url);
};

export const analyticsApi = {
  getOverview: async (params = {}) => {
    return await axiosClient.get('/admin/analytics/overview', { params });
  },

  getSalesTrend: async (params = {}) => {
    return await axiosClient.get('/admin/analytics/sales', { params });
  },

  getProductPerformance: async (params = {}) => {
    return await axiosClient.get('/admin/analytics/products', { params });
  },

  getCategorySales: async (params = {}) => {
    return await axiosClient.get('/admin/analytics/categories', { params });
  },

  getInventoryHealth: async (params = {}) => {
    return await axiosClient.get('/admin/analytics/inventory', { params });
  },

  getCustomerAnalytics: async (params = {}) => {
    return await axiosClient.get('/admin/analytics/customers', { params });
  },

  getPaymentAnalytics: async (params = {}) => {
    return await axiosClient.get('/admin/analytics/payments', { params });
  },

  getCouponAnalytics: async (params = {}) => {
    return await axiosClient.get('/admin/analytics/coupons', { params });
  },

  getAuditLogs: async (params = {}) => {
    return await axiosClient.get('/admin/analytics/audit', { params });
  },

  // CSV Export helpers with blob streaming
  downloadSalesCsv: async (params = {}) => {
    const data = await axiosClient.get('/admin/analytics/export/sales', {
      params,
      responseType: 'blob',
    });
    const blob = new Blob([data], { type: 'text/csv;charset=utf-8;' });
    triggerFileDownload(blob, `sales_report_${new Date().toISOString().slice(0, 10)}.csv`);
  },

  downloadProductsCsv: async (params = {}) => {
    const data = await axiosClient.get('/admin/analytics/export/products', {
      params,
      responseType: 'blob',
    });
    const blob = new Blob([data], { type: 'text/csv;charset=utf-8;' });
    triggerFileDownload(blob, `product_performance_${new Date().toISOString().slice(0, 10)}.csv`);
  },

  downloadInventoryCsv: async (params = {}) => {
    const data = await axiosClient.get('/admin/analytics/export/inventory', {
      params,
      responseType: 'blob',
    });
    const blob = new Blob([data], { type: 'text/csv;charset=utf-8;' });
    triggerFileDownload(blob, `inventory_health_${new Date().toISOString().slice(0, 10)}.csv`);
  },

  downloadPaymentsCsv: async (params = {}) => {
    const data = await axiosClient.get('/admin/analytics/export/payments', {
      params,
      responseType: 'blob',
    });
    const blob = new Blob([data], { type: 'text/csv;charset=utf-8;' });
    triggerFileDownload(blob, `payments_report_${new Date().toISOString().slice(0, 10)}.csv`);
  },

  downloadCustomersCsv: async (params = {}) => {
    const data = await axiosClient.get('/admin/analytics/export/customers', {
      params,
      responseType: 'blob',
    });
    const blob = new Blob([data], { type: 'text/csv;charset=utf-8;' });
    triggerFileDownload(blob, `customers_report_${new Date().toISOString().slice(0, 10)}.csv`);
  },
};

export default analyticsApi;
