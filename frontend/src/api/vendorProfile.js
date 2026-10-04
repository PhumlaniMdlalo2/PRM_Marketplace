import api from './client';

/**
 * Seller profiles.
 *
 * A profile is what a listing is published under, so the create-listing page checks for one before
 * submitting: the server rejects a create from an account without a profile, but only with an opaque
 * 400 that says nothing about which of the possible reasons it was.
 */

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
 * Creates the caller's own seller profile. Only a VENDOR account may hold one, and only one per
 * account, so a student or a second attempt both come back 400.
 *
 * <p>Only the two editable fields are sent: verified and ratingAvg are the server's to set.
 *
 * @throws {Object} the normalised client error, with `status` 400 when the role is not VENDOR or a
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