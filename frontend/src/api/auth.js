import { api } from './client';

/**
 * Authentication endpoints.
 *
 * The backend derives the account from the token wherever it can. `changePassword` in particular
 * names no account at all — it carries only the two passwords — so there is nothing here for a
 * caller to get wrong about whose password is being changed.
 */

export const login = async (email, password) => {
  const { data } = await api.post('/auth/login', { email, password });
  return data;
};

export const register = async (payload) => {
  // The user selects only public account types here. Admin privileges remain provisioned by the
  // backend and cannot be granted by a registration request.
  const { data } = await api.post('/auth/register', {
    role: payload.role,
    name: payload.name,
    email: payload.email,
    password: payload.password,
    phone: payload.phone || undefined,
    businessName: payload.businessName || undefined,
    registrationNo: payload.registrationNo || undefined,
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
 * Both passwords go in the request body. This used to be a query string, which put the new password
 * in the request line and therefore into every access log, proxy log and browser history entry
 * between the browser and the server — the one place a credential has no business being. The
 * backend reads them from a body now, the same as `/auth/reset-password`.
 *
 * Note there is still no account field anywhere in here: the server takes the account from the
 * token, so a caller cannot aim this at somebody else's login.
 */
export const changePassword = async (currentPassword, newPassword) => {
  const { data } = await api.post('/auth/change-password', { currentPassword, newPassword });
  return data;
};