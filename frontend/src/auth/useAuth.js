import { useContext } from 'react';
import { AuthContext } from './auth-context';

/**
 * Reads the auth session.
 *
 * Throws rather than returning undefined when called outside the provider: a component that silently
 * sees a signed-out user because it was mounted in the wrong tree fails later and further away, and
 * the stack trace from here points straight at the mistake.
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside an AuthProvider');
  return context;
}