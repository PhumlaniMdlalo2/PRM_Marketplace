import { api } from './client';

/**
 * Orders.
 *
 * Placement is server-driven: `checkoutCart` sends no line items, no prices and no buyer. The
 * server reads the cart it already holds, prices it, reserves stock, files the payment and empties
 * the cart, so the totals a user sees afterwards are the totals they are charged. The client chooses
 * only which of their own addresses to ship to and how they intend to pay; the server checks they
 * own the address, and decides the amount and the payer.
 */

export const listOrders = async () => {
  const { data } = await api.get('/orders');
  return data;
};

export const getOrder = async (id) => {
  const { data } = await api.get(`/orders/${id}`);
  return data;
};

/** The lines on one order. */
export const listOrderItems = async (orderId) => {
  const { data } = await api.get(`/order-items/order/${orderId}`);
  return data;
};

/**
 * Turns the stored cart into an order and files the payment attempt with it.
 *
 * @param {string|null} shippingAddressId one of the caller's own addresses, or null to skip
 * @param {string|null} paymentMethod 'CARD' | 'EFT' | 'WALLET', the backend enum names. Lowercase
 *   values are rejected by the enum binding, so these have to match exactly. Omitted means CARD.
 * @returns the placed order, or null when the server declined — an empty cart answers 400
 */
export const checkoutCart = async (shippingAddressId = null, paymentMethod = null) => {
  const params = {
    ...(shippingAddressId ? { shippingAddressId } : {}),
    ...(paymentMethod ? { paymentMethod } : {}),
  };
  const { data } = await api.post('/orders/checkout', null, {
    params: Object.keys(params).length ? params : undefined,
  });
  return data;
};

/** Buyer-side cancellation. The server rejects a move that is not legal from the current status. */
export const cancelOrder = async (id) => {
  await api.patch(`/orders/${id}/cancel`);
};

/** Seller or faculty advancing an order through its lifecycle. */
export const updateOrderStatus = async (id, status) => {
  const { data } = await api.patch(`/orders/${id}/status`, null, { params: { status } });
  return data;
};