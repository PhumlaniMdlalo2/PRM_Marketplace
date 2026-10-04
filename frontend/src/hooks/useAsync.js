import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Runs an async loader and tracks the three things every page needs: the data, whether a request
 * is in flight, and a message if the last one failed.
 *
 * `loader` is expected to be wrapped in the caller's own `useCallback`. That is deliberate: an
 * earlier version took a `deps` array and forwarded it, which meant the hook decided when to refetch
 * from a list the linter could not see and the caller could not verify. Making the caller memoise the
 * loader moves the dependency list to the call site where it is a plain array literal, so
 * exhaustive-deps can actually check it — and it also gives a natural home for the stale-response
 * handling below.
 */
export function useAsync(loader, { immediate = true } = {}) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(immediate);
  const [error, setError] = useState(null);

  // Identifies the newest request. A response is only allowed to touch state when its own number
  // is still the current one, which is what stops a slow earlier request from overwriting the
  // result of a later one: type into the search box, the first keystroke's response arrives after
  // the second's, and without this the list ends up showing the older results.
  const latestRequest = useRef(0);

  const run = useCallback(async (...args) => {
    const requestId = ++latestRequest.current;
    setLoading(true);
    setError(null);
    try {
      const result = await loader(...args);
      if (requestId === latestRequest.current) {
        setData(result);
        setLoading(false);
      }
      return result;
    } catch (caught) {
      if (requestId === latestRequest.current) {
        // Already normalised by the axios response interceptor, so this is a plain object with
        // message/fieldErrors rather than an Error instance.
        setError(caught);
        setLoading(false);
      }
      return null;
    }
  }, [loader]);

  useEffect(() => {
    if (immediate) run();
  }, [run, immediate]);

  return { data, loading, error, run, setData };
}