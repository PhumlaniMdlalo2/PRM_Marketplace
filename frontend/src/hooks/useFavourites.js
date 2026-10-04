import { useCallback, useEffect, useState } from 'react';
import { listSavedItems, toggleSavedItem } from '../api/savedItems';
import { useAuth } from '../auth/useAuth';

/**
 * Favourite state for a page full of product cards, shared by every card on it.
 *
 * Three pages needed the same thing — Home, Search and ProductDetails all had a heart that flipped
 * a piece of local state and told the server nothing, so a heart filled on Home was still empty after
 * a refresh, and the Saved items page disagreed with both. One hook, one request for the caller's
 * whole saved set, and a toggle that reports what the server actually decided.
 *
 * Two things it deliberately does not do:
 *
 * <ul>
 *   <li>It does not guess. `toggleSavedItem` returns whether the product is saved *now*, and that
 *       answer is what lands in state, so a save the server rejected cannot leave a filled heart
 *       lying about the database.</li>
 *   <li>It does not load for a signed-out visitor. The saved-items endpoint needs a token, so asking
 *       anonymously would produce the 401 that the interceptor answers by signing them out.</li>
 * </ul>
 *
 * The saved set is per account, not per page, so it is fetched once per session rather than once per
 * list of products. That is also why this hook takes no product ids: the heart asks
 * `isFavourite(id)` and the answer comes from the whole set. Passing the visible ids would only
 * suggest a filtering that does not happen, and would tie a refetch to every keystroke in a search.
 *
 * @returns {{
 *   isFavourite: (productId: string) => boolean,
 *   toggle: (productId: string) => Promise<boolean>,
 *   busyProductId: string | null,
 *   error: string | null
 * }}
 */
export function useFavourites() {
  const { isAuthenticated } = useAuth();
  const [saved, setSaved] = useState(() => new Set());
  const [busyProductId, setBusyProductId] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!isAuthenticated) return;

    // Cancellation rather than a mounted check: signing out mid-request must not leave the previous
    // account's saved set behind, and must not set state on an unmounted component.
    let current = true;
    listSavedItems()
      .then((entries) => {
        if (!current) return;
        setSaved(new Set(entries.filter((entry) => entry.product).map((entry) => entry.product.id)));
        setError(null);
      })
      .catch((caught) => {
        if (!current) return;
        // A failure here only costs the filled hearts, so it is reported without emptying the grid.
        setError(caught.message);
      });

    return () => {
      current = false;
    };
  }, [isAuthenticated]);

  // Signed out, nothing is saved — derived during render rather than stored, so signing out cannot
  // leave a stale heart behind for the request that was in flight at the time.
  const isFavourite = useCallback(
    (productId) => isAuthenticated && saved.has(productId),
    [isAuthenticated, saved],
  );

  /**
   * Saves or unsaves, and returns the state the server settled on.
   *
   * One product is in flight at a time: a second tap on the same heart while the first is still
   * travelling would toggle twice and land on the answer opposite to what one tap means.
   */
  const toggle = useCallback(
    async (productId) => {
      if (!isAuthenticated) return false;
      if (busyProductId === productId) return saved.has(productId);

      setBusyProductId(productId);
      setError(null);
      try {
        const nowSaved = await toggleSavedItem(productId);
        setSaved((current) => {
          const next = new Set(current);
          if (nowSaved) next.add(productId);
          else next.delete(productId);
          return next;
        });
        return nowSaved;
      } catch (caught) {
        setError(caught.message);
        return saved.has(productId);
      } finally {
        setBusyProductId(null);
      }
    },
    [busyProductId, isAuthenticated, saved],
  );

  return { isFavourite, toggle, busyProductId, error };
}