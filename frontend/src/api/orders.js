import { api } from './client';

/**
 * Orders.
 *
 * Placement is server-driven: `checkoutCart` sends no line items, no prices and no buyer. The
 * server reads the cart it already holds, prices it, reserves stock and empties the cart, so the
 * totals a user sees afterwards are the totals they are charged. The only thing a client chooses is
 * which of their own addresses to ship to, and the server checks they own it.
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
 * Turns the stored cart into an order.
 *
 * @param {string|null} shippingAddressId one of the caller's own addresses, or null to skip
 * @returns the placed order, or null when the server declined — an empty cart answers 400
 */
export const checkoutCart = async (shippingAddressId = null) => {
  const params = shippingAddressId ? { shippingAddressId } : undefined;
  const { data } = await api.post('/orders/checkout', null, { params });
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