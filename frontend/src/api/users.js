import api from './client';

/**
 * The caller's own account.
 *
 * Both endpoints name no account: `/users/me` resolves the target from the token, so there is no id
 * in the path or the body for a caller to aim somewhere else with. That is deliberate — the old
 * `PUT /users` took the target account from `user.id` inside the body and saved every field of it,
 * which let any signed-in user rewrite somebody else's account, including its role.
 *
 * Note there is no endpoint here for email or password. Email is absent because changing it
 * invalidates the verified flag the emailed code established, which needs its own confirm-the-new-
 * address flow; the backend has not got one, so the field is not offered. Passwords live on
 * `/auth/change-password`.
 */

/** The signed-in account, as the server currently has it. */
export const getMe = async () => {
  const { data } = await api.get('/users/me');
  return data;
};

/**
 * Saves the caller's own profile.
 *
 * `phone` and `avatarUrl` are replaced outright, so a blank value clears the field: the server is
 * told what the fields are now, not which ones changed. Send the whole form.
 *
 * @param {{ name: string, phone?: string, avatarUrl?: string }} profile
 * @returns {Promise<object>} the saved account, which the session should adopt
 */
export const updateMe = async ({ name, phone, avatarUrl }) => {
  const { data } = await api.put('/users/me', { name, phone, avatarUrl });
  return data;
};