import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Trash2, Users } from 'lucide-react';
import Button from '../components/ui/Button';
import Layout from '../components/layout/Layout';
import EmptyState from '../components/ui/EmptyState';
import { useAuth } from '../auth/useAuth';
import { useAsync } from '../hooks/useAsync';
import { deleteUser, listUsers } from '../api/users';

const AdminUsers = () => {
  const { user: admin } = useAuth();
  const load = useCallback(() => listUsers(), []);
  const { data: users, loading, error, setData, run } = useAsync(load);
  const [actionError, setActionError] = useState(null);
  const [deletingId, setDeletingId] = useState(null);

  const remove = async (account) => {
    if (account.id === admin?.id || deletingId) return;
    if (!window.confirm(`Delete the account for ${account.email}? This cannot be undone.`)) return;

    setDeletingId(account.id);
    setActionError(null);
    try {
      await deleteUser(account.id);
      setData((current) => (Array.isArray(current)
        ? current.filter((entry) => entry.id !== account.id)
        : current));
    } catch (caught) {
      setActionError(caught.message || 'The account could not be deleted.');
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <Layout showNav={false}>
      <main className="app-container mx-auto max-w-5xl px-4 py-8 sm:px-6">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-border pb-6">
          <div>
            <Link to="/admin" className="inline-flex items-center gap-1 text-sm font-semibold text-primary hover:underline">
              <ArrowLeft size={16} aria-hidden="true" />
              Admin overview
            </Link>
            <h1 className="mt-3 text-3xl font-bold tracking-tight text-text-primary">Marketplace accounts</h1>
            <p className="mt-1 text-sm text-text-secondary">View registered accounts and remove accounts when required.</p>
          </div>
          <button
            type="button"
            onClick={() => run()}
            className="text-sm font-semibold text-primary hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
          >
            Refresh list
          </button>
        </div>

        {error && (
          <div role="alert" className="mt-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            Could not load accounts. {error.message}
          </div>
        )}
        {actionError && (
          <div role="alert" className="mt-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {actionError}
          </div>
        )}

        {loading && !users && (
          <div className="mt-6 space-y-3" role="status" aria-label="Loading accounts">
            <div className="h-20 animate-pulse rounded-xl bg-lavender" />
            <div className="h-20 animate-pulse rounded-xl bg-lavender" />
          </div>
        )}
        {!loading && !error && users?.length === 0 && (
          <div className="mt-8">
            <EmptyState icon={Users} title="No accounts found" description="Registered marketplace accounts will appear here." />
          </div>
        )}
        {users?.length > 0 && (
          <ul className="mt-6 divide-y divide-border rounded-2xl border border-border bg-white">
            {users.map((account) => (
              <li key={account.id} className="flex flex-wrap items-center justify-between gap-4 p-4 sm:px-5">
                <div className="min-w-0">
                  <h2 className="truncate font-semibold text-text-primary">{account.name}</h2>
                  <p className="truncate text-sm text-text-secondary">{account.email}</p>
                  <p className="mt-1 text-xs text-text-muted">
                    {account.role}
                    {account.verified ? ' · Verified' : ' · Unverified'}
                  </p>
                </div>
                {account.id === admin?.id ? (
                  <span className="text-xs font-medium text-text-muted">Current admin account</span>
                ) : (
                  <Button
                    variant="secondary"
                    size="sm"
                    disabled={Boolean(deletingId)}
                    onClick={() => remove(account)}
                  >
                    <Trash2 size={15} aria-hidden="true" />
                    {deletingId === account.id ? 'Deleting…' : 'Delete account'}
                  </Button>
                )}
              </li>
            ))}
          </ul>
        )}
      </main>
    </Layout>
  );
};

export default AdminUsers;
