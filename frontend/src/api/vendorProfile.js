import api from './client';

/**
 * Seller profiles.
 *
 * A profile is what a listing is published under, so the create-listing page checks for one before
 * submitting: the server rejects a create from an account without a profile, but only with an opaque
 * 400 that says nothing about which of the possible reasons it was.
 */

/**
 * Every seller profile, for the public directory.
 *
 * The route has been open to anyone without a token since the backend was written, and nothing in
 * the app ever called it: a seller could only be reached by way of one of their listings, so a shop
 * with nothing listed at that moment did not exist as far as a buyer was concerned.
 *
 * The profile's own `user` relation is deliberately not in the body, so a directory entry can say
 * what a shop is called and how it has been rated, and nothing else about the person behind it.
 *
 * @returns {Promise<Array>} the profiles, always an array
 */
export const listVendorProfiles = async () => {
  const response = await api.get('/vendor-profiles');
  return Array.isArray(response.data) ? response.data : [];
};

/**
 * One seller profile.
 *
 * @throws {Object} the normalised client error, with `status` 404 when the shop is not on the
 *   directory — which is how an unknown id arrives, since the route answers 404 rather than an
 *   empty list.
 */
export const getVendorProfile = async (id) => {
  const response = await api.get(`/vendor-profiles/${id}`);
  return response.data;
};

/**
 * The caller's own seller profile.
 *
 * @throws {Object} the normalised client error, with `status` 404 when the account has no seller
 *   profile yet.
 */
export const getMyVendorProfile = async () => {
  const response = await api.get('/vendor-profiles/me');
  return response.data;
};

/**
 * Creates the caller's own seller profile. Verified STUDENT and VENDOR accounts may hold one, and
 * only one per account.
 *
 * <p>Only the two editable fields are sent: verified and ratingAvg are the server's to set.
 *
 * @throws {Object} the normalised client error, with `status` 400 when the role is not eligible or a
 *   profile already exists
 */
export const createVendorProfile = async (profile) => {
  const response = await api.post('/vendor-profiles', profile);
  return response.data;
};

/** Updates the caller's own seller profile. Somebody else's is reported as not found. */
export const updateVendorProfile = async (id, profile) => {
  const response = await api.put(`/vendor-profiles/${id}`, profile);
  return response.data;
};

export const getMyPayoutDetails = async () => {
  const response = await api.get('/vendor-profiles/me/payout-details');
  return response.data;
};

export const updateMyPayoutDetails = async (details) => {
  const response = await api.put('/vendor-profiles/me/payout-details', details);
  return response.data;
};

/** Grants or withdraws admin approval for a seller profile. */
export const setVendorVerification = async (id, verified) => {
  const response = await api.patch(`/vendor-profiles/${id}/verification`, null, {
    params: { verified },
  });
  return response.data;
};