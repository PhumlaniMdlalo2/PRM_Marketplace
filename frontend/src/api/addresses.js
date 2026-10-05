import api from './client';

/**
 * The caller's delivery addresses. Every route here is scoped to the token: the service files an
 * address under the caller and clears any id in the body, so an address cannot be planted on another
 * account or written over an existing row.
 */

export const listAddresses = async () => {
  const response = await api.get('/addresses');
  return response.data;
};

/**
 * Adds an address. Setting defaultAddress clears the previous default first, so at most one is ever
 * the default.
 */
export const createAddress = async (address) => {
  const response = await api.post('/addresses', address);
  return response.data;
};

/**
 * Edits a saved address.
 *
 * The whole address is sent rather than just the changed fields, because `Address.defaultAddress`
 * is a primitive boolean on the entity: a body that omits it arrives as false and would silently
 * unset the default. Callers should spread the stored address and overwrite what changed, which is
 * what the settings panel does.
 */
export const updateAddress = async (id, address) => {
  const response = await api.put(`/addresses/${id}`, address);
  return response.data;
};

export const deleteAddress = async (id) => {
  await api.delete(`/addresses/${id}`);
};

/** Promotes one address to the default. */
export const setDefaultAddress = async (id) => {
  const response = await api.patch(`/addresses/${id}/default`);
  return response.data;
};