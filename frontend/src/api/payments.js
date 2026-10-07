import api from './client';

export const isPaymentSimulationEnabled = async () => {
  const { data } = await api.get('/payments/simulation');
  return data === true;
};

export const listPaymentsForOrder = async (orderId) => {
  const { data } = await api.get(`/payments/order/${orderId}`);
  return Array.isArray(data) ? data : [];
};

export const listPaymentInstructions = async (orderId) => {
  const { data } = await api.get(`/payments/order/${orderId}/instructions`);
  return Array.isArray(data) ? data : [];
};

export const listSellerPayments = async () => {
  const { data } = await api.get('/payments/seller');
  return Array.isArray(data) ? data : [];
};

export const confirmPaymentReceipt = async (id) => {
  const { data } = await api.post(`/payments/${id}/confirm-receipt`);
  return data;
};

export const updatePaymentStatus = async (id, status) => {
  const { data } = await api.patch(`/payments/${id}/status`, null, { params: { status } });
  return data;
};

export const simulatePayment = async (id, outcome) => {
  const { data } = await api.post(`/payments/${id}/simulate`, null, { params: { outcome } });
  return data;
};
