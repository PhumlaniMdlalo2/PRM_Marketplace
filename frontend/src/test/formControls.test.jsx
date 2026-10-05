import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import Input from '../components/ui/Input'

/**
 * The two shared form controls.
 *
 * The label association test is here because it was genuinely broken rather than untested: Input
 * rendered a bare <label> with no htmlFor and no id on the field, so no label in the app pointed at
 * its control. Clicking a label should focus its input, and a screen reader should announce the
 * field's name. Writing the test is what made the fix unavoidable.
 */
describe('Input', () => {
  it('associates its label with the field', () => {
    render(<Input label="Email address" name="email" />)

    // getByLabelText only finds a control through a real label association, so this assertion is
    // the accessibility guarantee rather than a proxy for it.
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument()
  })

  it('lets a label click focus the field', async () => {
    const user = userEvent.setup()
    render(<Input label="Email address" name="email" />)

    await user.click(screen.getByText(/email address/i))
    expect(screen.getByLabelText(/email address/i)).toHaveFocus()
  })

  it('honours an explicit id over the generated one', () => {
    render(<Input label="Email address" name="email" id="signup-email" />)

    expect(screen.getByLabelText(/email address/i)).toHaveAttribute('id', 'signup-email')
  })

  it('gives two instances distinct ids', () => {
    render(
      <>
        <Input label="First" name="first" />
        <Input label="Second" name="second" />
      </>,
    )

    const ids = screen.getAllByRole('textbox').map((field) => field.getAttribute('id'))
    // Two fields sharing an id would break every label that points at them.
    expect(ids[0]).not.toBe(ids[1])
  })

  it('shows the error and hides the hint when both are given', () => {
    render(<Input label="Email address" name="email" hint="We never share it" error="Enter a valid email address" />)

    expect(screen.getByText(/enter a valid email address/i)).toBeInTheDocument()
    // The hint is a different message about the same field; showing both at once is noise.
    expect(screen.queryByText(/we never share it/i)).not.toBeInTheDocument()
  })

  it('keeps the password value readable only on request', async () => {
    const user = userEvent.setup()
    render(<Input label="Password" name="password" type="password" defaultValue="hunter2hunter2" />)

    // Exact match: the reveal button's own label also contains the word.
    const field = screen.getByLabelText('Password')
    expect(field).toHaveAttribute('type', 'password')

    await user.click(screen.getByRole('button', { name: /show password/i }))
    expect(field).toHaveAttribute('type', 'text')

    // And back again, so the toggle is not a one-way trip.
    await user.click(screen.getByRole('button', { name: /hide password/i }))
    expect(field).toHaveAttribute('type', 'password')
  })
})