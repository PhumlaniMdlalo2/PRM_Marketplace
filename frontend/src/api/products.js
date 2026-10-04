import { api } from './client';

/**
 * Catalogue endpoints.
 *
 * Browsing is public on the server, so none of the read functions here need a token; the
 * interceptor attaches one when there is one, which is harmless. The write functions are different:
 * the server ignores any owner, id or status in the body and derives them from the token, so those
 * fields are deliberately not forwarded from here.
 */

/** Newest first. */
export const listAll = async () => {
  const { data } = await api.get('/products');
  return data;
};

/** The caller's own listings, from the token. */
export const listMine = async () => {
  const { data } = await api.get('/products/mine');
  return data;
};

export const getById = async (id) => {
  const { data } = await api.get(`/products/${id}`);
  return data;
};

export const listByCategory = async (category) => {
  const { data } = await api.get(`/products/category/${encodeURIComponent(category)}`);
  return data;
};

/**
 * Reviews left on one listing.
 *
 * This lives in the reviews controller on the server but belongs with the product page, which is
 * the only consumer, so it is kept here rather than in a separate module of one function. The
 * endpoint is public and returns a plain array rather than a page.
 */
export const listReviews = async (productId) => {
  const { data } = await api.get(`/reviews/product/${productId}`);
  return data;
};

export const listByVendor = async (vendorId) => {
  const { data } = await api.get(`/products/vendor/${vendorId}`);
  return data;
};

/**
 * The filtered, paged search.
 *
 * Empty values are dropped rather than sent as `?keyword=` because the server treats a blank
 * string as a real filter value, which would match nothing instead of everything.
 */
export const search = async (criteria = {}) => {
  const params = {};
  for (const [key, value] of Object.entries(criteria)) {
    if (value !== undefined && value !== null && value !== '') params[key] = value;
  }
  const { data } = await api.get('/products/search', { params });
  return data;
};

/**
 * @param {object} product  name, description, price, stockQuantity, category, imageUrl,
 *                           condition, city, province
 */
export const create = async (product) => {
  const { data } = await api.post('/products', product);
  return data;
};

export const update = async (id, product) => {
  const { data } = await api.put(`/products/${id}`, product);
  return data;
};

/** Retiring a listing clears the `active` flag rather than deleting the row. */
export const retire = async (id) => update(id, { active: false });

export const listImages = async (productId) => {
  const { data } = await api.get(`/product-images/product/${productId}`);
  return data;
};

export const getPrimaryImage = async (productId) => {
  const { data } = await api.get(`/product-images/product/${productId}/primary`);
  return data;
};

/** The product comes from the path, so it is not part of the body. */
export const addImage = async (productId, image) => {
  const { data } = await api.post(`/product-images/product/${productId}`, image);
  return data;
};

export const updateImage = async (id, image) => {
  const { data } = await api.put(`/product-images/${id}`, image);
  return data;
};

export const deleteImage = async (id) => {
  await api.delete(`/product-images/${id}`);
};

export const clearImages = async (productId) => {
  await api.delete(`/product-images/product/${productId}`);
};