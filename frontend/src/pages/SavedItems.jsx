import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { HeartOff } from 'lucide-react';
import ProductCard from '../components/ui/ProductCard';
import BackButton from '../components/ui/BackButton';
import EmptyState from '../components/ui/EmptyState';
import Layout from '../components/layout/Layout';
import { ProductGridSkeleton } from '../components/ui/Skeleton';
import { listSavedItems, toggleSavedItem } from '../api/savedItems';
import { toCardProps } from '../lib/format';
import { useAsync } from '../hooks/useAsync';

const SavedItems = () => {
  const navigate = useNavigate();

  /**
   * The list endpoint already carries each product, so the page is one request. The saved-item count
   * is a separate endpoint, but counting an array that is already in memory buys nothing and would
   * be a second round trip that can disagree with the first.
   */
  const loadSavedItems = useCallback(async () => {
    const saved = await listSavedItems();

    return saved
      .filter((entry) => entry.product)
      .map((entry) => ({
        savedItemId: entry.id,
        productId: entry.product.id,
        savedAt: entry.savedAt,
        card: toCardProps(entry.product),
      }));
  }, []);

  const { data, loading, error, setData } = useAsync(loadSavedItems);
  const [busyProduct, setBusyProduct] = useState(null);
  const [actionError, setActionError] = useState(null);

  const saved = data ?? [];

  /**
   * Every card on this page is saved by definition, so the heart can only mean "remove". The row is
   * dropped from local state after the server confirms, rather than optimistically, so a failed
   * unsave leaves the item where it still is instead of quietly lying about the shortlist.
   */
  const onFavouriteToggle = async (productId) => {
    if (busyProduct === productId) return;

    setBusyProduct(productId);
    setActionError(null);
    try {
      const stillSaved = await toggleSavedItem(productId);
      setData((current) => (current ?? []).filter((entry) => (
        stillSaved || entry.productId !== productId
      )));
    } catch (caught) {
      setActionError(caught.message);
    } finally {
      setBusyProduct(null);
    }
  };

  return (
    <Layout>
      <div className="app-container py-6">
        <div className="flex items-center gap-4 mb-2">
          <BackButton />
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Saved items</h1>
        </div>
        <p className="text-text-secondary mb-6 ml-12">
          {loading ? 'Loading…' : `${saved.length} ${saved.length === 1 ? 'item' : 'items'} you've favourited`}
        </p>

        {error && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-2xl" role="alert">
            <p className="text-sm text-red-800">{error.message}</p>
          </div>
        )}
        {actionError && (
          <p className="mb-4 text-sm text-red-700" role="alert">{actionError}</p>
        )}

        {loading ? (
          <ProductGridSkeleton count={8} />
        ) : saved.length === 0 ? (
          <EmptyState
            icon={HeartOff}
            title="Nothing saved yet"
            description="Tap the heart on any item to save it here for later."
            action={
              <button
                onClick={() => navigate('/search')}
                className="px-4 py-2 bg-primary text-white rounded-full text-sm font-medium hover:bg-primary-hover active:scale-95 transition-all duration-200"
              >
                Browse listings
              </button>
            }
          />
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3.5">
            {saved.map((entry) => (
              <ProductCard
                key={entry.savedItemId}
                {...entry.card}
                isFavourite
                onFavouriteToggle={() => onFavouriteToggle(entry.productId)}
                onClick={() => navigate(`/product/${entry.productId}`)}
              />
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
};

export default SavedItems;