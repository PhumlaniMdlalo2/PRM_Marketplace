import { afterEach, describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { AuthProvider } from '../auth/AuthContext'
import { useAuth } from '../auth/useAuth'

const CurrentRole = () => {
  const { user } = useAuth()
  return <span>{user?.role}</span>
}

afterEach(() => {
  window.localStorage.clear()
})

describe('stored authentication session', () => {
  it('migrates a cached faculty role to admin', () => {
    window.localStorage.setItem('prm.user', JSON.stringify({
      id: 'admin-1',
      name: 'Marketplace Admin',
      email: 'admin@example.ac.za',
      role: 'FACULTY',
    }))

    render(
      <AuthProvider>
        <CurrentRole />
      </AuthProvider>,
    )

    expect(screen.getByText('ADMIN')).toBeInTheDocument()
    expect(JSON.parse(window.localStorage.getItem('prm.user')).role).toBe('ADMIN')
  })
})
