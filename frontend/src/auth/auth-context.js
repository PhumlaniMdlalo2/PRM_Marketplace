import { createContext } from 'react';

/**
 * The context object lives in its own module so that `AuthContext.jsx` can export only a component
 * and `useAuth.js` only a hook. Keeping all three in one file trips React Fast Refresh: editing the
 * provider would then full-reload the page instead of hot-swapping, losing component state on every
 * keystroke-triggered save.
 */
export const AuthContext = createContext(null);