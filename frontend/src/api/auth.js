import { api } from './client';

/**
 * Authentication endpoints.
 *
 * The backend derives the account from the token wherever it can. `changePassword` in particular
 * takes no account parameter at all — it takes only the two passwords — so there is nothing here
 * for a caller to get wrong about whose password is being changed.
 */

export const login = async (email, password) => {
  const { data } = await api.post('/auth/login', { email, password });
  return data;
};

export const register = async (payload) => {
  // `role` is deliberately omitted. The server grants only the self-service roles and rejects
  // anything else, so sending a role the UI never legitimately offers would just be a way to
  // provoke a 400.
  const { data } = await api.post('/auth/register', {
    name: payload.name,
    email: payload.email,
    password: payload.password,
    phone: payload.phone || undefined,
  });
  return data;
};

export const verifyCode = async (email, code) => {
  const { data } = await api.post('/auth/verify', { email, code });
  return data;
};

/**
 * Resend the verification code.
 *
 * Takes no request body: the server reads the address from the `email` query parameter, so sending
 * `{ email }` as a body is silently ignored and the call fails as a missing parameter.
 */
export const resendCode = async (email) => {
  const { data } = await api.post('/auth/resend-code', null, { params: { email } });
  return data;
};

export const forgotPassword = async (email) => {
  const { data } = await api.post('/auth/forgot-password', { email });
  return data;
};

export const resetPassword = async (token, newPassword) => {
  const { data } = await api.post('/auth/reset-password', { token, newPassword });
  return data;
};

/**
 * Changes the signed-in user's password.
 *
 * These go in the query string because that is what the endpoint declares, which means the current
 * password lands in access logs and browser history. The backend is the side that chose this, and
 * the fix belongs there: move it to a request body the way every other credential route already
 * does. Flagged rather than silently worked around.
 */
export const changePassword = async (currentPassword, newPassword) => {
  const { data } = await api.post('/auth/change-password', null, {
    params: { currentPassword, newPassword },
  });
  return data;
};