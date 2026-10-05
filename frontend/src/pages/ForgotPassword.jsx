import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, Mail } from 'lucide-react';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { forgotPassword } from '../api/auth';

/**
 * Password reset, step one: ask for a reset token.
 *
 * The backend answers `/auth/forgot-password` with the same message whether or not the account
 * exists. That is deliberate — a different answer for a known address would turn this form into a
 * way to find out who has an account here. So the confirmation below is worded to match, and no
 * branching on 404 happens anywhere on this page.
 */
const ForgotPassword = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setError(null);

    const trimmed = email.trim();
    if (!trimmed) return setError('Email is required');
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
      return setError('Enter a valid email address');
    }

    setSubmitting(true);
    try {
      await forgotPassword(trimmed);
      setSent(true);
    } catch (caught) {
      // A 400 means the server rejected the address as malformed. Anything else is reported as-is,
      // but note the server swallows mail failures (EmailServiceImpl.send), so a 200 here does not
      // guarantee a message was actually delivered.
      setError(caught.status === 400
        ? 'That does not look like an email address.'
        : caught.message);
    } finally {
      setSubmitting(false);
    }
  };

  if (sent) {
    return (
      <div className="min-h-dvh bg-white px-6 pt-12 pb-8">
        <div className="w-full max-w-md mx-auto">
          <h1 className="text-3xl font-bold text-text-primary tracking-tight text-wrap-balance">
            Check your email
          </h1>
          <p className="mt-2 text-text-secondary text-wrap-pretty">
            If {email.trim()} has an account, a reset token is on its way. It expires in 60 minutes.
          </p>
          <p className="mt-4 text-sm text-text-secondary text-wrap-pretty">
            The email contains a token rather than a link, because the server does not know the
            frontend's address. Paste it on the next screen.
          </p>
          <div className="mt-8 space-y-3">
            <Button size="lg" onClick={() => navigate('/reset-password', { state: { email: email.trim() } })}>
              I have a token
            </Button>
            <Link
              to="/login"
              className="block text-center text-sm text-primary font-semibold hover:underline"
            >
              Back to sign in
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-dvh bg-white px-6 pt-12 pb-8">
      <div className="w-full max-w-md mx-auto">
        <Link
          to="/login"
          className="inline-flex items-center gap-1.5 text-sm text-text-secondary hover:text-text-primary"
        >
          <ArrowLeft size={16} aria-hidden="true" />
          Back to sign in
        </Link>

        <div className="mt-6">
          <h1 className="text-3xl font-bold text-text-primary tracking-tight text-wrap-balance">
            Reset your password
          </h1>
          <p className="mt-2 text-text-secondary text-wrap-pretty">
            Enter the address on your account and we will send a reset token to it.
          </p>
        </div>

        <form onSubmit={submit} className="mt-8 space-y-4" noValidate>
          {error && (
            <div
              role="alert"
              className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              {error}
            </div>
          )}

          <Input
            label="Email"
            type="email"
            name="email"
            placeholder="you@university.ac.za"
            icon={Mail}
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            autoComplete="email"
          />

          <div className="pt-2">
            <Button type="submit" size="lg" disabled={submitting}>
              {submitting ? 'Sending…' : 'Send reset token'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ForgotPassword;