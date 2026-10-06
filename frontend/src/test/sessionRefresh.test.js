import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY,
  api,
  clearSession,
  readToken,
  setSessionRefreshHandler,
  setUnauthorizedHandler,
  writeRefreshToken,
  writeToken,
} from '../api/client';

/**
 * The session's renewal path, exercised against a stand-in for the network.
 *
 * Two facts about this client are load-bearing and neither is visible from a page:
 *
 *   - a 401 on an ordinary request buys a new session once and replays the request, so an
 *     access token that expires mid-use does not throw the user out;
 *   - a failure arrives as a rejection carrying `status`, `message` and `fieldErrors`. The
 *     interceptor used to return that same shape from its error handler, which made axios
 *     *resolve*, so every `catch` in the app was dead code and errors looked like successes.
 *
 * Both are asserted here, along with the edges: a refused login is not a dead session, a refused
 * renewal is, and a network failure is neither.
 */

const REFRESHED = {
  token: 'renewed-access',
  refreshToken: 'renewed-refresh',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: { id: 'u1', name: 'Jane', email: 'jane@example.com', role: 'STUDENT', verified: true },
};

const seedSession = () => {
  writeToken('expired-access');
  writeRefreshToken('stale-refresh');
};

/**
 * Stands in for an axios adapter, including the part that matters here: a built-in adapter
 * *rejects* a non-2xx status rather than resolving it, which is what puts the response
 * interceptor on the failure path.
 */
const errorFor = (status, data, config) => {
  const error = new Error(data.message ?? `Request failed with status code ${status}`);
  error.isAxiosError = true;
  error.config = config;
  error.response = { status, statusText: 'Error', data, headers: {}, config };
  return error;
};

let respond = () => ({ status: 200, data: {} });
let refreshEntries = 0;

const adapter = async (config) => {
  const result = await respond(config);
  if (result.status >= 400) throw errorFor(result.status, result.data ?? {}, config);
  return { data: result.data, status: result.status, statusText: 'OK', headers: {}, config };
};

beforeEach(() => {
  api.defaults.adapter = adapter;
  refreshEntries = 0;
});

afterEach(() => {
  clearSession();
  setUnauthorizedHandler(null);
  setSessionRefreshHandler(null);
  respond = () => ({ status: 200, data: {} });
});

describe('renewal', () => {
  it('renews once and replays the request that found the expiry', async () => {
    seedSession();
    const onRefresh = vi.fn();
    const refreshBodies = [];
    setSessionRefreshHandler(onRefresh);
    respond = (config) => {
      if (config.url === '/auth/refresh') {
        refreshEntries += 1;
        refreshBodies.push(config.data);
        return { status: 200, data: REFRESHED };
      }
      // The server refuses anything still carrying the expired access token.
      if (refreshEntries === 0) {
        return { status: 401, data: { status: 401, message: 'Access token expired' } };
      }
      return { status: 200, data: { id: 'o1' } };
    };

    const { data } = await api.get('/orders');

    expect(data).toEqual({ id: 'o1' });
    expect(refreshEntries).toBe(1);
    expect(JSON.parse(refreshBodies[0])).toEqual({ refreshToken: 'stale-refresh' });
    expect(onRefresh).toHaveBeenCalledWith(REFRESHED);
    expect(readToken()).toBe('renewed-access');
    expect(localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY)).toBe('renewed-refresh');
  });

  it('renews several failures at once with a single exchange', async () => {
    seedSession();
    let renewed = false;
    respond = async (config) => {
      if (config.url === '/auth/refresh') {
        refreshEntries += 1;
        // A full turn of the event loop, so every request that failed alongside this one has
        // had the chance to reach the interceptor before the renewal lands.
        await new Promise((resolve) => { setTimeout(resolve, 0); });
        renewed = true;
        return { status: 200, data: REFRESHED };
      }
      if (!renewed) return { status: 401, data: { status: 401, message: 'Access token expired' } };
      return { status: 200, data: { path: config.url } };
    };

    const responses = await Promise.all([
      api.get('/orders'),
      api.get('/products'),
      api.get('/cart-items'),
    ]);

    expect(refreshEntries).toBe(1);
    expect(responses.map((response) => response.data.path))
      .toEqual(['/orders', '/products', '/cart-items']);
    expect(readToken()).toBe('renewed-access');
  });

  it('keeps the session-scoped session scoped when it renews', async () => {
    writeToken('expired-access', { persistent: false });
    writeRefreshToken('stale-refresh', { persistent: false });
    let renewed = false;
    respond = (config) => {
      if (config.url === '/auth/refresh') {
        refreshEntries += 1;
        renewed = true;
        return { status: 200, data: REFRESHED };
      }
      if (!renewed) return { status: 401, data: { status: 401, message: 'Access token expired' } };
      return { status: 200, data: { ok: true } };
    };

    await api.get('/orders');

    // Renewing into localStorage would quietly turn "do not keep me signed in" into the other one.
    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
    expect(localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY)).toBeNull();
    expect(sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBe('renewed-access');
    expect(sessionStorage.getItem(REFRESH_TOKEN_STORAGE_KEY)).toBe('renewed-refresh');
  });
});

describe('when renewal is refused', () => {
  it('clears the session, tells the app once, and hands back the reason', async () => {
    seedSession();
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    respond = (config) => {
      if (config.url === '/auth/refresh') {
        refreshEntries += 1;
        return {
          status: 401,
          data: { status: 401, message: 'Session expired. Please sign in again' },
        };
      }
      return { status: 401, data: { status: 401, message: 'Access token expired' } };
    };

    await expect(api.get('/orders')).rejects.toMatchObject({
      status: 401,
      message: 'Session expired. Please sign in again',
    });

    expect(refreshEntries).toBe(1);
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
    expect(readToken()).toBeNull();
    expect(localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY)).toBeNull();
    expect(sessionStorage.getItem(REFRESH_TOKEN_STORAGE_KEY)).toBeNull();
  });

  it('signs out without attempting a renewal when there is nothing to renew with', async () => {
    writeToken('expired-access');
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    respond = () => ({ status: 401, data: { status: 401, message: 'Full authentication is required' } });

    await expect(api.get('/orders')).rejects.toMatchObject({ status: 401 });

    expect(refreshEntries).toBe(0);
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
    expect(readToken()).toBeNull();
  });

  it('leaves the session alone when renewal fails for a reason that is not a verdict', async () => {
    seedSession();
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    respond = (config) => {
      if (config.url === '/auth/refresh') {
        refreshEntries += 1;
        return { status: 500, data: { status: 500, message: 'Something went wrong' } };
      }
      return { status: 401, data: { status: 401, message: 'Access token expired' } };
    };

    await expect(api.get('/orders')).rejects.toMatchObject({ status: 500 });

    // A timeout or a server blip must not be read as "you are signed out"; the next request can
    // still try the same refresh token.
    expect(onUnauthorized).not.toHaveBeenCalled();
    expect(readToken()).toBe('expired-access');
    expect(localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY)).toBe('stale-refresh');
  });
});

describe('when the credentials themselves are refused', () => {
  it('does not renew a refused login, and does not sign anybody out for it', async () => {
    seedSession();
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    respond = (config) => {
      if (config.url === '/auth/refresh') {
        refreshEntries += 1;
        return { status: 200, data: REFRESHED };
      }
      return { status: 401, data: { status: 401, message: 'Invalid email or password' } };
    };

    await expect(api.post('/auth/login', { email: 'jane@example.com' }))
      .rejects.toMatchObject({ status: 401, message: 'Invalid email or password' });

    expect(refreshEntries).toBe(0);
    expect(onUnauthorized).not.toHaveBeenCalled();
    expect(readToken()).toBe('expired-access');
  });
});

describe('errors', () => {
  it('rejects with the backend message and field errors instead of resolving', async () => {
    respond = () => ({
      status: 400,
      data: {
        status: 400,
        message: 'Title is required',
        fieldErrors: { title: 'Title is required' },
      },
    });

    await expect(api.post('/products', {})).rejects.toMatchObject({
      status: 400,
      message: 'Title is required',
      fieldErrors: { title: 'Title is required' },
    });
  });

  it('explains a dropped connection rather than surfacing a transport error', async () => {
    respond = () => Promise.reject(Object.assign(new Error('timeout of 15000ms exceeded'), {
      isAxiosError: true,
      code: 'ECONNABORTED',
      config: { url: '/orders' },
    }));

    await expect(api.get('/orders')).rejects.toMatchObject({
      status: null,
      message: 'The server took too long to respond. Please try again.',
    });
  });
});
