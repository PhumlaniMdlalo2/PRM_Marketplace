import { useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft, KeyRound } from 'lucide-react';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { resetPassword } from '../api/auth';

/**
 * Password reset, step two: trade the token for a new password.
 *
 * The token is read from `?token=` first and from router state second, so both of these work:
 * opening a link by hand, and continuing from the ForgotPassword page which passes the address
 * along. It is also editable, because the emailed token arrives as text and a user who has already
 * pasted it somewhere does not need it prefilled correctly.
 *
 * On success the user is sent to sign in rather than straight into the app. The token is consumed
 * by the reset, and no session is issued by `/auth/reset-password`, so there is nothing to be
 * signed in with — going to the home page would bounce them back to login anyway, just less
 * clearly.
 */
const ResetPassword = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();

  const [token, setToken] = useState(
    () => searchParams.get('token') ?? location.state?.token ?? '',
  );
  const [form, setForm] = useState({ password: '', confirmPassword: '' });
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const set = (name) => (event) => setForm((f) => ({ ...f, [name]: event.target.value }));

  const submit = async (event) => {
    event.preventDefault();
    setError(null);

    const trimmed = token.trim();
    if (!trimmed) return setError('Paste the token from your email');
    if (!form.password) return setError('Enter a new password');
    // The server enforces 8 characters; checking it here means the user finds out before a round
    // trip rather than from a 400.
    if (form.password.length < 8) return setError('Password must be at least 8 characters');
    if (form.password !== form.confirmPassword) {
      return setError('The two passwords do not match');
    }

    setSubmitting(true);
    try {
      await resetPassword(trimmed, form.password);
      navigate('/login', { replace: true, state: { reset: true } });
    } catch (caught) {
      setError(caught.fieldErrors?.newPassword ?? caught.message);
    } finally {
      setSubmitting(false);
    }
  };

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
            Choose a new password
          </h1>
          <p className="mt-2 text-text-secondary text-wrap-pretty">
            Paste the token from your email, then set a new password. The token works once and expires
            in 60 minutes.
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
            label="Reset token"
            name="token"
            placeholder="Paste the token from the email"
            icon={KeyRound}
            value={token}
            onChange={(event) => setToken(event.target.value)}
          />
          <Input
            label="New password"
            type="password"
            name="password"
            placeholder="At least 8 characters"
            value={form.password}
            onChange={set('password')}
            autoComplete="new-password"
          />
          <Input
            label="Confirm new password"
            type="password"
            name="confirmPassword"
            value={form.confirmPassword}
            onChange={set('confirmPassword')}
            autoComplete="new-password"
          />

          <div className="pt-2">
            <Button type="submit" size="lg" disabled={submitting}>
              {submitting ? 'Updating…' : 'Update password'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ResetPassword;