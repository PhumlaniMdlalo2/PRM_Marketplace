import { Heart, Star, ShieldCheck, MessageSquare } from 'lucide-react';
import { useParams, Link } from 'react-router-dom';
import { useCallback, useState } from 'react';
import Button from '../components/ui/Button';
import Avatar from '../components/ui/Avatar';
import BackButton from '../components/ui/BackButton';
import ProductCard from '../components/ui/ProductCard';
import Layout from '../components/layout/Layout';
import { getById, listByCategory, listReviews } from '../api/products';
import { formatCondition, formatLocation, formatPrice, toCardProps } from '../lib/format';
import { useAsync } from '../hooks/useAsync';

const ProductDetails = () => {
  const { id } = useParams();
  const [isFavourite, setIsFavourite] = useState(false);

  const loadProduct = useCallback(() => getById(id), [id]);
  const loadReviews = useCallback(() => listReviews(id).catch(() => []), [id]);

  const { data, loading, error } = useAsync(loadProduct);

  const product = data;

  // Reviews are a separate endpoint from the product, and there is no vendor review count on
  // VendorProfile, so the number under the rating is the count of reviews on this listing.
  const { data: reviews } = useAsync(loadReviews);
  const reviewCount = reviews?.length ?? 0;

  // /api/products/category/{category} takes no query parameters, so there is no server-side
  // paging here — the category comes back whole and the trimming happens below.
  // `category` is named separately rather than written as `product?.category` in the deps, so the
  // compiler sees the same value the callback closes over instead of inferring all of `product`.
  const category = product?.category;
  const loadSimilar = useCallback(
    () => (category ? listByCategory(category) : Promise.resolve(null)),
    [category],
  );

  const { data: similarData } = useAsync(loadSimilar);

  // Same category, minus the listing being viewed.
  const similarProducts = (similarData ?? [])
    .filter((item) => item.id !== product?.id)
    .slice(0, 5)
    .map(toCardProps);

  if (loading) {
    return (
      <Layout showNav={false}>
        <div className="app-container py-10">
          <div className="h-8 w-2/3 rounded-lg bg-lavender animate-pulse" />
          <div className="mt-4 h-10 w-1/3 rounded-lg bg-lavender animate-pulse" />
          <div className="mt-8 aspect-square max-w-md rounded-3xl bg-lavender animate-pulse" />
        </div>
      </Layout>
    );
  }

  if (error) {
    return (
      <Layout showNav={false}>
        <div className="app-container py-16 text-center">
          <p className="text-text-secondary">{error.message}</p>
          <Link
            to="/"
            className="mt-4 inline-block text-sm font-semibold text-primary hover:underline"
          >
            Back to browsing
          </Link>
        </div>
      </Layout>
    );
  }

  if (!product) return null;

  const rating = product.vendor?.ratingAvg;
  const hasRating = typeof rating === 'number' && rating > 0;

  return (
    <Layout showNav={false}>
      <div className="bg-white pb-32">
        <div className="app-container py-4 flex items-center justify-between">
          <BackButton />
          <button
            onClick={() => setIsFavourite(!isFavourite)}
            className="p-2 rounded-full hover:bg-lavender transition-all duration-200 active:scale-90"
            aria-label={isFavourite ? 'Remove from favourites' : 'Add to favourites'}
          >
            <Heart
              size={24}
              className={`transition-colors ${isFavourite ? 'fill-error text-error' : 'text-text-secondary'}`}
            />
          </button>
        </div>

        <div className="app-container">
          <div className="md:grid md:grid-cols-2 md:gap-10">
            <div className="aspect-square bg-lavender rounded-3xl overflow-hidden">
              {product.imageUrl ? (
                <img
                  src={product.imageUrl}
                  alt={product.name}
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="w-full h-full flex items-center justify-center text-text-muted">
                  <span className="text-sm font-medium">{product.name}</span>
                </div>
              )}
            </div>

            <div className="py-6 md:py-0">
              <div className="flex items-center gap-2 mb-2">
                {product.condition && (
                  <span className="text-xs font-semibold text-primary bg-primary-muted px-2.5 py-1 rounded-md">
                    {formatCondition(product.condition)}
                  </span>
                )}
                {product.vendor?.verified && (
                  <span className="text-xs text-text-secondary flex items-center gap-1">
                    <ShieldCheck size={14} className="text-success" /> Verified seller
                  </span>
                )}
              </div>

              <h1 className="text-2xl font-bold text-text-primary tracking-tight text-wrap-balance">
                {product.name}
              </h1>
              <p className="text-2xl font-bold text-primary mt-2 tracking-tight">
                {formatPrice(product.price)}
              </p>
              {formatLocation(product) && (
                <p className="text-sm text-text-secondary mt-1">{formatLocation(product)}</p>
              )}

              {product.description && (
                <div className="mt-7">
                  <h3 className="text-base font-semibold text-text-primary mb-2">Description</h3>
                  <p className="text-text-secondary leading-relaxed text-wrap-pretty max-w-[65ch]">
                    {product.description}
                  </p>
                </div>
              )}

              {product.vendor && (
                <div className="mt-7 flex items-center gap-4 p-4 rounded-2xl bg-lavender">
                  <Avatar size="lg" alt={product.vendor.businessName} />
                  <div>
                    <p className="font-semibold text-text-primary">
                      {product.vendor.businessName ?? 'Registered seller'}
                    </p>
                    {hasRating ? (
                      <div className="flex items-center gap-1.5 mt-1">
                        <Star size={14} className="fill-warning text-warning" />
                        <span className="text-sm font-medium text-text-primary">
                          {rating.toFixed(1)}
                        </span>
                        <span className="text-sm text-text-secondary">({reviewCount} reviews)</span>
                      </div>
                    ) : (
                      <p className="text-xs text-text-muted mt-1">No ratings yet</p>
                    )}
                    {product.vendor.createdAt && (
                      <p className="text-xs text-text-muted mt-0.5">
                        Member since {new Date(product.vendor.createdAt).getFullYear()}
                      </p>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>

          {similarProducts.length > 0 && (
            <div className="mt-8">
              <h3 className="text-base font-semibold text-text-primary mb-4">Similar items</h3>
              <div className="flex gap-3.5 overflow-x-auto pb-4 scrollbar-hide">
                {similarProducts.map((item) => (
                  <div key={item.id} className="flex-shrink-0 w-40">
                    <Link to={`/product/${item.id}`}>
                      <ProductCard {...item} />
                    </Link>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="fixed bottom-0 left-0 right-0 bg-white/95 backdrop-blur border-t border-border p-4 z-40 safe-area-inset-bottom">
          <div className="app-container flex gap-3">
            <Button variant="secondary" className="flex-1">
              <MessageSquare size={18} />
              Contact
            </Button>
            <Button className="flex-[1.4]">Buy now</Button>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default ProductDetails;