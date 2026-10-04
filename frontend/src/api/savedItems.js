import api from './client';

/**
 * Saved items are the caller's own shortlist, so every endpoint here is scoped by the token rather
 * than by a user id in the path. There is no way to ask for someone else's saved list.
 */

/** The caller's saved items, each carrying the product it points at. */
export const listSavedItems = async () => {
  const { data } = await api.get('/saved-items');
  return data ?? [];
};

/** How many items the caller has saved. */
export const countSavedItems = async () => {
  const { data } = await api.get('/saved-items/count');
  return data ?? 0;
};

/**
 * Saves a product if it is not saved and removes it if it is, which is what the heart on a product
 * card means.
 *
 * The server answers 200 with the new saved item, or 204 with no body when it removed one, so the
 * return value is normalised here to the question a caller actually has: is this product saved now.
 * Note this endpoint is not consistent with the post-like toggle, which answers 201 to create; the
 * status is therefore read as "did the server send a body back" rather than matched to one code.
 *
 * @returns {boolean} true when the product is now saved
 */
export const toggleSavedItem = async (productId) => {
  const response = await api.post(`/saved-items/product/${productId}/toggle`);
  return response.status !== 204;
};

/** Removes one saved item by its own id. Returns false when it was not the caller's. */
export const removeSavedItem = async (savedItemId) => {
  try {
    await api.delete(`/saved-items/${savedItemId}`);
    return true;
  } catch (error) {
    // The server answers 404 for an item that is not there or not the caller's, which is the same
    // outcome the caller wanted: it is not in the list any more.
    if (error.status === 404) return true;
    throw error;
  }
};

/** Removes a saved item by the product it points at, for when the saved item's own id is unknown. */
export const removeSavedItemByProduct = async (productId) => {
  try {
    await api.delete(`/saved-items/product/${productId}`);
    return true;
  } catch (error) {
    if (error.status === 404) return true;
    throw error;
  }
};