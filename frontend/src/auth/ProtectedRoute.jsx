import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './useAuth';

/**
 * Gates a route that needs a signed-in user.
 *
 * <p>Returning `Navigate` rather than rendering an error keeps the URL honest: the user lands on
 * the sign-in page and, once signed in, is sent to where they were actually trying to go. The
 * attempted path travels in `state.from`, which the login page reads.
 *
 * <p>There is deliberately no "checking your session" branch. `AuthProvider` initialises the token
 * synchronously from browser storage on its first render, so by the time any child renders, the
 * answer is already known. A guard that waited on a loading flag would only add a render pass in
 * which the decision had to be deferred.
 *
 * <p>This is a usability guard, not a security control. Every one of these endpoints is protected
 * server-side too, and the axios interceptor signs the user out on a 401. The point here is to avoid
 * showing a signed-out user a page of empty tables and unexplained failures.
 */
export default function ProtectedRoute({ children }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location.pathname }} />;

  return children;
}