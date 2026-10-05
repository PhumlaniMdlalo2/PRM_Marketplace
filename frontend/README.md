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
without that, jsdom's relative `/api` URLs fail silently inside `useAsync`'s empty state and the suite
would pass while asserting nothing.

| File | Covers |
| --- | --- |
| `src/test/routes.test.jsx` | Every route in `App.jsx` mounts, and protected routes redirect a signed-out visitor. |
| `src/test/client.test.js` | Token storage, including the "remember me" branch where un-ticking the box has to actively remove the `localStorage` copy. |
| `src/test/vendorRating.test.jsx` | The seller card's rating and review count, including the case where the two disagree. |
| `src/test/passwordReset.test.jsx` | The reset-request and reset forms, including a server that rejects the address. |
| `src/test/formControls.test.jsx` | The shared `Input`, including the label being associated with its control. |

`routes.test.jsx` exists because `Home.jsx` once used `forwardRef` without importing it, which threw
while the module was being evaluated — and since `App.jsx` imports every page eagerly, that one
missing import took down every route in the app. `npm run build` still succeeded. The route suite
fails on that exact bug while the build does not, which is the whole reason it is there. **A route
added to `App.jsx` belongs in that list.**

## Browser scripts

`scripts/` holds two Playwright scripts. They are run by hand against a dev server, not part of
`npm test`, and they are not browser-level end-to-end tests: there is no `@playwright/test` runner, no
browser install step, and no seeded backend.

```bash
npm run dev                                    # in one terminal
node scripts/check-layout.mjs                  # visit every route at 4 viewports, report overflow
node scripts/screenshots.mjs                   # write PNGs to ../shots (gitignored)
```

Both read `BASE_URL` (default `http://localhost:5173`) and need a signed-in user for the protected
routes, which is why they mostly capture the sign-in screen unless you log in first. `playwright` is
a dev dependency but its browsers are not installed by `npm install`; run `npx playwright install`
first.
