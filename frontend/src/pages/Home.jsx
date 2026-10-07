import { forwardRef, useCallback, useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Sparkles, Store } from 'lucide-react';
import { useAuth } from '../auth/useAuth';
import SearchBar from '../components/ui/SearchBar';
import CategoryChip from '../components/ui/CategoryChip';
import ProductCard from '../components/ui/ProductCard';
import { ProductGridSkeleton } from '../components/ui/Skeleton';
import Layout from '../components/layout/Layout';
import { search, searchAcrossCategories } from '../api/products';
import { getMyVendorProfile } from '../api/vendorProfile';
import { toCardProps } from '../lib/format';
import { useAsync } from '../hooks/useAsync';
import { useFavourites } from '../hooks/useFavourites';
import { GOODS_CATEGORIES, SERVICE_CATEGORIES } from '../lib/marketplaceCategories';
import SellerHome from './SellerHome';

/**
 * Renders a grid of product cards with working hearts.
 *
 * `favourites` is passed in rather than fetched here so one hook instance serves the whole page
 * instead of every card asking for the saved set on its own.
 */
const FavouritedGrid = ({ products, favourites, onNavigate }) => (
  <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3.5">
    {products.map((product) => (
      <ProductCard
        key={product.id}
        {...product}
        isFavourite={favourites.isFavourite(product.id)}
        onFavouriteToggle={() => favourites.toggle(product.id)}
        onClick={() => onNavigate(product.id)}
      />
    ))}
  </div>
);

const Home = () => {
  const { user } = useAuth();
  const isSellerEligible = user?.role === 'VENDOR' || user?.role === 'STUDENT';
  const loadSellerProfile = useCallback(() => getMyVendorProfile(), []);
  const {
    data: sellerProfile,
    loading: sellerProfileLoading,
    error: sellerProfileError,
    run: reloadSellerProfile,
  } = useAsync(loadSellerProfile, { immediate: isSellerEligible });

  if (user?.role === 'VENDOR') {
    return (
      <SellerHome
        user={user}
        profile={sellerProfile}
        profileLoading={sellerProfileLoading}
        profileError={sellerProfileError}
        onRetry={reloadSellerProfile}
      />
    );
  }

  if (user?.role === 'STUDENT' && sellerProfileLoading) {
    return (
      <Layout>
        <div className="app-container py-10">
          <div className="h-36 max-w-3xl animate-pulse rounded-3xl bg-lavender" role="status">
            <span className="sr-only">Checking seller workspace</span>
          </div>
        </div>
      </Layout>
    );
  }

  if (user?.role === 'STUDENT' && sellerProfile) {
    return <SellerHome user={user} profile={sellerProfile} />;
  }

  return (
    <BuyerHome
      sellerProfileError={
        user?.role === 'STUDENT' && sellerProfileError?.status !== 404 ? sellerProfileError : null
      }
    />
  );
};

const BuyerHome = ({ sellerProfileError }) => {
  const { hash } = useLocation();
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [section, setSection] = useState('goods');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const forYouRef = useRef(null);
  const categories = section === 'services' ? SERVICE_CATEGORIES : GOODS_CATEGORIES;

  const loadFeatured = useCallback(
    () => {
      const criteria = {
        keyword: searchQuery || undefined,
        category: selectedCategory === 'All' ? undefined : selectedCategory,
        activeOnly: true,
        size: 12,
      };
      return selectedCategory === 'All'
        ? searchAcrossCategories(categories.map(({ value }) => value), criteria)
        : search(criteria);
    },
    [categories, searchQuery, selectedCategory],
  );

  const { data, loading, error } = useAsync(loadFeatured);

  const products = (data?.content ?? []).map(toCardProps);
  const favourites = useFavourites();

  useEffect(() => {
    if (hash === '#for-you' && forYouRef.current) {
      forYouRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [hash]);

  const openProduct = (id) => navigate(`/product/${id}`);

  return (
    <Layout>
      <div className="app-container py-6">
        {sellerProfileError && (
          <p role="alert" className="mb-4 text-sm text-error">
            We could not check your seller profile. The buyer catalogue is available, but seller status may be out of date.
          </p>
        )}
        <section className="marketplace-intro">
          <div>
            <p className="marketplace-kicker">Your campus marketplace</p>
            <h1>Find what campus needs.</h1>
            <p>Browse goods, find services and catch up with your campus community.</p>
          </div>
          <SearchBar
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="marketplace-search"
          />
        </section>

        <div className="mb-5 flex items-center justify-between gap-3">
          <div className="flex gap-1 rounded-xl bg-lavender p-1" role="tablist" aria-label="Marketplace sections">
            {[
              { id: 'goods', label: 'Goods' },
              { id: 'services', label: 'Services' },
            ].map((item) => (
              <button
                key={item.id}
                type="button"
                role="tab"
                aria-selected={section === item.id}
                aria-controls="marketplace-category-panel"
                onClick={() => {
                  setSection(item.id);
                  setSelectedCategory('All');
                }}
                className={`min-h-10 rounded-lg px-4 text-sm font-semibold transition-colors ${
                  section === item.id
                    ? 'bg-white text-primary shadow-sm'
                    : 'text-text-secondary hover:text-text-primary'
                }`}
              >
                {item.label}
              </button>
            ))}
          </div>
          <Link
            to="/bulletin"
            className="shrink-0 px-1 py-2 text-sm font-semibold text-primary transition-colors hover:text-primary-hover"
          >
            Community
          </Link>
        </div>

        <div
          className="marketplace-categories flex gap-2 overflow-x-auto pb-0 scrollbar-hide"
          id="marketplace-category-panel"
          role="tabpanel"
          aria-label={section === 'goods' ? 'Goods categories' : 'Services categories'}
        >
          <CategoryChip
            label="All"
            isSelected={selectedCategory === 'All'}
            onClick={() => setSelectedCategory('All')}
          />
          {categories.map((category) => (
            <CategoryChip
              key={category.value}
              label={category.label}
              isSelected={selectedCategory === category.value}
              onClick={() => setSelectedCategory(category.value)}
            />
          ))}
        </div>

        {/* Not a category, so it sits outside the tablist rather than as a chip that would read as
            one of the product filters. */}
        <Link
          to="/vendors"
          className="mb-3 inline-flex items-center gap-2 px-1 py-2 text-sm font-semibold text-primary transition-colors hover:text-primary-hover"
        >
          <Store size={16} className="text-primary" />
          Browse the seller directory
        </Link>

        {favourites.error && (
          <p role="alert" className="mb-4 text-sm text-error">
            {favourites.error}
          </p>
        )}

        <div className="mt-7">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-2xl font-semibold tracking-tight text-text-primary">
              {section === 'goods' ? 'Featured goods' : 'Featured services'}
            </h2>
            <span className="text-xs font-medium text-text-muted">
              {data ? `${data.totalElements} results` : ''}
            </span>
          </div>
          {error ? (
            <div
              role="alert"
              className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              {error.message}
            </div>
          ) : loading ? (
            <ProductGridSkeleton count={6} />
          ) : products.length === 0 ? (
            <div className="py-12 text-center">
              <p className="text-text-secondary">
                No {section === 'goods' ? 'goods' : 'services'} match your search yet.
              </p>
            </div>
          ) : (
            <FavouritedGrid products={products} favourites={favourites} onNavigate={openProduct} />
          )}
        </div>

        <ForYouSection key={section} ref={forYouRef} section={section} />
      </div>
    </Layout>
  );
};

export default Home;

/**
 * A second pass over the same catalogue endpoint, newest first.
 *
 * There is no recommendation service behind this. It is a plain listing so the section has real
 * data in it; the copy describes an intention, not behaviour, and should either be reworded or the
 * ranking actually built.
 */
const ForYouSection = forwardRef(function ForYouSection({ section }, ref) {
  const navigate = useNavigate();
  const [fyCategory, setFyCategory] = useState('All');
  const categories = section === 'services' ? SERVICE_CATEGORIES : GOODS_CATEGORIES;

  const loadForYou = useCallback(
    () => {
      const criteria = {
        category: fyCategory === 'All' ? undefined : fyCategory,
        activeOnly: true,
        size: 8,
        sortBy: 'createdAt',
        direction: 'desc',
      };
      return fyCategory === 'All'
        ? searchAcrossCategories(categories.map(({ value }) => value), criteria)
        : search(criteria);
    },
    [categories, fyCategory],
  );

  const { data, loading } = useAsync(loadForYou);

  const items = (data?.content ?? []).map(toCardProps);
  // A second hook instance, deliberately. Sharing one with the section above would mean the two
  // sections hold two independent copies of the same saved set, so saving in one would leave the
  // other stale until it refetched — one instance each is simpler to reason about than one shared
  // copy kept in sync across two lists that are fetched separately anyway.
  const favourites = useFavourites();

  return (
    <section ref={ref} id="for-you" className="mt-12 border-t border-border pt-8 scroll-mt-20">
      <div className="flex items-center gap-3 mb-5 max-w-2xl">
        <div className="w-11 h-11 rounded-2xl bg-primary/10 flex items-center justify-center">
          <Sparkles size={22} className="text-primary" />
        </div>
        <div className="flex-1">
          <h2 className="text-xl font-bold text-text-primary tracking-tight leading-tight">Just listed</h2>
            <p className="text-sm text-text-secondary">Fresh finds from the Vendra community</p>
        </div>
      </div>

      <div className="flex gap-2 overflow-x-auto pb-4 scrollbar-hide" role="tablist" aria-label="For you categories">
        <CategoryChip
          label="All"
          isSelected={fyCategory === 'All'}
          onClick={() => setFyCategory('All')}
        />
        {categories.map((category) => (
          <CategoryChip
            key={category.value}
            label={category.label}
            isSelected={fyCategory === category.value}
            onClick={() => setFyCategory(category.value)}
          />
        ))}
      </div>

      <div className="mt-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-text-primary">
            {fyCategory === 'All'
              ? section === 'goods' ? 'New goods around campus' : 'New services around campus'
              : `New in ${categories.find((category) => category.value === fyCategory)?.label ?? fyCategory}`}
          </h3>
          <span className="text-xs font-medium text-text-muted">
            {data ? `${data.totalElements} items` : ''}
          </span>
        </div>
        {loading ? (
          <ProductGridSkeleton count={8} />
        ) : items.length === 0 ? (
          <div className="py-12 text-center">
            <p className="text-text-secondary">Nothing listed here yet</p>
          </div>
        ) : (
          <FavouritedGrid
            products={items}
            favourites={favourites}
            onNavigate={(id) => navigate(`/product/${id}`)}
          />
        )}
      </div>
    </section>
  );
});

ForYouSection.displayName = 'ForYouSection';