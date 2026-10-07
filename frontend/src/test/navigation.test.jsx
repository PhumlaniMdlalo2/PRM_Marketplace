import { describe, expect, it } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../auth/AuthContext'
import BottomNavigation from '../components/layout/BottomNavigation'
import TopNavigation from '../components/layout/TopNavigation'

const renderNavigation = (initialEntry) => render(
  <AuthProvider>
    <MemoryRouter initialEntries={[initialEntry]}>
      <TopNavigation />
      <BottomNavigation />
    </MemoryRouter>
  </AuthProvider>,
)

describe('main navigation active state', () => {
  it('highlights only Home on the marketplace route', () => {
    renderNavigation('/marketplace')

    for (const navigation of [
      screen.getByRole('navigation', { name: 'Primary' }),
      screen.getByRole('navigation', { name: 'Main navigation' }),
    ]) {
      expect(within(navigation).getByRole('link', { name: 'Home' })).toHaveAttribute('aria-current', 'page')
      expect(within(navigation).queryByRole('link', { name: 'For You' })).not.toBeInTheDocument()
    }
  })

  it('keeps Home active at the For You section anchor without a separate For You tab', () => {
    renderNavigation('/marketplace#for-you')

    for (const navigation of [
      screen.getByRole('navigation', { name: 'Primary' }),
      screen.getByRole('navigation', { name: 'Main navigation' }),
    ]) {
      expect(within(navigation).getByRole('link', { name: 'Home' })).toHaveAttribute('aria-current', 'page')
      expect(within(navigation).queryByRole('link', { name: 'For You' })).not.toBeInTheDocument()
    }
  })
})
