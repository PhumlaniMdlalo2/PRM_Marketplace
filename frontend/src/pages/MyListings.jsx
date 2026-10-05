import { useCallback, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, PackageOpen, Pencil, Plus } from 'lucide-react';
import Layout from '../components/layout/Layout';
import Button from '../components/ui/Button';
import EmptyState from '../components/ui/EmptyState';
import { listMine, reactivate, retire } from '../api/products';
import { useAsync } from '../hooks/useAsync';
import { formatPrice } from '../lib/format';

/**
 * The seller's own listings.
 *
 * This backs `GET /products/mine`, which was implemented and never called from anywhere — a seller
 * had no way to see what they had put up, so editing or retiring a listing meant having kept the id
 * from the create confirmation. It is also the only place `active` is visible, because the public
 * catalogue hides retired listings entirely.
 *
 * Retiring is not a delete. The endpoint takes a listing off sale while keeping its rows, so an
 * order that already references it still resolves; the two are separate calls rather than an update
 * body because `Product.active` is a primitive boolean and an update that omits it arrives as false.
 */
const MyListings = () => {
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const [busyId, setBusyId] = useState(null);

  const load = useCallback(() => listMine(), []);
  const { data, loading, run } = useAsync(load);

  const listings = data ?? [];

  const toggleActive = async (listing) => {
    setError(null);
    setBusyId(listing.id);
    try {
      if (listing.active) await retire(listing.id);
      else await reactivate(listing.id);
      // Refetched rather than patched locally, so a listing the server refused to change cannot
      // appear changed here.
      await run();
    } catch (caught) {
      setError(caught.message);
    } finally {
      setBusyId(null);
    }
  };

  return (
    <Layout>
      <div className="app-container py-6 max-w-2xl mx-auto pb-24">
        <h1 className="text-2xl font-bold text-text-primary tracking-tight">Your listings</h1>
        <p className="text-text-secondary mt-1">
          {loading ? 'Loading…' : `${listings.length} ${listings.length === 1 ? 'listing' : 'listings'}`}
        </p>

        {error && (
          <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-2xl" role="alert">
            <p className="text-sm text-red-800">{error}</p>
          </div>
        )}

        <div className="mt-5 space-y-3">
          {!loading && listings.length === 0 ? (
            <EmptyState
              icon={PackageOpen}
              title="No listings yet"
              description="Anything you put up for sale appears here so you can edit or take it down."
            />
          ) : (
            listings.map((listing) => (
              <article
                key={listing.id}
                className={`bg-white border border-border rounded-2xl p-4 ${
                  listing.active ? '' : 'opacity-70'
                }`}
              >
                <div className="flex items-start gap-4">
                  <span className="w-14 h-14 bg-lavender rounded-xl flex-shrink-0 overflow-hidden">
                    {listing.imageUrl && (
                      <img src={listing.imageUrl} alt="" className="w-full h-full object-cover" />
                    )}
                  </span>
                  <div className="flex-1 min-w-0">
                    <Link
                      to={`/product/${listing.id}`}
                      className="block font-semibold text-text-primary truncate hover:underline"
                    >
                      {listing.name}
                    </Link>
                    <p className="text-sm text-text-secondary mt-0.5">
                      {formatPrice(listing.price)}
                      {listing.stockQuantity > 0
                        ? ` · ${listing.stockQuantity} in stock`
                        : ' · out of stock'}
                    </p>
                    <p className="text-xs text-text-muted mt-0.5">
                      {listing.active ? 'On sale' : 'Retired — not visible to buyers'}
                    </p>
                  </div>
                </div>

                <div className="mt-3 flex flex-wrap gap-2">
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => navigate(`/listing/edit/${listing.id}`)}
                  >
                    <Pencil size={14} />
                    Edit
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    disabled={busyId === listing.id}
                    onClick={() => toggleActive(listing)}
                  >
                    {listing.active ? <EyeOff size={14} /> : <Eye size={14} />}
                    {busyId === listing.id
                      ? 'Working…'
                      : listing.active
                        ? 'Take down'
                        : 'Put back on sale'}
                  </Button>
                </div>
              </article>
            ))
          )}
        </div>

        <Link
          to="/listing/create"
          className="mt-6 flex items-center justify-center gap-2 py-3.5 bg-primary text-white font-semibold rounded-2xl shadow-md shadow-primary/20 hover:bg-primary-hover active:scale-[0.98] transition-all duration-200"
        >
          <Plus size={20} />
          Sell an item
        </Link>
      </div>
    </Layout>
  );
};

export default MyListings;