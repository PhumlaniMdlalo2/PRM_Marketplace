import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import NotificationBell from '../components/layout/NotificationBell'
import { AuthProvider } from '../auth/AuthContext'
import { TOKEN_STORAGE_KEY } from '../api/client'
import {
  countUnreadNotifications,
  dismissNotification,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '../api/notifications'

/**
 * The backend writes notifications for payments and orders, and the app used to have no place to
 * read them, so a badge showing a count that was never fetched or cleared would be the same gap
 * one step further along.
 *
 * The cases worth pinning down are therefore the ones that make the badge and the list agree: it
 * must be counted for a signed-in visitor only, decremented when a row is marked read, and reset
 * when everything is read, each of which can drift from the server if the optimistic update is
 * written carelessly.
 */

vi.mock('../api/notifications', () => ({
  countUnreadNotifications: vi.fn(),
  listNotifications: vi.fn(),
  markNotificationRead: vi.fn(),
  markAllNotificationsRead: vi.fn(),
  dismissNotification: vi.fn(),
}))

const notification = (overrides = {}) => ({
  id: '99999999-9999-4999-8999-999999999999',
  type: 'ORDER',
  title: 'New order received',
  message: 'Order 42 for R100 is waiting for you to confirm.',
  read: false,
  readAt: null,
  createdAt: new Date(Date.now() - 5 * 60_000).toISOString(),
  ...overrides,
})

const signIn = () => {
  localStorage.setItem(TOKEN_STORAGE_KEY, 'test.token.value')
  localStorage.setItem('prm.user', JSON.stringify({ id: 'user-1', name: 'Buyer' }))
}

const renderBell = (variant) =>
  render(
    <MemoryRouter>
      <AuthProvider>
        <NotificationBell variant={variant} />
      </AuthProvider>
    </MemoryRouter>,
  )

beforeEach(() => {
  signIn()
  countUnreadNotifications.mockResolvedValue(0)
  listNotifications.mockResolvedValue([])
  markNotificationRead.mockResolvedValue(null)
  markAllNotificationsRead.mockResolvedValue(0)
  dismissNotification.mockResolvedValue(true)
})

describe('NotificationBell', () => {
  it('is absent for a signed-out visitor, who has nothing to be notified about', () => {
    localStorage.clear()

    renderBell()

    expect(screen.queryByRole('button')).toBeNull()
    expect(countUnreadNotifications).not.toHaveBeenCalled()
  })

  it('shows the unread count the server reports', async () => {
    countUnreadNotifications.mockResolvedValue(3)

    renderBell()

    expect(await screen.findByRole('button', { name: 'Notifications, 3 unread' })).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
  })

  it('shows no badge at all when nothing is unread', async () => {
    renderBell()

    expect(await screen.findByRole('button', { name: 'Notifications' })).toBeInTheDocument()
    expect(screen.queryByLabelText(/unread/)).toBeNull()
  })

  it('hides the badge while the count cannot be fetched', async () => {
    countUnreadNotifications.mockRejectedValue(new Error('offline'))

    renderBell()

    // A count the server would not give is not a reason to put an error on every page in the app,
    // and it is certainly not a reason to show a badge of zero, which reads as "nothing new"
    // rather than "we do not know".
    expect(await screen.findByRole('button', { name: 'Notifications' })).toBeInTheDocument()
    expect(screen.queryByLabelText(/unread/)).toBeNull()
  })

  it('lists what the server has when opened', async () => {
    listNotifications.mockResolvedValue([
      notification({ title: 'Payment successful', message: 'Your payment of R780 was successful.' }),
      notification({
        id: '00000000-0000-4000-8000-000000000001',
        read: true,
        readAt: new Date().toISOString(),
        title: 'Order confirmed',
        message: 'Your order 42 is now confirmed.',
      }),
    ])

    renderBell()
    fireEvent.click(await screen.findByRole('button', { name: 'Notifications' }))

    expect(await screen.findByText('Payment successful')).toBeInTheDocument()
    expect(screen.getByText('Your order 42 is now confirmed.')).toBeInTheDocument()
    expect(listNotifications).toHaveBeenCalledTimes(1)
  })

  it('marks one read and drops the badge with it', async () => {
    countUnreadNotifications.mockResolvedValue(1)
    listNotifications.mockResolvedValue([notification()])

    renderBell()
    fireEvent.click(await screen.findByRole('button', { name: 'Notifications, 1 unread' }))
    const row = await screen.findByLabelText(
      /Mark "New order received" as read/,
    )
    fireEvent.click(row)

    await waitFor(() => {
      expect(markNotificationRead).toHaveBeenCalledWith(notification().id)
    })
    expect(screen.getByRole('button', { name: 'Notifications' })).toBeInTheDocument()
  })

  it('puts the count back when the server refuses to mark one read', async () => {
    countUnreadNotifications.mockResolvedValue(1)
    listNotifications.mockResolvedValue([notification()])
    markNotificationRead.mockRejectedValue(new Error('gone'))

    renderBell()
    fireEvent.click(await screen.findByRole('button', { name: 'Notifications, 1 unread' }))
    fireEvent.click(await screen.findByLabelText(/Mark "New order received" as read/))

    await waitFor(() => {
      expect(markNotificationRead).toHaveBeenCalled()
    })
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Notifications, 1 unread' })).toBeInTheDocument()
    })
  })

  it('marks everything read in one call and clears the badge', async () => {
    countUnreadNotifications.mockResolvedValue(2)
    listNotifications.mockResolvedValue([
      notification(),
      notification({ id: '00000000-0000-4000-8000-000000000002', title: 'Payment failed' }),
    ])

    renderBell()
    fireEvent.click(await screen.findByRole('button', { name: 'Notifications, 2 unread' }))
    fireEvent.click(await screen.findByLabelText('Mark all notifications as read'))

    await waitFor(() => {
      expect(markAllNotificationsRead).toHaveBeenCalledTimes(1)
    })
    expect(screen.getByRole('button', { name: 'Notifications' })).toBeInTheDocument()
  })

  it('reports a refusal to load the list rather than showing an empty one', async () => {
    listNotifications.mockRejectedValue(new Error('offline'))

    renderBell()
    fireEvent.click(await screen.findByRole('button', { name: 'Notifications' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Notifications could not be loaded.',
    )
  })

  it('dismisses a row from the list', async () => {
    listNotifications.mockResolvedValue([notification()])

    renderBell()
    fireEvent.click(await screen.findByRole('button', { name: 'Notifications' }))
    fireEvent.click(await screen.findByLabelText(/Dismiss "New order received"/))

    await waitFor(() => {
      expect(dismissNotification).toHaveBeenCalledWith(notification().id)
    })
    expect(screen.queryByText('New order received')).toBeNull()
  })

  it('is labelled for the mobile tab', () => {
    renderBell('tab')

    expect(screen.getByRole('button', { name: 'Notifications' })).toBeInTheDocument()
  })
})
