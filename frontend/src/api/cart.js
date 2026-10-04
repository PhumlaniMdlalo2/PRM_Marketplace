import { api } from './client';

/**
 * The caller's cart.
 *
 * Every route here is already scoped to the token on the server side — there is no userId to pass
 * and no way to name somebody else's cart — so nothing here needs an id argument for the owner.
 */

export const listCartItems = async () => {
  const { data } = await api.get('/cart-items');
  return data;
};

export const addCartItem = async (productId, quantity = 1) => {
  const { data } = await api.post('/cart-items', null, { params: { productId, quantity } });
  return data;
};

/**
 * Sets a line to an exact quantity.
 *
 * The server treats zero or less as "remove this line" and answers 204 with no body, so the return
 * value is null in that case rather than an item. Callers must not assume a line came back.
 */
export const setCartItemQuantity = async (cartItemId, quantity) => {
  const { data } = await api.put(`/cart-items/${cartItemId}/quantity`, null, {
    params: { quantity },
  });
  return data ?? null;
};

export const removeCartItem = async (cartItemId) => {
  await api.delete(`/cart-items/${cartItemId}`);
};

export const clearCart = async () => {
  await api.delete('/cart-items');
};