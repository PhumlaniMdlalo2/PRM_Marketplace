import { Heart, Star, ShieldCheck, MessageSquare } from 'lucide-react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useCallback, useState } from 'react';
import Button from '../components/ui/Button';
import Avatar from '../components/ui/Avatar';
import BackButton from '../components/ui/BackButton';
import ProductCard from '../components/ui/ProductCard';
import Layout from '../components/layout/Layout';
import { getById, listByCategory, listReviews } from '../api/products';
import { addCartItem } from '../api/cart';
import { startConversation } from '../api/messages';
import { formatCondition, formatLocation, formatPrice, toCardProps } from '../lib/format';
import { useAsync } from '../hooks/useAsync';
import { useFavourites } from '../hooks/useFavourites';
import { useAuth } from '../auth/useAuth';

const ProductDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();
  const [buying, setBuying] = useState(false);
  const [actionError, setActionError] = useState(null);

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

  // The heart here used to be local state, so it was empty on every visit and forgot itself on
  // refresh. It now reads the caller's saved set, which is also what the cards in "Similar items"
  // below need, so the two agree with each other and with the Saved items page.
  const favourites = useFavourites();

  const returnTo = `/product/${id}`;
  const requireSignIn = () => navigate('/login', { state: { from: { pathname: returnTo } } });

  const onFavouriteToggle = async () => {
    if (!isAuthenticated) {
      requireSignIn();
      return;
    }
    await favourites.toggle(id);
  };

  /**
   * "Buy now" adds the item to the cart and goes to the cart, which is where the actual checkout
   * decision happens. There is no one-click purchase anywhere in this API — an order is created from
   * a cart — so a button labelled Buy now that quietly did nothing was worse than one that says
   * where it takes you.
   */
  const onBuyNow = async () => {
    if (!isAuthenticated) {
      requireSignIn();
      return;
    }
    if (buying) return;
    setBuying(true);
    setActionError(null);
    try {
      await addCartItem(id, 1);
      navigate('/cart');
    } catch (caught) {
      // Out of stock is the common case here. A seller is stopped from reaching this button at all,
      // but the server does not refuse a seller adding their own listing, so the message is shown
      // rather than swallowed whichever reason came back.
      setActionError(caught.message);
    } finally {
      setBuying(false);
    }
  };

  /**
   * "Contact" opens a conversation with the seller, threading the listing through so the seller can
   * see what is being asked about.
   */
  const onContact = async () => {
    if (!isAuthenticated) {
      requireSignIn();
      return;
    }
    const sellerId = product?.vendor?.userId ?? product?.vendor?.id;
    if (!sellerId) {
      setActionError('This listing has no seller to contact.');
      return;
    }
    if (buying) return;
    setBuying(true);
    setActionError(null);
    try {
      const conversation = await startConversation(sellerId, id);
      navigate(`/messages/${conversation.id}`);
    } catch (caught) {
      setActionError(caught.message);
    } finally {
      setBuying(false);
    }
  };

  // Contacting yourself is a dead end: the conversation would be with you, and the message would sit
  // unread forever because the other side is the person reading it.
  const isOwnListing = isAuthenticated && product?.vendor?.userId === user?.id;

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
            onClick={onFavouriteToggle}
            disabled={favourites.busyProductId === id}
            className="p-2 rounded-full hover:bg-lavender transition-all duration-200 active:scale-90 disabled:opacity-60"
            aria-label={
              favourites.isFavourite(id) ? 'Remove from favourites' : 'Add to favourites'
            }
            aria-pressed={favourites.isFavourite(id)}
          >
            <Heart
              size={24}
              className={`transition-colors ${favourites.isFavourite(id) ? 'fill-error text-error' : 'text-text-secondary'}`}
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
                      <ProductCard
                        {...item}
                        isFavourite={favourites.isFavourite(item.id)}
                        onFavouriteToggle={() => favourites.toggle(item.id)}
                      />
                    </Link>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="fixed bottom-0 left-0 right-0 bg-white/95 backdrop-blur border-t border-border p-4 z-40 safe-area-inset-bottom">
          <div className="app-container flex gap-3">
            {(actionError || favourites.error) && (
              <p role="alert" className="mb-2 text-sm text-error">
                {actionError ?? favourites.error}
              </p>
            )}
            <Button
              variant="secondary"
              className="flex-1"
              onClick={onContact}
              disabled={buying || isOwnListing}
            >
              <MessageSquare size={18} />
              {isOwnListing ? 'Your listing' : 'Contact'}
            </Button>
            <Button
              className="flex-[1.4]"
              onClick={onBuyNow}
              disabled={buying || isOwnListing}
            >
              {buying ? 'Working…' : 'Buy now'}
            </Button>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default ProductDetails;