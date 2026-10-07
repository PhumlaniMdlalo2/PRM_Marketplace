import { Link, Navigate, useLocation } from 'react-router-dom';
import { Shield } from 'lucide-react';
import Button from '../components/ui/Button';
import { useAuth } from './useAuth';

export default function AdminRoute({ children }) {
  const { isAuthenticated, signOut, user } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/admin/login" replace state={{ from: location.pathname }} />;
  }

  if (user?.role !== 'ADMIN') {
    return (
      <main className="min-h-dvh bg-lavender/40 px-5 py-16">
        <section className="mx-auto max-w-md rounded-2xl border border-border bg-white p-8 text-center">
          <Shield size={30} className="mx-auto mb-4 text-primary" aria-hidden="true" />
          <h1 className="text-2xl font-bold text-text-primary">Admin access only</h1>
          <p className="mt-2 text-sm text-text-secondary">
            This sign-in does not have administrator access.
          </p>
          <div className="mt-6 flex flex-col gap-3">
            <Button onClick={signOut}>Sign out</Button>
            <Link to="/marketplace" className="text-sm font-semibold text-primary hover:underline">
              Return to the marketplace
            </Link>
          </div>
        </section>
      </main>
    );
  }

  return children;
}
