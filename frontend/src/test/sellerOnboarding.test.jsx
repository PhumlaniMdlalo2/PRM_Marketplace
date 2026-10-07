import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import Settings from '../pages/Settings'
import CreateListing from '../pages/CreateListing'
import { createVendorProfile, getMyVendorProfile } from '../api/vendorProfile'

const authMocks = vi.hoisted(() => ({
  user: { id: 'student-1', name: 'Jane Student', email: 'jane@mycput.ac.za', role: 'STUDENT' },
}))

vi.mock('../auth/useAuth', () => ({
  useAuth: () => ({ user: authMocks.user, signOut: vi.fn() }),
}))

vi.mock('../api/vendorProfile', () => ({
  createVendorProfile: vi.fn(),
  getMyVendorProfile: vi.fn(),
  updateVendorProfile: vi.fn(),
}))

vi.mock('../components/layout/Layout', () => ({
  default: ({ children }) => children,
}))

const pendingProfile = {
  id: 'seller-1',
  businessName: 'Jane Books',
  registrationNo: null,
  verified: false,
}

beforeEach(() => {
  vi.mocked(createVendorProfile).mockReset()
  vi.mocked(getMyVendorProfile).mockReset()
  authMocks.user = {
    id: 'student-1',
    name: 'Jane Student',
    email: 'jane@mycput.ac.za',
    role: 'STUDENT',
  }
})

describe('student seller onboarding', () => {
  it('lets a student apply for a seller profile from Settings', async () => {
    const user = userEvent.setup()
    vi.mocked(getMyVendorProfile)
      .mockRejectedValueOnce({ status: 404, message: 'Not found' })
      .mockResolvedValue(pendingProfile)
    vi.mocked(createVendorProfile).mockResolvedValue(pendingProfile)

    render(<MemoryRouter><Settings /></MemoryRouter>)
    await user.click(screen.getByRole('button', { name: /seller profile/i }))
    await user.type(await screen.findByLabelText(/business name/i), 'Jane Books')
    await user.click(screen.getByRole('button', { name: /apply to become a seller/i }))

    expect(createVendorProfile).toHaveBeenCalledWith({
      businessName: 'Jane Books',
      registrationNo: '',
    })
    expect(await screen.findByText(/pending admin approval/i)).toBeInTheDocument()
  })

  it('blocks a student with a pending seller application from publishing listings', async () => {
    vi.mocked(getMyVendorProfile).mockResolvedValue(pendingProfile)

    render(<MemoryRouter><CreateListing /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: /seller approval pending/i })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /create listing/i })).not.toBeInTheDocument()
  })
})
