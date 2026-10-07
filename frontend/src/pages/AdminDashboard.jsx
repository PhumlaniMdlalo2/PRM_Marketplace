import { useCallback } from 'react';
import { Link } from 'react-router-dom';
import { ClipboardList, LogOut, Shield, Users } from 'lucide-react';
import Button from '../components/ui/Button';
import Layout from '../components/layout/Layout';
import { useAuth } from '../auth/useAuth';
import { useAsync } from '../hooks/useAsync';
import { listReportsForModeration } from '../api/reports';
import { listVendorProfiles } from '../api/vendorProfile';
import { listUsers } from '../api/users';

const AdminDashboard = () => {
  const { user, signOut } = useAuth();
  const loadOverview = useCallback(async () => {
    const [reports, sellers, users] = await Promise.all([
      listReportsForModeration(),
      listVendorProfiles(),
      listUsers(),
    ]);
    return {
      openReports: reports.filter((report) => report.status === 'OPEN').length,
      pendingSellers: sellers.filter((seller) => !seller.verified).length,
      userCount: users.length,
    };
  }, []);
  const { data, loading, error, run } = useAsync(loadOverview);

  const metrics = [
    { label: 'Open reports', value: data?.openReports, href: '/admin/moderation', icon: ClipboardList },
    { label: 'Pending seller reviews', value: data?.pendingSellers, href: '/admin/moderation', icon: Shield },
    { label: 'Marketplace accounts', value: data?.userCount, href: '/admin/users', icon: Users },
  ];

  return (
    <Layout showNav={false}>
      <main className="app-container mx-auto max-w-5xl px-4 py-8 sm:px-6">
        <header className="flex flex-wrap items-center justify-between gap-4 border-b border-border pb-6">
          <div>
            <p className="text-sm font-medium text-primary">Marketplace administration</p>
            <h1 className="mt-1 text-3xl font-bold tracking-tight text-text-primary">
              Welcome, {user?.name || 'Admin'}
            </h1>
          </div>
          <Button variant="secondary" onClick={signOut}>
            <LogOut size={17} aria-hidden="true" />
            Sign out
          </Button>
        </header>

        <section className="mt-8" aria-labelledby="overview-heading">
          <div className="flex items-end justify-between gap-4">
            <div>
              <h2 id="overview-heading" className="text-xl font-bold text-text-primary">Overview</h2>
              <p className="mt-1 text-sm text-text-secondary">Current marketplace work requiring admin access.</p>
            </div>
            <button
              type="button"
              onClick={() => run()}
              className="text-sm font-semibold text-primary hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
            >
              Refresh
            </button>
          </div>

          {error && (
            <div role="alert" className="mt-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
              Could not load the admin overview. {error.message}
            </div>
          )}

          <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {metrics.map(({ label, value, href, icon: Icon }) => (
              <Link
                key={label}
                to={href}
                className="rounded-2xl border border-border bg-white p-5 transition-colors hover:border-primary/40 hover:bg-lavender/30 focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
              >
                <Icon size={20} className="text-primary" aria-hidden="true" />
                <p className="mt-5 text-sm text-text-secondary">{label}</p>
                <p className="mt-1 text-3xl font-bold tabular-nums text-text-primary" aria-live="polite">
                  {loading ? '…' : value ?? '—'}
                </p>
                <span className="sr-only">Open {label.toLowerCase()}</span>
              </Link>
            ))}
          </div>
        </section>

        <section className="mt-10" aria-labelledby="admin-tools-heading">
          <h2 id="admin-tools-heading" className="text-xl font-bold text-text-primary">Admin tools</h2>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <Link
              to="/admin/moderation"
              className="rounded-2xl border border-border bg-white p-5 hover:border-primary/40 focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
            >
              <h3 className="font-semibold text-text-primary">Reports and seller applications</h3>
              <p className="mt-1 text-sm text-text-secondary">
                Review reports, record decisions, and approve or withdraw seller approval.
              </p>
            </Link>
            <Link
              to="/admin/users"
              className="rounded-2xl border border-border bg-white p-5 hover:border-primary/40 focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
            >
              <h3 className="font-semibold text-text-primary">Account management</h3>
              <p className="mt-1 text-sm text-text-secondary">
                Review marketplace accounts and remove accounts when necessary.
              </p>
            </Link>
          </div>
          <p className="mt-5 text-sm text-text-muted">
            Order status changes and payment settlement are not yet available in the admin interface.
          </p>
        </section>
      </main>
    </Layout>
  );
};

export default AdminDashboard;
