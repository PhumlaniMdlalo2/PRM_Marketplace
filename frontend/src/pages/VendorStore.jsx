import { useCallback } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Star, Store } from 'lucide-react';
import Layout from '../components/layout/Layout';
import BackButton from '../components/ui/BackButton';
import EmptyState from '../components/ui/EmptyState';
import ProductCard from '../components/ui/ProductCard';
import { ProductGridSkeleton } from '../components/ui/Skeleton';
import { getVendorProfile } from '../api/vendorProfile';
import { listByVendor } from '../api/products';
import { toCardProps } from '../lib/format';
import { useAsync } from '../hooks/useAsync';
import { useFavourites } from '../hooks/useFavourites';
import { useAuth } from '../auth/useAuth';

/**
 * One seller's shop: who they are, and everything they have listed right now.
 *
 * The profile and the listings arrive together because neither page is worth rendering on its own —
 * a shop with its name but no shelves is as half a page as shelves with no one to attribute them to.
 * Listings come back active-only from the server, so a retired item never shows up here to be
 * tapped into a page that would say it is gone.
 */
const VendorStore = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const favourites = useFavourites();

  const loadStore = useCallback(async () => {
    const [vendor, products] = await Promise.all([getVendorProfile(id), listByVendor(id)]);
    return { vendor, products: Array.isArray(products) ? products : [] };
  }, [id]);

  const { data, loading, error } = useAsync(loadStore);

  const onFavouriteToggle = async (productId) => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/vendors/${id}` } });
      return;
    }
    await favourites.toggle(productId);
  };

  const vendor = data?.vendor ?? null;
  const products = data?.products ?? [];

  const ratingCount = vendor?.ratingCount ?? 0;
  const ratingAverage = vendor?.ratingAvg;
  const ratingLine = ratingCount > 0 && ratingAverage != null
    ? `${Number(ratingAverage).toFixed(1)} · ${ratingCount} ${ratingCount === 1 ? 'rating' : 'ratings'}`
    : 'No ratings yet';

  return (
    <Layout>
      <div className="app-container py-6 pb-32 max-w-5xl mx-auto">
        <div className="flex items-center gap-4 mb-6">
          <BackButton />
          <h1 className="text-xl font-bold text-text-primary tracking-tight">Shop</h1>
        </div>

        {error && !vendor && (
          <div className="p-4 bg-red-50 border border-red-200 rounded-2xl mb-6" role="alert">
            <p className="text-sm text-red-800">
              {error.status === 404
                ? 'This shop is not on the directory.'
                : 'The shop could not be loaded. Please try again.'}
            </p>
            <Link to="/vendors" className="mt-2 inline-block text-sm font-medium text-primary">
              Back to all sellers
            </Link>
          </div>
        )}

        {vendor && (
          <div className="bg-white border border-border rounded-2xl p-4 mb-6 flex items-start gap-3">
            <span className="w-12 h-12 rounded-xl bg-lavender flex items-center justify-center shrink-0">
              <Store size={22} className="text-primary" />
            </span>
            <div className="min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <h2 className="font-bold text-text-primary text-lg truncate">
                  {vendor.businessName || 'Unnamed shop'}
                </h2>
                {vendor.verified && (
                  <span className="text-[11px] font-medium text-primary bg-primary-muted px-1.5 py-0.5 rounded-md">
                    Verified
                  </span>
                )}
              </div>
              <p className="mt-1 flex items-center gap-1.5 text-sm text-text-secondary">
                <Star size={14} className="fill-primary text-primary shrink-0" />
                {ratingLine}
              </p>
              {vendor.createdAt && (
                <p className="mt-1 text-xs text-text-muted">
                  Member since {new Date(vendor.createdAt).getFullYear()}
                </p>
              )}
            </div>
          </div>
        )}

        {loading && !vendor && (
          <ProductGridSkeleton count={8} />
        )}

        {vendor && products.length === 0 && !loading && (
          <EmptyState
            icon={Store}
            title="Nothing listed right now"
            description="This shop has no active listings at the moment."
          />
        )}

        {products.length > 0 && (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3.5">
            {products.map((product) => (
              <ProductCard
                key={product.id}
                {...toCardProps(product)}
                isFavourite={favourites.isFavourite(product.id)}
                onFavouriteToggle={() => onFavouriteToggle(product.id)}
                onClick={() => navigate(`/product/${product.id}`)}
              />
            ))}
          </div>
        )}

        {favourites.error && (
          <p role="alert" className="mt-4 text-sm text-error">{favourites.error}</p>
        )}
      </div>
    </Layout>
  );
};

export default VendorStore;
