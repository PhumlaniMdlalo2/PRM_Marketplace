import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import Login from '../pages/Login'
import Verification from '../pages/Verification'
import { resendCode } from '../api/auth'

const authMocks = vi.hoisted(() => ({
  signIn: vi.fn(),
  verifyAccount: vi.fn(),
}))

vi.mock('../auth/useAuth', () => ({
  useAuth: () => ({ ...authMocks, isAuthenticated: false }),
}))

vi.mock('../api/auth', () => ({
  resendCode: vi.fn(),
}))

beforeEach(() => {
  authMocks.signIn.mockReset()
  authMocks.verifyAccount.mockReset()
  vi.mocked(resendCode).mockReset()
})

describe('Verification recovery', () => {
  it('lets a user without navigation state enter an email and request a new code', async () => {
    const user = userEvent.setup()
    vi.mocked(resendCode).mockResolvedValue({ message: 'Verification code sent' })

    render(
      <MemoryRouter initialEntries={['/verification']}>
        <Verification />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText(/email/i), 'buyer@example.ac.za')
    await user.click(screen.getByRole('button', { name: /resend code/i }))

    await waitFor(() => {
      expect(resendCode).toHaveBeenCalledWith('buyer@example.ac.za')
    })
    expect(await screen.findByRole('status')).toHaveTextContent(/new verification code was sent/i)
  })

  it('offers the verification recovery page after an unverified login attempt', async () => {
    const user = userEvent.setup()
    authMocks.signIn.mockRejectedValue(new Error('Account is not verified yet'))

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/verification" element={<Verification />} />
        </Routes>
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText(/email/i), 'buyer@example.ac.za')
    await user.type(screen.getByLabelText(/^password$/i), 'password123')
    await user.click(screen.getByRole('button', { name: /log in/i }))
    await user.click(await screen.findByRole('link', { name: /verify your account/i }))

    expect(screen.getByLabelText(/email/i)).toHaveValue('buyer@example.ac.za')
    expect(screen.getByRole('heading', { name: /almost there/i })).toBeInTheDocument()
  })

  it('shows mail delivery errors instead of claiming a code was sent', async () => {
    const user = userEvent.setup()
    vi.mocked(resendCode).mockRejectedValue(new Error('Email delivery is temporarily unavailable'))

    render(
      <MemoryRouter initialEntries={[{ pathname: '/verification', state: { email: 'buyer@example.ac.za' } }]}>
        <Verification />
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: /resend code/i }))

    expect(await screen.findByText(/email delivery is temporarily unavailable/i)).toBeInTheDocument()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
})
