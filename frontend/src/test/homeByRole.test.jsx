import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Home from '../pages/Home'
import { useAuth } from '../auth/useAuth'
import { getMyVendorProfile } from '../api/vendorProfile'
import { search, listMine, searchAcrossCategories } from '../api/products'
import { confirmPaymentReceipt, listSellerPayments } from '../api/payments'
import { GOODS_CATEGORIES } from '../lib/marketplaceCategories'
import { CATEGORY_OPTIONS } from '../lib/listingForm'

vi.mock('../auth/useAuth', () => ({
  useAuth: vi.fn(),
}))

vi.mock('../api/vendorProfile', () => ({
  getMyVendorProfile: vi.fn(),
}))

vi.mock('../api/products', () => ({
  search: vi.fn(),
  searchAcrossCategories: vi.fn(),
  listMine: vi.fn(),
}));

vi.mock('../api/payments', () => ({
  confirmPaymentReceipt: vi.fn(),
  listSellerPayments: vi.fn(),
}))

vi.mock('../hooks/useFavourites', () => ({
  useFavourites: () => ({
    error: null,
    isFavourite: () => false,
    toggle: vi.fn(),
  }),
}))

vi.mock('../components/layout/Layout', () => ({
  default: ({ children }) => children,
}))

const sellerProfile = {
  id: 'seller-1',
  businessName: 'Harbour Books',
  verified: true,
}

const pendingPayment = {
  paymentId: 'payment-1',
  orderId: 'order-1',
  method: 'EFT',
  status: 'PENDING',
  amount: 215,
  transactionReference: 'PAY-ORDER1',
}

beforeEach(() => {
  vi.clearAllMocks()
  search.mockResolvedValue({ content: [], totalElements: 0 })
  searchAcrossCategories.mockResolvedValue({ content: [], totalElements: 0 })
  listMine.mockResolvedValue([])
  listSellerPayments.mockResolvedValue([])
  confirmPaymentReceipt.mockResolvedValue({})
})

describe('home by account type', () => {
  it('shows a verified student seller their shop workspace instead of the buyer catalogue', async () => {
    useAuth.mockReturnValue({
      user: { id: 'student-1', name: 'Tumi', role: 'STUDENT' },
    })
    getMyVendorProfile.mockResolvedValue(sellerProfile)
    listMine.mockResolvedValue([
      { id: 'listing-1', name: 'Design notebook', price: 85, stockQuantity: 3, active: true },
    ])
    listSellerPayments.mockResolvedValue([pendingPayment])

    render(<MemoryRouter><Home /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: /good to see you, harbour books/i }))
      .toBeInTheDocument()
    expect(await screen.findByText('Design notebook')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Payments to confirm' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /confirm cleared eft/i })).toBeInTheDocument()
    expect(screen.queryByRole('tablist', { name: /product categories/i })).not.toBeInTheDocument()
    expect(search).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: /confirm cleared eft/i }))
    await waitFor(() => expect(confirmPaymentReceipt).toHaveBeenCalledWith('payment-1'))
  })

  it('keeps community members on the buyer catalogue', async () => {
    useAuth.mockReturnValue({
      user: { id: 'resident-1', name: 'Anele', role: 'RESIDENT' },
    })

    render(<MemoryRouter><Home /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: 'Featured goods' })).toBeInTheDocument()
    expect(screen.getByRole('tablist', { name: /marketplace sections/i })).toBeInTheDocument()
    expect(getMyVendorProfile).not.toHaveBeenCalled()
  })

  it('lets buyers browse service listings by service category', async () => {
    useAuth.mockReturnValue({
      user: { id: 'resident-1', name: 'Anele', role: 'RESIDENT' },
    })

    render(<MemoryRouter><Home /></MemoryRouter>)

    await screen.findByRole('heading', { name: 'Featured goods' })
    fireEvent.click(screen.getByRole('tab', { name: 'Services' }))

    expect(screen.getByRole('heading', { name: 'Featured services' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Community' })).toHaveAttribute('href', '/bulletin')
    fireEvent.click(screen.getAllByRole('button', { name: 'Tutoring and academic help' })[0])

    await waitFor(() => {
      expect(search).toHaveBeenCalledWith(expect.objectContaining({
        category: 'Tutoring and academic help',
      }))
    })
  })

  it('groups existing bike listings under sports and hobbies', async () => {
    useAuth.mockReturnValue({
      user: { id: 'resident-1', name: 'Anele', role: 'RESIDENT' },
    })

    render(<MemoryRouter><Home /></MemoryRouter>)

    await screen.findByRole('heading', { name: 'Featured goods' })
    expect(GOODS_CATEGORIES.some(({ label }) => label === 'Bikes')).toBe(false)
    expect(CATEGORY_OPTIONS).toContain('Sports and hobbies')
    expect(CATEGORY_OPTIONS).not.toContain('Bikes')

    fireEvent.click(screen.getAllByRole('button', { name: 'Sports and hobbies' })[0])
    await waitFor(() => {
      expect(search).toHaveBeenCalledWith(expect.objectContaining({
        category: 'Sports and hobbies,Bikes',
      }))
    })
  })

  it('keeps students without a seller profile on the buyer catalogue', async () => {
    useAuth.mockReturnValue({
      user: { id: 'student-1', name: 'Tumi', role: 'STUDENT' },
    })
    getMyVendorProfile.mockRejectedValue({ status: 404, message: 'Not found' })

    render(<MemoryRouter><Home /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: 'Featured goods' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows seller onboarding to a vendor account without a seller profile', async () => {
    useAuth.mockReturnValue({
      user: { id: 'vendor-1', name: 'Musa', role: 'VENDOR' },
    })
    getMyVendorProfile.mockRejectedValue({ status: 404, message: 'Not found' })

    render(<MemoryRouter><Home /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: /set up your shop/i }))
      .toBeInTheDocument()
    expect(screen.getByRole('link', { name: /open seller settings/i })).toHaveAttribute('href', '/settings')
    expect(search).not.toHaveBeenCalled()
  })
})
