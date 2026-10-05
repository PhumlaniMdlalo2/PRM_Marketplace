import { describe, it, expect } from 'vitest'
import { readToken, writeToken, TOKEN_STORAGE_KEY } from '../api/client'

/**
 * Token storage is the one piece of client.js that has real branching, and the branching is a
 * security decision: "remember me" is only meaningful if un-ticking it actively removes the
 * localStorage copy rather than merely adding a second one somewhere.
 *
 * These tests also underpin the route suite, which signs in by writing a token directly.
 */
describe('writeToken', () => {
  it('defaults to a persistent session in localStorage', () => {
    writeToken('abc123')

    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBe('abc123')
    expect(sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull()
  })

  it('keeps a session-scoped token out of localStorage', () => {
    writeToken('abc123', { persistent: false })

    expect(sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBe('abc123')
    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull()
  })

  it('removes the other store copy when the preference changes', () => {
    writeToken('persistent', { persistent: true })
    writeToken('scoped', { persistent: false })

    // Otherwise un-ticking the box would leave the long-lived copy behind and the checkbox
    // would be a lie.
    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull()
    expect(sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBe('scoped')
  })

  it('clears both stores when given nothing, which is how sign-out works', () => {
    writeToken('persistent', { persistent: true })
    writeToken(null)

    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull()
    expect(sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull()
    expect(readToken()).toBeNull()
  })
})

describe('readToken', () => {
  it('prefers localStorage over sessionStorage', () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, 'long-lived')
    sessionStorage.setItem(TOKEN_STORAGE_KEY, 'short-lived')

    expect(readToken()).toBe('long-lived')
  })

  it('falls back to sessionStorage so a reload of the same tab keeps the session', () => {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, 'short-lived')

    expect(readToken()).toBe('short-lived')
  })

  it('returns null when nothing is stored', () => {
    expect(readToken()).toBeNull()
  })
})