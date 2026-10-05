import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import ForgotPassword from '../pages/ForgotPassword'
import ResetPassword from '../pages/ResetPassword'
import { forgotPassword, resetPassword } from '../api/auth'

/**
 * The reset flow is the one path a user takes when they cannot authenticate, so it has no other way
 * in. `Login.jsx` has linked to /forgot-password since the button was written, with no route behind
 * it, which meant a locked-out user got a blank page instead of a way forward.
 *
 * The API module is mocked rather than the adapter, so each test states exactly what the server
 * answered. What matters here is not the request but the two things a user in this situation cannot
 * afford: being told whether an account exists, and being left on a dead page.
 */
vi.mock('../api/auth', () => ({
  forgotPassword: vi.fn(),
  resetPassword: vi.fn(),
}))

beforeEach(() => {
  vi.mocked(forgotPassword).mockReset()
  vi.mocked(resetPassword).mockReset()
})

describe('ForgotPassword', () => {
  it('rejects an address that is not an email before making a request', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter><ForgotPassword /></MemoryRouter>)

    await user.type(screen.getByLabelText(/email/i), 'not-an-address')
    await user.click(screen.getByRole('button', { name: /send reset token/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/valid email/i)
    // Nothing was sent, so no request was made.
    expect(forgotPassword).not.toHaveBeenCalled()
  })

  it('confirms with the same wording whether or not the account exists', async () => {
    const user = userEvent.setup()
    // The server answers 200 for an unknown address on purpose, so this is the success shape.
    vi.mocked(forgotPassword).mockResolvedValue({ message: 'sent' })

    render(<MemoryRouter><ForgotPassword /></MemoryRouter>)
    await user.type(screen.getByLabelText(/email/i), 'nobody@example.ac.za')
    await user.click(screen.getByRole('button', { name: /send reset token/i }))

    expect(await screen.findByRole('heading', { name: /check your email/i })).toBeInTheDocument()
    expect(screen.getByText(/if nobody@example.ac.za has an account/i)).toBeInTheDocument()
  })

  it('says nothing about whether the account exists when the server reports a bad address', async () => {
    const user = userEvent.setup()
    vi.mocked(forgotPassword).mockRejectedValue(Object.assign(
      new Error('Bad request'),
      { status: 400 },
    ))

    render(<MemoryRouter><ForgotPassword /></MemoryRouter>)
    await user.type(screen.getByLabelText(/email/i), 'still-not-an-address@example.ac.za')
    await user.click(screen.getByRole('button', { name: /send reset token/i }))

    // The 400 is about the format, not about whether the account exists, and the wording must not
    // turn into a hint either way.
    expect(await screen.findByRole('alert')).toHaveTextContent(/does not look like an email address/i)
  })
})

describe('ResetPassword', () => {
  const renderAt = (initialEntry) => render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/login" element={<h1>Welcome back</h1>} />
      </Routes>
    </MemoryRouter>,
  )

  it('prefills the token from the query string', () => {
    renderAt('/reset-password?token=abc-123')

    expect(screen.getByLabelText(/reset token/i)).toHaveValue('abc-123')
  })

  it('refuses mismatched passwords without calling the server', async () => {
    const user = userEvent.setup()
    renderAt('/reset-password?token=abc-123')

    await user.type(screen.getByLabelText(/^new password/i), 'hunter2hunter2')
    await user.type(screen.getByLabelText(/confirm new password/i), 'hunter2hunter2x')
    await user.click(screen.getByRole('button', { name: /update password/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/do not match/i)
    expect(resetPassword).not.toHaveBeenCalled()
  })

  it('enforces the server minimum length before making a request', async () => {
    const user = userEvent.setup()
    renderAt('/reset-password?token=abc-123')

    // The backend's ResetPasswordRequest requires 8 characters.
    await user.type(screen.getByLabelText(/^new password/i), 'short')
    await user.type(screen.getByLabelText(/confirm new password/i), 'short')
    await user.click(screen.getByRole('button', { name: /update password/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/at least 8 characters/i)
    expect(resetPassword).not.toHaveBeenCalled()
  })

  it('sends the token and the new password, then sends the user to sign in', async () => {
    const user = userEvent.setup()
    vi.mocked(resetPassword).mockResolvedValue({ message: 'Password updated' })

    renderAt('/reset-password?token=abc-123')
    await user.type(screen.getByLabelText(/^new password/i), 'hunter2hunter2')
    await user.type(screen.getByLabelText(/confirm new password/i), 'hunter2hunter2')
    await user.click(screen.getByRole('button', { name: /update password/i }))

    await waitFor(() => {
      expect(resetPassword).toHaveBeenCalledWith('abc-123', 'hunter2hunter2')
    })
    // No session is issued by /auth/reset-password, so there is nothing to be signed in with.
    expect(await screen.findByRole('heading', { name: /welcome back/i })).toBeInTheDocument()
  })

  it('shows the server message when the token has expired or already been used', async () => {
    const user = userEvent.setup()
    vi.mocked(resetPassword).mockRejectedValue(Object.assign(
      new Error('Reset token has expired'),
      { status: 400 },
    ))

    renderAt('/reset-password?token=stale-token')
    await user.type(screen.getByLabelText(/^new password/i), 'hunter2hunter2')
    await user.type(screen.getByLabelText(/confirm new password/i), 'hunter2hunter2')
    await user.click(screen.getByRole('button', { name: /update password/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/expired/i)
  })
})