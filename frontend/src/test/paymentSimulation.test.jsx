import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import OrderDetail from '../pages/OrderDetail'
import { getOrder, listOrderItems } from '../api/orders'
import {
  isPaymentSimulationEnabled, listPaymentInstructions, simulatePayment,
} from '../api/payments'

vi.mock('../api/orders', () => ({
  cancelOrder: vi.fn(),
  getOrder: vi.fn(),
  listOrderItems: vi.fn(),
}))

vi.mock('../api/messages', () => ({
  startConversation: vi.fn(),
}))

vi.mock('../api/payments', () => ({
  isPaymentSimulationEnabled: vi.fn(),
  listPaymentInstructions: vi.fn(),
  simulatePayment: vi.fn(),
  updatePaymentStatus: vi.fn(),
}))

vi.mock('../components/layout/Layout', () => ({
  default: ({ children }) => children,
}))

const order = {
  id: 'order-1',
  status: 'PENDING',
  totalAmount: '450.00',
  createdAt: '2026-10-06T10:00:00',
}

const pendingPayment = {
  paymentId: 'payment-1',
  orderId: 'order-1',
  method: 'SANDBOX',
  status: 'PENDING',
  amount: '450.00',
  transactionReference: 'PAY-SANDBOX-1',
}

beforeEach(() => {
  vi.mocked(getOrder).mockResolvedValue(order)
  vi.mocked(listOrderItems).mockResolvedValue([])
  vi.mocked(isPaymentSimulationEnabled).mockResolvedValue(true)
  vi.mocked(listPaymentInstructions).mockResolvedValue([pendingPayment])
  vi.mocked(simulatePayment).mockReset()
  vi.mocked(simulatePayment).mockResolvedValue({ ...pendingPayment, status: 'COMPLETED' })
})

it('shows the seller transfer details and the unique EFT payment reference', async () => {
  vi.mocked(listPaymentInstructions).mockResolvedValue([{
    ...pendingPayment,
    method: 'EFT',
    sellerName: 'Campus Books',
    payoutDetails: {
      accountHolder: 'Campus Books',
      bankName: 'Example Bank',
      accountNumber: '12345678',
      branchCode: '123456',
      accountType: 'CURRENT',
    },
  }])
  renderOrderDetail()

  expect(await screen.findByText(/Bank:\s*Example Bank/)).toBeInTheDocument()
  expect(screen.getByText(/Account number:\s*12345678/)).toBeInTheDocument()
  expect(screen.getAllByText(/PAY-SANDBOX-1/)).toHaveLength(2)
})

const renderOrderDetail = () => render(
  <MemoryRouter initialEntries={['/orders/order-1']}>
    <Routes>
      <Route path="/orders/:id" element={<OrderDetail />} />
      <Route path="/orders" element={<h1>Your orders</h1>} />
    </Routes>
  </MemoryRouter>,
)

describe('order fulfillment and tracking', () => {
  it('shows the delivery estimate, destination and current delivery stage', async () => {
    vi.mocked(getOrder).mockResolvedValue({
      ...order,
      status: 'SHIPPED',
      fulfillmentMethod: 'DELIVERY',
      shippingAddress: { singleLine: '12 Main Road, Cape Town' },
      estimatedDeliveryDate: '2026-10-14',
    })
    vi.mocked(listPaymentInstructions).mockResolvedValue([])

    renderOrderDetail()

    expect(await screen.findByText('12 Main Road, Cape Town')).toBeInTheDocument()
    expect(screen.getByText(/estimated delivery by/i)).toBeInTheDocument()
    expect(screen.getAllByText('On the way')).toHaveLength(2)
    expect(screen.getByRole('list', { name: 'Order progress' })).toBeInTheDocument()
  })

  it('shows meetup readiness without a delivery estimate', async () => {
    vi.mocked(getOrder).mockResolvedValue({
      ...order,
      status: 'SHIPPED',
      fulfillmentMethod: 'MEETUP',
    })
    vi.mocked(listPaymentInstructions).mockResolvedValue([])

    renderOrderDetail()

    expect(await screen.findByText('Arrange a safe time and place with each seller through marketplace messages.'))
      .toBeInTheDocument()
    expect(screen.getAllByText('Ready for meetup')).toHaveLength(2)
    expect(screen.queryByText(/estimated delivery by/i)).not.toBeInTheDocument()
  })
})

describe('sandbox payment simulation', () => {
  it('clearly labels simulated outcomes and records an explicitly selected success', async () => {
    renderOrderDetail()

    expect(await screen.findByText(/no money moves/i)).toBeInTheDocument()
    fireEvent.click(await screen.findByRole('button', { name: /simulate success/i }))

    expect(simulatePayment).toHaveBeenCalledWith('payment-1', 'SUCCESS')
  })

  it('offers a separate explicit failure result', async () => {
    renderOrderDetail()

    fireEvent.click(await screen.findByRole('button', { name: /simulate failure/i }))

    expect(simulatePayment).toHaveBeenCalledWith('payment-1', 'FAILURE')
  })
})
