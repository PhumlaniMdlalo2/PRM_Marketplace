import { useCallback, useEffect, useMemo, useState } from 'react';
import * as authApi from '../api/auth';
import {
  clearSession, readToken, setSessionRefreshHandler, setUnauthorizedHandler, writeRefreshToken, writeToken,
} from '../api/client';
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
  // The session is rebuilt from localStorage on first render rather than fetched, so the first paint
  // of an authenticated page does not wait on a round trip. `GET /api/users/me` does exist and is
  // what the profile page reads; `refreshUser` uses it to correct the stored copy whenever the server
  // is the authority and the stored copy may be stale.
  //
  // Both values initialise synchronously, which is why there is no "loading" flag here: by the time
  // this component's first render commits, the session is already known, and a flag that starts true
  // and flips false in an effect would only add a render pass in which the UI must handle a state
  // that can never actually be observed.
  const [user, setUser] = useState(readStoredUser);
  const [token, setToken] = useState(readToken);

  const signOut = useCallback(() => {
    clearSession();
    persistUser(null);
    setToken(null);
    setUser(null);
  }, []);

  // The axios interceptor calls this only when the session has run out for good: the server would
  // not renew it, or there was nothing left to renew with. An expired access token does not land
  // here — the interceptor renews it underneath whichever request noticed first. What arrives is
  // the tail end of a session, so an expired one clears itself without the component that happened
  // to make the failing request knowing why.
  useEffect(() => {
    setUnauthorizedHandler(() => signOut());
    // The same interception point, on its way back up: storage holds the new credentials already,
    // and this is where React's copy of them is brought back into step.
    setSessionRefreshHandler((authResponse) => {
      persistUser(authResponse.user);
      setUser(authResponse.user);
      setToken(authResponse.token);
    });
    return () => {
      setUnauthorizedHandler(null);
      setSessionRefreshHandler(null);
    };
  }, [signOut]);

  /**
   * Stores the token, refresh token and user from a login response.
   *
   * `persistent` decides which browser store both of them land in. Note that a session-scoped token
   * is also read back by the interceptor, so un-ticking "remember me" shortens the session's life
   * without breaking anything while the tab stays open.
   */
  const adoptSession = useCallback((authResponse, { persistent = true } = {}) => {
    writeToken(authResponse.token, { persistent });
    writeRefreshToken(authResponse.refreshToken, { persistent });
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

  /**
   * Adopts an account the server has just returned.
   *
   * A profile edit changes the name and avatar that the header, the nav and every "your listings"
   * label read out of this context, so the stored copy has to be replaced with the saved one. The
   * alternative — leaving it — leaves the UI showing the old name until the next sign-in, which
   * reads as the save having failed.
   */
  const applyUser = useCallback((nextUser) => {
    if (!nextUser) return null;
    persistUser(nextUser);
    setUser(nextUser);
    return nextUser;
  }, []);

  const value = useMemo(() => ({
    user,
    token,
    isAuthenticated: Boolean(token),
    isVerified: Boolean(user?.verified),
    signIn,
    signUp,
    signOut,
    verifyAccount,
    applyUser,
  }), [user, token, signIn, signUp, signOut, verifyAccount, applyUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}