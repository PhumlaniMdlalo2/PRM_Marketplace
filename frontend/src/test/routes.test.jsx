import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { AuthProvider } from '../auth/AuthContext'
import App from '../App'
import { TOKEN_STORAGE_KEY } from '../api/client'

/**
 * Route smoke tests.
 *
 * These exist because of a specific failure this project shipped: `Home.jsx` used `forwardRef`
 * without importing it, which threw a ReferenceError while the module was being evaluated. Because
 * `App.jsx` imports every page eagerly, that one missing import took down *every* route in the app,
 * and nothing caught it -- there was no test suite, and the build still succeeded.
 *
 * So the assertion under test is deliberately blunt: every route must mount without throwing and
 * must put something on screen. It is not trying to check layout or copy, only that the page is
 * reachable and alive.
 */

/** Public routes, with one landmark that proves the right page rendered rather than a blank shell. */
const PUBLIC_ROUTES = [
  { path: '/', landmark: () => screen.findByRole('heading', { name: /a marketplace for campus life/i }) },
  { path: '/marketplace', landmark: () => screen.getAllByRole('tablist') },
  { path: '/search', landmark: () => screen.findByRole('heading', { name: /browse goods/i }) },
  { path: '/product/6f1a1f9e-0000-4000-8000-000000000001', landmark: null },
  { path: '/login', landmark: () => screen.findByRole('heading', { name: /welcome back/i }) },
  { path: '/signup', landmark: () => screen.findByRole('heading', { name: /create your account/i }) },
  { path: '/forgot-password', landmark: () => screen.findByRole('heading', { name: /reset your password/i }) },
  { path: '/reset-password', landmark: () => screen.findByRole('heading', { name: /choose a new password/i }) },
  {
    path: '/verification',
    // Reached the way SignUp reaches it, with the address it hands over in router state.
    state: { email: 'buyer@example.ac.za' },
    landmark: () => screen.findByRole('heading', { name: /almost there/i }),
  },
  { path: '/bulletin', landmark: () => screen.findByRole('heading', { name: /bulletin/i }) },
  { path: '/bulletin/7b2b2f9e-0000-4000-8000-000000000001', landmark: null },
  { path: '/vendors', landmark: () => screen.findByRole('heading', { name: 'Sellers' }) },
  { path: '/vendors/4a1c5b2e-0000-4000-8000-000000000001', landmark: null },
  { path: '/nope', landmark: null },
]

/** Routes behind ProtectedRoute. Each needs a token in storage or it redirects to /login. */
const PROTECTED_ROUTES = [
  '/messages',
  '/messages/8c3c3f9e-0000-4000-8000-000000000001',
  '/orders',
  '/cart',
  '/profile',
  '/profile/edit',
  '/settings',
  '/saved',
  '/orders/2e5e5f9e-0000-4000-8000-000000000001',
  '/listing/create',
  '/listing/edit/9d4d4f9e-0000-4000-8000-000000000001',
  '/listing/mine',
  '/bulletin/create',
  '/student-groups',
]

const USER = { id: '11111111-1111-1111-1111-111111111111', name: 'Test Buyer', role: 'STUDENT', verified: true }

/**
 * `usr` is the key React Router keeps its own location state under, so pushing it here is the only
 * way to arrive at a route the way the app actually navigates to it. `/verification` uses router
 * state after signup when available, but also supports direct navigation for account recovery.
 */
const renderAppAt = (path, routerState = undefined) => {
  window.history.pushState(routerState === undefined ? {} : { usr: routerState }, '', path)
  return render(
    <AuthProvider>
      <App />
    </AuthProvider>,
  )
}

const signIn = () => {
  localStorage.setItem(TOKEN_STORAGE_KEY, 'test.token.value')
  localStorage.setItem('prm.user', JSON.stringify(USER))
}

beforeEach(() => {
  window.history.pushState({}, '', '/')
})

describe('public routes mount', () => {
  it.each(PUBLIC_ROUTES)('$path renders without throwing', async ({ path, landmark, state }) => {
    const { container } = renderAppAt(path, state)

    expect(container.firstChild).not.toBeNull()

    // Pages that fetch show a skeleton first, and a skeleton is deliberately text-free, so "put
    // something on screen" has to wait for the load to finish rather than assert immediately.
    await waitFor(() => expect(container.textContent?.trim().length).toBeGreaterThan(0))

    if (landmark) expect(await landmark()).toBeTruthy()
  })
})

describe('protected routes mount for a signed-in user', () => {
  it.each(PROTECTED_ROUTES)('$path renders without throwing', (path) => {
    signIn()
    const { container } = renderAppAt(path)

    expect(container.firstChild).not.toBeNull()
    expect(container.textContent?.trim().length).toBeGreaterThan(0)

    // A redirect to /login would leave "Welcome back" on screen instead of the page.
    expect(screen.queryByRole('heading', { name: /welcome back/i })).toBeNull()
  })
})

describe('protected routes redirect a signed-out visitor', () => {
  it.each(PROTECTED_ROUTES)('$path sends the visitor to sign in', (path) => {
    renderAppAt(path)

    expect(screen.getByRole('heading', { name: /welcome back/i })).toBeInTheDocument()
  })
})

describe('a route that does not exist', () => {
  it('renders the not-found page rather than a blank screen', () => {
    const { container } = renderAppAt('/definitely-not-a-route')

    expect(container.textContent?.trim().length).toBeGreaterThan(0)
  })
})