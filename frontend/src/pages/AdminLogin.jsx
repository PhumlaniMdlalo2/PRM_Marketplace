import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Shield } from 'lucide-react';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { useAuth } from '../auth/useAuth';

const ADMIN_EMAIL = 'admin.vendra@gmail.com';

export default function AdminLogin() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, signIn, signOut, user } = useAuth();
  const [email, setEmail] = useState(ADMIN_EMAIL);
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isAuthenticated && user?.role === 'ADMIN') navigate('/admin', { replace: true });
  }, [isAuthenticated, user?.role, navigate]);

  const submit = async (event) => {
    event.preventDefault();
    setError(null);
    if (!email.trim()) {
      setError('Admin email is required.');
      return;
    }
    if (!password) {
      setError('Password is required.');
      return;
    }

    setSubmitting(true);
    try {
      const account = await signIn(email.trim(), password);
      if (account?.role !== 'ADMIN') {
        signOut();
        setError('This account does not have administrator access.');
        return;
      }

      const destination = location.state?.from;
      navigate(
        typeof destination === 'string' && destination.startsWith('/admin')
          ? destination
          : '/admin',
        { replace: true },
      );
    } catch (caught) {
      setError(caught.message || 'Sign-in failed. Check your password and try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="min-h-dvh bg-lavender/40 px-5 py-10 sm:py-16">
      <section className="mx-auto max-w-md rounded-2xl border border-border bg-white p-7 shadow-sm sm:p-9">
        <Link to="/marketplace" className="text-sm font-semibold text-primary hover:underline">
          Back to marketplace
        </Link>
        <div className="mt-8">
          <div className="mb-5 flex h-12 w-12 items-center justify-center rounded-xl bg-lavender text-primary">
            <Shield size={22} aria-hidden="true" />
          </div>
          <h1 className="text-3xl font-bold tracking-tight text-text-primary">Admin sign in</h1>
          <p className="mt-2 text-sm text-text-secondary">
            Sign in with the provisioned marketplace administrator account.
          </p>
        </div>

        <form className="mt-7 space-y-4" onSubmit={submit} noValidate>
          {error && (
            <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}
          <Input
            label="Admin email"
            name="email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            autoComplete="username"
          />
          <Input
            label="Password"
            name="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            autoFocus
          />
          <Button type="submit" size="lg" disabled={submitting}>
            {submitting ? 'Signing in…' : 'Sign in to admin'}
          </Button>
        </form>
      </section>
    </main>
  );
}
