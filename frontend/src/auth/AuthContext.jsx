import { useCallback, useEffect, useMemo, useState } from 'react';
import * as authApi from '../api/auth';
import { readToken, setUnauthorizedHandler, writeToken } from '../api/client';
import { AuthContext } from './auth-context';

const USER_STORAGE_KEY = 'prm.user';

const readStoredUser = () => {
  try {
    const raw = window.localStorage.getItem(USER_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

const persistUser = (user) => {
  try {
    if (user) window.localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
    else window.localStorage.removeItem(USER_STORAGE_KEY);
  } catch {
    // A session that does not survive a reload is better than an app that will not start.
  }
};

export function AuthProvider({ children }) {
  // The session is rebuilt from localStorage on first render rather than fetched. The token
  // carries no readable payload, and the backend exposes no "/me" endpoint to refresh the user
  // from, so the stored copy is the only source available without a new backend route.
  //
  // Both values initialise synchronously, which is why there is no "loading" flag here: by the time
  // this component's first render commits, the session is already known, and a flag that starts true
  // and flips false in an effect would only add a render pass in which the UI must handle a state
  // that can never actually be observed.
  const [user, setUser] = useState(readStoredUser);
  const [token, setToken] = useState(readToken);

  const signOut = useCallback(() => {
    writeToken(null);
    persistUser(null);
    setToken(null);
    setUser(null);
  }, []);

  // The axios interceptor calls this when the server rejects the token, so an expired session
  // clears itself without the component that happened to make the failing request knowing why.
  useEffect(() => {
    setUnauthorizedHandler(() => signOut());
    return () => setUnauthorizedHandler(null);
  }, [signOut]);

  /**
   * Stores the token and user from a login or verify response.
   *
   * `persistent` decides which browser store the token lands in. Note that a session-scoped token is
   * also read back by the interceptor, so un-ticking "remember me" shortens the session's life
   * without breaking anything while the tab stays open.
   */
  const adoptSession = useCallback((authResponse, { persistent = true } = {}) => {
    writeToken(authResponse.token, { persistent });
    persistUser(authResponse.user);
    setToken(authResponse.token);
    setUser(authResponse.user);
    return authResponse.user;
  }, []);

  const signIn = useCallback(async (email, password, { persistent = true } = {}) => {
    const authResponse = await authApi.login(email, password);
    return adoptSession(authResponse, { persistent });
  }, [adoptSession]);

  // Registration deliberately does not adopt a session. The response *does* carry a token, but the
  // account is unverified at that moment and login answers 401 "Account is not verified yet" until
  // the emailed code is entered. Adopting it would leave the app holding a credential for an account
  // that cannot be used, so the user is sent to the verification page and signs in afterwards.
  const signUp = useCallback(async (payload) => authApi.register(payload), []);

  const verifyAccount = useCallback(async (email, code) => authApi.verifyCode(email, code), []);

  const value = useMemo(() => ({
    user,
    token,
    isAuthenticated: Boolean(token),
    isVerified: Boolean(user?.verified),
    signIn,
    signUp,
    signOut,
    verifyAccount,
  }), [user, token, signIn, signUp, signOut, verifyAccount]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}