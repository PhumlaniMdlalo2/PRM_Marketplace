import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Sparkles } from 'lucide-react';
import SearchBar from '../components/ui/SearchBar';
import CategoryChip from '../components/ui/CategoryChip';
import ProductCard from '../components/ui/ProductCard';
import { ProductGridSkeleton } from '../components/ui/Skeleton';
import Layout from '../components/layout/Layout';
import { search } from '../api/products';
import { toCardProps } from '../lib/format';
import { useAsync } from '../hooks/useAsync';
import { useFavourites } from '../hooks/useFavourites';

// The categories the backend actually stores. These are the values the Product.category column is
// compared against, so they are not free-text labels chosen for the UI.
const categories = ['Furniture', 'Electronics', 'Fashion', 'Bikes'];

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
  const { hash } = useLocation();
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const forYouRef = useRef(null);

  const loadFeatured = useCallback(
    () =>
      search({
        keyword: searchQuery || undefined,
        category: selectedCategory === 'All' ? undefined : selectedCategory,
        activeOnly: true,
        size: 12,
      }),
    [searchQuery, selectedCategory],
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
        <div className="max-w-2xl pt-4">
          <SearchBar
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="mb-6"
          />
        </div>

        <div className="flex gap-2 overflow-x-auto pb-4 scrollbar-hide" role="tablist" aria-label="Product categories">
          <CategoryChip
            label="All"
            isSelected={selectedCategory === 'All'}
            onClick={() => setSelectedCategory('All')}
          />
          {categories.map((category) => (
            <CategoryChip
              key={category}
              label={category}
              isSelected={selectedCategory === category}
              onClick={() => setSelectedCategory(category)}
            />
          ))}
        </div>

        {favourites.error && (
          <p role="alert" className="mb-4 text-sm text-error">
            {favourites.error}
          </p>
        )}

        <div className="mt-7">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-text-primary">Featured items</h2>
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
              <p className="text-text-secondary">No items match your search</p>
            </div>
          ) : (
            <FavouritedGrid products={products} favourites={favourites} onNavigate={openProduct} />
          )}
        </div>

        <ForYouSection ref={forYouRef} />
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
const ForYouSection = forwardRef(function ForYouSection(_props, ref) {
  const navigate = useNavigate();
  const [fyCategory, setFyCategory] = useState('All');

  const loadForYou = useCallback(
    () =>
      search({
        category: fyCategory === 'All' ? undefined : fyCategory,
        activeOnly: true,
        size: 8,
        sortBy: 'createdAt',
        direction: 'desc',
      }),
    [fyCategory],
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
          <h2 className="text-xl font-bold text-text-primary tracking-tight leading-tight">For You</h2>
          <p className="text-sm text-text-secondary">Latest listings across the marketplace</p>
        </div>
      </div>

      <div className="flex gap-2 overflow-x-auto pb-4 scrollbar-hide" role="tablist" aria-label="For you categories">
        {categories.map((category) => (
          <CategoryChip
            key={category}
            label={category}
            isSelected={fyCategory === category}
            onClick={() => setFyCategory(category)}
          />
        ))}
      </div>

      <div className="mt-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-text-primary">
            {fyCategory === 'All' ? 'Newest listings' : `${fyCategory} picks`}
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