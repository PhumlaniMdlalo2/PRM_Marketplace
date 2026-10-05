import '@testing-library/jest-dom/vitest'
import { afterEach, vi } from 'vitest'
import { cleanup } from '@testing-library/react'

// Every test renders into jsdom, so the DOM has to be torn down between tests or one test's
// markup is still mounted when the next one queries it.
afterEach(() => {
  cleanup()
  localStorage.clear()
  sessionStorage.clear()
  vi.clearAllMocks()
})

/**
 * Nothing in this suite is allowed to touch the network.
 *
 * The pages fetch on mount through the shared axios instance in `src/api/client.js`. Left alone in
 * jsdom those requests fail against a relative "/api" URL, and because `useAsync` swallows errors
 * into an empty state the suite would quietly pass while asserting nothing about the data. Stubbing
 * the adapter makes "no network" an enforced fact rather than an accident.
 *
 * Individual tests override this with `api.defaults.adapter = ...` to hand back real fixtures.
 */
vi.mock('axios', async () => {
  const actual = await vi.importActual('axios')
  return {
    ...actual,
    default: {
      ...actual.default,
      create: (config) => {
        const instance = actual.default.create(config)
        // Returning a resolved, empty body keeps components on their normal happy path.
        instance.defaults.adapter = async (requestConfig) => ({
          data: {},
          status: 200,
          statusText: 'OK',
          headers: {},
          config: requestConfig,
        })
        return instance
      },
    },
  }
})