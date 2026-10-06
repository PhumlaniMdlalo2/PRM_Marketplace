import api from './client';

/**
 * Stores one image and answers with the address it can be read back from.
 *
 * The Content-Type has to be named on this call, and only this call: the shared instance defaults
 * to application/json, and axios converts a FormData body to JSON as soon as it sees that header —
 * the file itself would be dropped before the request left. Naming multipart/form-data is what
 * keeps the body intact, and axios strips the header again at send time so the browser can add the
 * boundary that makes it parseable.
 *
 * The address comes back relative (`/api/uploads/images/…`) on purpose: everything that renders it
 * is served from the same origin.
 */
export const uploadImage = async (file) => {
  const body = new FormData();
  body.append('file', file);
  const { data } = await api.post('/uploads/images', body, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data.url;
};
