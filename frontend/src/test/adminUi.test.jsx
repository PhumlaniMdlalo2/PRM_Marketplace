import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import AdminLogin from '../pages/AdminLogin'
import AdminRoute from '../auth/AdminRoute'

const auth = vi.hoisted(() => ({
  isAuthenticated: false,
  user: null,
  signIn: vi.fn(),
  signOut: vi.fn(),
}))

vi.mock('../auth/useAuth', () => ({
  useAuth: () => auth,
}))

const renderAdminLogin = () => render(
  <MemoryRouter initialEntries={['/admin/login']}>
    <Routes>
      <Route path="/admin/login" element={<AdminLogin />} />
      <Route path="/admin" element={<h1>Admin dashboard</h1>} />
    </Routes>
  </MemoryRouter>,
)

beforeEach(() => {
  auth.isAuthenticated = false
  auth.user = null
  auth.signIn.mockReset()
  auth.signOut.mockReset()
})

describe('admin sign-in', () => {
  it('uses the configured admin email and only enters the portal for an admin account', async () => {
    const user = userEvent.setup()
    auth.signIn.mockResolvedValue({ role: 'ADMIN' })

    renderAdminLogin()
    expect(screen.getByLabelText(/admin email/i)).toHaveValue('admin.vendra@gmail.com')
    await user.type(screen.getByLabelText(/^password$/i), 'admin-test-passphrase')
    await user.click(screen.getByRole('button', { name: /sign in to admin/i }))

    expect(auth.signIn).toHaveBeenCalledWith('admin.vendra@gmail.com', 'admin-test-passphrase')
    expect(await screen.findByRole('heading', { name: 'Admin dashboard' })).toBeInTheDocument()
  })

  it('rejects accounts without the admin role', async () => {
    const user = userEvent.setup()
    auth.signIn.mockResolvedValue({ role: 'STUDENT' })

    renderAdminLogin()
    await user.type(screen.getByLabelText(/^password$/i), 'admin-test-passphrase')
    await user.click(screen.getByRole('button', { name: /sign in to admin/i }))

    expect(auth.signOut).toHaveBeenCalledOnce()
    expect(await screen.findByRole('alert')).toHaveTextContent(/does not have administrator access/i)
  })

  it('redirects signed-out users from protected admin pages to the admin sign-in', () => {
    render(
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route path="/admin" element={<AdminRoute><h1>Private admin page</h1></AdminRoute>} />
          <Route path="/admin/login" element={<h1>Admin sign in</h1>} />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Admin sign in' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Private admin page' })).not.toBeInTheDocument()
  })
})
