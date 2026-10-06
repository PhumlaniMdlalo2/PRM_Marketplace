import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { Search, Star, Store } from 'lucide-react';
import Layout from '../components/layout/Layout';
import EmptyState from '../components/ui/EmptyState';
import { ListRowSkeleton } from '../components/ui/Skeleton';
import { listVendorProfiles } from '../api/vendorProfile';
import { useAsync } from '../hooks/useAsync';

/**
 * The seller directory.
 *
 * `GET /api/vendor-profiles` has always been public, and nothing called it: a shop could only be
 * reached through one of its listings, so a seller with nothing listed at that moment had no page
 * a buyer could land on and no way to be found by name.
 *
 * The route takes no paging, so the whole directory arrives in one response and the filter below
 * is a client-side one. That is the right side for it to live on — the interesting query here is
 * "show me the shops with this word in the name", which a list this short answers without a round
 * trip per keystroke.
 */
const Vendors = () => {
  const loadVendors = useCallback(() => listVendorProfiles(), []);
  const { data: vendors, loading, error } = useAsync(loadVendors);
  const [query, setQuery] = useState('');

  const directory = vendors ?? [];
  const needle = query.trim().toLowerCase();
  const shown = needle
    ? directory.filter((vendor) => (vendor.businessName ?? '').toLowerCase().includes(needle))
    : directory;

  /** The two numbers the profile carries, as one line a buyer can read at a glance. */
  const ratingLine = (vendor) => {
    const count = vendor.ratingCount ?? 0;
    const average = vendor.ratingAvg;
    if (count < 1 || average == null) return 'No ratings yet';
    return `${Number(average).toFixed(1)} · ${count} ${count === 1 ? 'rating' : 'ratings'}`;
  };

  return (
    <Layout>
      <div className="app-container py-6 pb-32 max-w-3xl mx-auto">
        <div className="flex items-center justify-between gap-3 mb-1">
          <h1 className="text-xl font-bold text-text-primary tracking-tight">Sellers</h1>
          <span className="text-sm text-text-muted">
            {directory.length} {directory.length === 1 ? 'shop' : 'shops'}
          </span>
        </div>
        <p className="text-sm text-text-secondary mb-5">
          Every registered shop on the marketplace, newest and oldest alike.
        </p>

        <div className="relative mb-5">
          <Search
            size={16}
            className="absolute left-3.5 top-1/2 -translate-y-1/2 text-text-muted pointer-events-none"
          />
          <label htmlFor="vendor-search" className="sr-only">Search sellers</label>
          <input
            id="vendor-search"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by shop name"
            className="w-full pl-10 pr-4 py-2.5 bg-lavender rounded-full text-sm text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary/30 focus:bg-white transition-all"
          />
        </div>

        {error && (
          <div className="p-4 bg-red-50 border border-red-200 rounded-2xl mb-4" role="alert">
            <p className="text-sm text-red-800">
              The directory could not be loaded. Please try again.
            </p>
          </div>
        )}

        {loading && !directory.length && (
          <div className="bg-white border border-border rounded-2xl divide-y divide-border overflow-hidden">
            <ListRowSkeleton />
            <ListRowSkeleton />
            <ListRowSkeleton />
          </div>
        )}

        {!loading && !error && directory.length === 0 && (
          <EmptyState
            icon={Store}
            title="No sellers yet"
            description="Once a shop is registered it will appear here."
          />
        )}

        {directory.length > 0 && shown.length === 0 && (
          <EmptyState
            icon={Search}
            title="No shop by that name"
            description="Try a shorter word, or clear the search to see every shop."
          />
        )}

        {shown.length > 0 && (
          <div className="bg-white border border-border rounded-2xl divide-y divide-border overflow-hidden">
            {shown.map((vendor) => (
              <Link
                key={vendor.id}
                to={`/vendors/${vendor.id}`}
                className="flex items-start gap-3 p-4 hover:bg-lavender/50 transition-colors"
              >
                <span className="w-11 h-11 rounded-xl bg-lavender flex items-center justify-center shrink-0">
                  <Store size={20} className="text-primary" />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="flex items-center gap-2 flex-wrap">
                    <span className="font-semibold text-text-primary truncate">
                      {vendor.businessName || 'Unnamed shop'}
                    </span>
                    {vendor.verified && (
                      <span className="text-[11px] font-medium text-primary bg-primary-muted px-1.5 py-0.5 rounded-md">
                        Verified
                      </span>
                    )}
                  </span>
                  <span className="mt-1 flex items-center gap-1.5 text-xs text-text-secondary">
                    <Star size={13} className="fill-primary text-primary shrink-0" />
                    {ratingLine(vendor)}
                  </span>
                  {vendor.createdAt && (
                    <span className="mt-1 block text-xs text-text-muted">
                      Member since {new Date(vendor.createdAt).getFullYear()}
                    </span>
                  )}
                </span>
              </Link>
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
};

export default Vendors;
