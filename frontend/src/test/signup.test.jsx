import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import SignUp from '../pages/SignUp'

const authMocks = vi.hoisted(() => ({
  signUp: vi.fn(),
}))

vi.mock('../auth/useAuth', () => ({
  useAuth: () => ({ signUp: authMocks.signUp, isAuthenticated: false }),
}))

const renderSignup = () => render(
  <MemoryRouter initialEntries={['/signup']}>
    <Routes>
      <Route path="/signup" element={<SignUp />} />
      <Route path="/verification" element={<h1>Check your email</h1>} />
    </Routes>
  </MemoryRouter>,
)

const completeCommonFields = async (user, email) => {
  await user.type(screen.getByLabelText(/full name/i), 'Jane Doe')
  await user.type(screen.getByLabelText(/email/i), email)
  await user.type(screen.getByLabelText(/phone number/i), '+27 82 123 4567')
  await user.type(screen.getByLabelText(/set password/i), 'Password123')
}

beforeEach(() => {
  authMocks.signUp.mockReset()
  authMocks.signUp.mockResolvedValue({})
})

describe('signup account types', () => {
  it.each(['jane@university.ac.za', 'jane@campus.edu.za'])(
    'allows student registration with an approved academic email: %s',
    async (email) => {
    const user = userEvent.setup()
    renderSignup()
    await completeCommonFields(user, email)
    await user.click(screen.getByRole('button', { name: 'Register' }))

    expect(await screen.findByRole('heading', { name: /check your email/i })).toBeInTheDocument()
    expect(authMocks.signUp).toHaveBeenCalledWith(expect.objectContaining({
      role: 'STUDENT',
      email,
    }))
    },
  )

  it('rejects student emails outside the academic domains', async () => {
    const user = userEvent.setup()
    renderSignup()
    await completeCommonFields(user, 'jane@example.com')
    await user.click(screen.getByRole('button', { name: 'Register' }))

    expect(await screen.findByText(/Student emails must use an \.ac\.za or \.edu\.za domain/i))
      .toBeInTheDocument()
    expect(authMocks.signUp).not.toHaveBeenCalled()
  })

  it('collects vendor store details and submits them for approval', async () => {
    const user = userEvent.setup()
    renderSignup()
    await user.selectOptions(screen.getByLabelText(/signing up as/i), 'VENDOR')
    await completeCommonFields(user, 'jane@example.com')
    await user.type(screen.getByLabelText(/business or store name/i), 'Jane Books')
    await user.type(screen.getByLabelText(/business registration number/i), 'REG-123')
    await user.click(screen.getByRole('button', { name: 'Register' }))

    expect(await screen.findByRole('heading', { name: /check your email/i })).toBeInTheDocument()
    expect(authMocks.signUp).toHaveBeenCalledWith(expect.objectContaining({
      role: 'VENDOR',
      businessName: 'Jane Books',
      registrationNo: 'REG-123',
    }))
  })

  it('allows community members to sign up with a non-CPUT email', async () => {
    const user = userEvent.setup()
    renderSignup()
    await user.selectOptions(screen.getByLabelText(/signing up as/i), 'RESIDENT')
    await completeCommonFields(user, 'jane@example.com')
    await user.click(screen.getByRole('button', { name: 'Register' }))

    expect(await screen.findByRole('heading', { name: /check your email/i })).toBeInTheDocument()
    expect(authMocks.signUp).toHaveBeenCalledWith(expect.objectContaining({
      role: 'RESIDENT',
      email: 'jane@example.com',
    }))
  })
})
