# PRM Marketplace frontend

React 19 + Vite + Tailwind 4 single-page app for the student resale marketplace. It talks to the
Spring backend through same-origin `/api/...` paths; in development Vite proxies those to
`VITE_BACKEND_URL` (default `http://localhost:8080`).

## Commands

```bash
npm run dev         # dev server
npm run build       # production build to dist/
npm run preview     # serve the built bundle
npm run lint        # oxlint
npm test            # vitest, single run
npm run test:watch  # vitest in watch mode
```

## Tests

Vitest with jsdom. `src/test/setup.js` stubs the axios adapter so no test can reach the network —
without that, jsdom's relative `/api` URLs fail silently inside `useAsync`'s empty state and the
suite would pass while asserting nothing.

`src/test/routes.test.jsx` is a route smoke suite: it mounts every route in `App.jsx` and checks
that each one renders. It exists because `Home.jsx` once used `forwardRef` without importing it,
which threw while the module was being evaluated — and since `App.jsx` imports every page eagerly,
that one missing import took down every route in the app. `npm run build` still succeeded. The
route suite fails on that exact bug while the build does not, which is the whole reason it is there.

`src/test/client.test.js` covers token storage, including the "remember me" branch where
un-ticking the box has to actively remove the localStorage copy.

Browser-level end-to-end testing is not set up. `playwright` is a dev dependency but there is no
`@playwright/test` runner, no browser install, and no seeded backend to point it at.
