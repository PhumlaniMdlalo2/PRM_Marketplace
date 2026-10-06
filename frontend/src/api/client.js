import axios from 'axios'

/**
 * One axios instance for the whole app.
 *
 * Everything goes through here rather than through per-call `axios.get` calls so that four
 * concerns are handled in exactly one place: the Bearer token, turning the backend's error body
 * into a predictable JavaScript shape, renewing a session whose access token has just expired, and
 * reacting to a session the server refuses to renew.
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
export const REFRESH_TOKEN_STORAGE_KEY = 'prm.refresh'

const readFrom = (storage, key) => {
  try {
    return storage?.getItem(key) ?? null;
  } catch {
    return null;
  }
};

const writeTo = (storage, key, value) => {
  try {
    if (value) storage?.setItem(key, value);
    else storage?.removeItem(key);
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
 * Finds a stored value, reporting which store it came from.
 *
 * localStorage is read first, so a session-scoped token is still found after a reload of the same
 * tab. The flag is what keeps renewal honest: the replacement refresh token is written back to the
 * store the previous one was found in, so "remember me" keeps meaning what it meant at sign-in.
 */
const locate = (key) => {
  for (const storage of storages()) {
    const value = readFrom(storage, key);
    if (value) return { value, persistent: storage === window.localStorage };
  }
  return null;
};

const writeStored = (key, value, { persistent = true } = {}) => {
  // Both stores are cleared on every write. Unticking "remember me" has to actively remove the
  // localStorage copy, otherwise the session survives anyway and the checkbox lies.
  for (const storage of storages()) writeTo(storage, key, null);
  if (!value) return;
  writeTo(persistent ? window.localStorage : window.sessionStorage, key, value);
};

/**
 * The access token lives in browser storage so the session survives a reload: the backend signs it
 * for 24 hours, and an in-memory copy would sign the user out of every refresh of the tab while the
 * refresh token sat unused beside it.
 *
 * "Remember me" is wired to a real difference: a session-scoped token goes into sessionStorage
 * instead and dies with the tab, which is what unticking the box is asking for.
 */
export const writeToken = (token, options = {}) => writeStored(TOKEN_STORAGE_KEY, token, options);

/** Written next to the access token, in the same store, so both halves of the session travel together. */
export const writeRefreshToken = (token, options = {}) => writeStored(REFRESH_TOKEN_STORAGE_KEY, token, options);

/**
 * Drops both halves of the session from both stores.
 *
 * Signing out with the refresh token still in place would leave the app able to mint a new access
 * token after the user believed it was gone.
 */
export const clearSession = () => {
  for (const storage of storages()) {
    writeTo(storage, TOKEN_STORAGE_KEY, null);
    writeTo(storage, REFRESH_TOKEN_STORAGE_KEY, null);
  }
};

export const readToken = () => locate(TOKEN_STORAGE_KEY)?.value ?? null;

/** Read before every renewal: `{ token, persistent }`, or null when there is nothing to renew with. */
export const readRefreshToken = () => {
  const located = locate(REFRESH_TOKEN_STORAGE_KEY);
  return located ? { token: located.value, persistent: located.persistent } : null;
};

/**
 * Called when the session cannot be renewed, so the app can drop it.
 *
 * Wiring this to a callback rather than importing the auth context keeps this module free of
 * React: the client is imported by the provider that would otherwise have to import the client,
 * and that circular dependency is what makes interceptors quietly break.
 */
let onUnauthorized = null;
export const setUnauthorizedHandler = (handler) => { onUnauthorized = handler; };

/**
 * Called after a successful renewal.
 *
 * Storage is already updated by then; this exists because the auth context also holds the token
 * and user as React state, and without it `isAuthenticated` and the stored copy would drift from
 * the credentials the interceptor is now using.
 */
let onSessionRefresh = null;
export const setSessionRefreshHandler = (handler) => { onSessionRefresh = handler; };

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
 *
 * It is a formatter only. The decision about what a 401 means belongs to the response interceptor
 * below, which is the one place that can see the whole session.
 */
const normalise = (error) => {
  const body = error.response?.data ?? {};

  return {
    status: error.response?.status ?? null,
    message: body.message || (error.code === 'ECONNABORTED'
      ? 'The server took too long to respond. Please try again.'
      : 'Cannot reach the server. Please check your connection.'),
    fieldErrors: body.fieldErrors ?? null,
    raw: error,
  };
};

/** Authentication routes whose own 401 means "those credentials were refused", not "renew". */
const AUTH_CALLS = ['/auth/login', '/auth/refresh'];

/**
 * Exchanges the stored refresh token for a fresh pair, once.
 *
 * The call carries `skipAuthRefresh` because it is the refresh itself: a 401 here is the server
 * refusing to renew, and sending that back through the same path would be a loop rather than an
 * answer.
 *
 * A 401 or 403 is the end of the session and clears it. A network failure or a 500 is not a
 * verdict on the session, so it is left alone — signing a user out because a request timed out
 * would turn a recoverable hiccup into a lost one.
 */
const performRefresh = async () => {
  const located = readRefreshToken();
  if (!located) return null;

  try {
    const { data } = await api.post('/auth/refresh', { refreshToken: located.token }, { skipAuthRefresh: true });
    writeToken(data.token, { persistent: located.persistent });
    writeRefreshToken(data.refreshToken, { persistent: located.persistent });
    onSessionRefresh?.(data);
    return data;
  } catch (error) {
    if (error.status === 401 || error.status === 403) {
      clearSession();
      onUnauthorized?.();
    }
    throw error;
  }
};

/**
 * Single flight: several requests failing at the same instant produce one exchange, not one each.
 *
 * The refresh token is spent by the server on its first use, so parallel exchanges would race, and
 * whichever one landed second would be holding a token the server already knows is dead.
 */
let refreshInFlight = null;
const ensureRefreshed = () => {
  if (!refreshInFlight) {
    refreshInFlight = performRefresh().finally(() => { refreshInFlight = null; });
  }
  return refreshInFlight;
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status;
    if (status !== 401) return Promise.reject(normalise(error));

    // The config carries the URL, the renewal flags and the identity of the request. Without it
    // the 401 cannot be identified, renewed or replayed, so it is handled the way a session that
    // has simply run out is handled.
    const config = error.config;
    if (!config) {
      clearSession();
      onUnauthorized?.();
      return Promise.reject(normalise(error));
    }

    // A refused login or a refused renewal says something about the credentials just presented,
    // not about the session, so neither is allowed to clear anything.
    const isAuthCall = config.skipAuthRefresh
      || AUTH_CALLS.some((route) => (config.url ?? '').includes(route));
    if (isAuthCall) return Promise.reject(normalise(error));

    // The renewal path: one attempt per request, and only when there is something to renew with.
    if (!config._retried && readRefreshToken()) {
      config._retried = true;
      try {
        const refreshed = await ensureRefreshed();
        if (refreshed) return api.request(config);
      } catch (refreshError) {
        // performRefresh has already cleared the session when the server refused the token.
        return Promise.reject(refreshError);
      }
    }

    // Nothing left to try: the credential is dead and every later request must stop repeating it.
    clearSession();
    onUnauthorized?.();
    return Promise.reject(normalise(error));
  },
);

export default api;
