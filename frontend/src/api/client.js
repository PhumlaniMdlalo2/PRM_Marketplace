import axios from 'axios'

/**
 * One axios instance for the whole app.
 *
 * Everything goes through here rather than through per-call `axios.get` calls so that three
 * concerns are handled in exactly one place: the Bearer token, turning the backend's error body
 * into a predictable JavaScript shape, and reacting to a token that has stopped working.
 *
 * The baseURL is relative on purpose. In development Vite proxies "/api" to the Spring backend
 * (see vite.config.js), so the browser talks to its own origin and CORS never applies. In
 * production the app is served from the same place as the API, or behind the same proxy, and the
 * relative path still resolves correctly.
 */
export const api = axios.create({
  baseURL: '/api',
  // Anything slower than this is worth telling the user about rather than leaving a spinner up
  // forever. Without it a hung request is indistinguishable from a slow one.
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

export const TOKEN_STORAGE_KEY = 'prm.token'

const readFrom = (storage) => {
  try {
    return storage?.getItem(TOKEN_STORAGE_KEY) ?? null;
  } catch {
    return null;
  }
};

const writeTo = (storage, token) => {
  try {
    if (token) storage?.setItem(TOKEN_STORAGE_KEY, token);
    else storage?.removeItem(TOKEN_STORAGE_KEY);
  } catch {
    // Blocked storage must not break the app; the session simply will not persist.
  }
};

const storages = () => {
  try {
    return [window.localStorage, window.sessionStorage];
  } catch {
    return [];
  }
};

/**
 * The token lives in localStorage by default so the session survives a reload — the backend's JWT
 * TTL is 24 hours and there is no refresh endpoint, so an in-memory token would sign the user out
 * on every refresh.
 *
 * "Remember me" is wired to a real difference: a session-scoped token goes into sessionStorage
 * instead and dies with the tab, which is what unticking the box is asking for.
 */
export const writeToken = (token, { persistent = true } = {}) => {
  // Both stores are cleared on every write. Unticking "remember me" has to actively remove the
  // localStorage copy, otherwise the session survives anyway and the checkbox lies.
  for (const storage of storages()) writeTo(storage, null);
  if (!token) return;
  writeTo(persistent ? window.localStorage : window.sessionStorage, token);
};

/**
 * Reads from localStorage first, then sessionStorage, so a session-scoped token is still found
 * after a reload of the same tab.
 */
export const readToken = () => {
  for (const storage of storages()) {
    const token = readFrom(storage);
    if (token) return token;
  }
  return null;
};

/**
 * Called when the server rejects the token, so the app can drop the session.
 *
 * Wiring this to a callback rather than importing the auth context keeps this module free of
 * React: the client is imported by the provider that would otherwise have to import the client,
 * and that circular dependency is what makes interceptors quietly break.
 */
let onUnauthorized = null;
export const setUnauthorizedHandler = (handler) => { onUnauthorized = handler; };

api.interceptors.request.use((config) => {
  const token = readToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

/**
 * The backend's error bodies look like:
 *
 *   { timestamp, status, error, message, path, fieldErrors }
 *
 * where fieldErrors is a map of form-field name to message. axios by default hands the raw
 * Error to callers, so `error.response.data.message` is undefined and `error.message` is the
 * generic "Request failed with status code 400". Normalising into a plain object means a component
 * can render `error.message` and `error.fieldErrors.name` without knowing any of this.
 */
const normalise = (error) => {
  const body = error.response?.data ?? {};

  if (error.response?.status === 401) {
    // The token is gone, expired or revoked. Clearing it here stops every later request from
    // replaying a credential the server has already rejected.
    writeToken(null);
    onUnauthorized?.();
  }

  return {
    status: error.response?.status ?? null,
    message: body.message || (error.code === 'ECONNABORTED'
      ? 'The server took too long to respond. Please try again.'
      : 'Cannot reach the server. Please check your connection.'),
    fieldErrors: body.fieldErrors ?? null,
    raw: error,
  };
};

api.interceptors.response.use((response) => response, normalise);

export default api;