import { useCallback, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { SlidersHorizontal, PackageX } from 'lucide-react';
import SearchBar from '../components/ui/SearchBar';
import CategoryChip from '../components/ui/CategoryChip';
import ProductCard from '../components/ui/ProductCard';
import EmptyState from '../components/ui/EmptyState';
import { ProductGridSkeleton } from '../components/ui/Skeleton';
import Layout from '../components/layout/Layout';
import { search, searchAcrossCategories } from '../api/products';
import { toCardProps } from '../lib/format';
import { useAsync } from '../hooks/useAsync';
import { useFavourites } from '../hooks/useFavourites';
import { useAuth } from '../auth/useAuth';
import { CAMPUSES } from '../lib/campuses';
import { GOODS_CATEGORIES, SERVICE_CATEGORIES } from '../lib/marketplaceCategories';

const PAGE_SIZE = 24;

// Labels for display, values for the wire. The server compares this against the Condition enum
// (NEW, LIKE_NEW, GOOD, FAIR, POOR), so a display string sent as-is would silently match nothing.
const conditions = [
  { label: 'All', value: undefined },
  { label: 'New', value: 'NEW' },
  { label: 'Like new', value: 'LIKE_NEW' },
  { label: 'Good', value: 'GOOD' },
  { label: 'Fair', value: 'FAIR' },
  { label: 'Poor', value: 'POOR' },
];

const MAX_PRICE = 10000;

const Search = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  // Filters live in the URL so a search can be linked and survives a refresh. The keyword is seeded
  // from ?q=, which is how Home's search bar hands off to this page.
  const [params, setParams] = useSearchParams();

  const searchQuery = params.get('q') ?? '';
  const selectedCategory = params.get('category') ?? 'All';
  const section = params.get('section') === 'services' ? 'services' : 'goods';
  const categories = section === 'services' ? SERVICE_CATEGORIES : GOODS_CATEGORIES;
  const condition = params.get('condition') ?? '';
  const city = params.get('city') ?? '';
  const campus = params.get('campus') ?? '';
  const maxPrice = Number(params.get('maxPrice') ?? MAX_PRICE);
  const page = Number(params.get('page') ?? 0);

  const [showFilters, setShowFilters] = useState(false);

  const update = (key, value, { resetPage = true } = {}) => {
    setParams(
      (previous) => {
        const next = new URLSearchParams(previous);
        if (value === '' || value === undefined || value === null) next.delete(key);
        else next.set(key, String(value));
        // Any filter change invalidates the current page number; staying on page 4 of a result set
        // that now has one page shows an empty grid.
        if (resetPage) next.delete('page');
        return next;
      },
      { replace: true },
    );
  };

  const loadResults = useCallback(
    () => {
      const criteria = {
        keyword: searchQuery || undefined,
        category: selectedCategory === 'All' ? undefined : selectedCategory,
        condition: condition || undefined,
        city: city || undefined,
        campus: campus || undefined,
        maxPrice: maxPrice >= MAX_PRICE ? undefined : maxPrice,
        activeOnly: true,
        page,
        size: PAGE_SIZE,
      };
      return selectedCategory === 'All'
        ? searchAcrossCategories(categories.map(({ value }) => value), criteria)
        : search(criteria);
    },
    [searchQuery, categories, selectedCategory, condition, city, campus, maxPrice, page],
  );

  const { data, loading, error } = useAsync(loadResults);

  const results = (data?.content ?? []).map(toCardProps);
  const favourites = useFavourites();

  // A failed save must not take the results with it: the grid stays, and the message says what went
  // wrong with the heart rather than pretending the search failed.
  const onFavouriteToggle = async (productId) => {
    if (!isAuthenticated) {
      // Saved items belong to an account. Sending an anonymous visitor to sign in keeps the promise
      // the heart makes instead of quietly accepting a tap that will not be there later.
      navigate('/login', { state: { from: { pathname: `/search${window.location.search}` } } });
      return;
    }
    await favourites.toggle(productId);
  };

  return (
    <Layout>
      <div className="app-container py-6">
        <h1 className="text-2xl font-bold text-text-primary tracking-tight mb-4">
          {section === 'goods' ? 'Browse goods' : 'Browse services'}
        </h1>

        <nav className="mb-6 flex flex-wrap items-center gap-2" aria-label="Marketplace sections">
          <Link
            to="/marketplace"
            aria-current={section === 'goods' ? 'page' : undefined}
            className={`rounded-xl px-4 py-2 text-sm font-semibold ${
              section === 'goods' ? 'bg-primary text-white' : 'bg-lavender text-text-secondary hover:text-text-primary'
            }`}
          >
            Goods
          </Link>
          <Link
            to="/search?section=services"
            aria-current={section === 'services' ? 'page' : undefined}
            className={`rounded-xl px-4 py-2 text-sm font-semibold ${
              section === 'services' ? 'bg-primary text-white' : 'bg-lavender text-text-secondary hover:text-text-primary'
            }`}
          >
            Services
          </Link>
          <Link
            to="/bulletin"
            className="rounded-xl px-4 py-2 text-sm font-semibold text-primary hover:bg-primary-muted"
          >
            Community
          </Link>
        </nav>

        <div className="max-w-2xl">
          <SearchBar
            value={searchQuery}
            onChange={(e) => update('q', e.target.value)}
            className="mb-4"
          />
        </div>

        <button
          onClick={() => setShowFilters(!showFilters)}
          className="flex items-center gap-2 text-sm font-medium text-text-primary hover:text-primary transition-colors mb-4"
          aria-expanded={showFilters}
        >
          <SlidersHorizontal size={17} />
          {showFilters ? 'Hide filters' : 'Show filters'}
        </button>

        <div className="mb-5">
          <h2 className="mb-3 text-sm font-medium text-text-primary">
            {section === 'goods' ? 'Goods category' : 'Service category'}
          </h2>
          <div className="flex flex-wrap gap-2" aria-label="Filter by category">
            <CategoryChip
              label="All"
              isSelected={selectedCategory === 'All'}
              onClick={() => update('category', '')}
            />
            {categories.map((category) => (
              <CategoryChip
                key={category.value}
                label={category.label}
                isSelected={selectedCategory === category.value}
                onClick={() => update('category', category.value)}
              />
            ))}
          </div>
        </div>

        {showFilters && (
          <div className="space-y-5 pb-2 max-w-2xl">
            <div>
              <div className="flex justify-between mb-2">
                <h3 className="text-sm font-medium text-text-primary">Maximum price</h3>
                <span className="text-sm text-text-secondary">
                  R{maxPrice.toLocaleString('en-ZA')}
                </span>
              </div>
              <input
                type="range"
                min="100"
                max={MAX_PRICE}
                step="100"
                value={maxPrice}
                onChange={(e) => update('maxPrice', e.target.value)}
                className="w-full h-2 bg-lavender rounded-full appearance-none cursor-pointer accent-primary"
                aria-label="Maximum price"
              />
            </div>

            <div>
              <label
                htmlFor="search-city"
                className="text-sm font-medium text-text-primary mb-2 block"
              >
                City
              </label>
              <input
                id="search-city"
                type="text"
                value={city}
                onChange={(e) => update('city', e.target.value)}
                placeholder="e.g. Cape Town"
                className="w-full rounded-xl border border-border px-4 py-2.5 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>

            <div>
              <label htmlFor="search-campus" className="mb-2 block text-sm font-medium text-text-primary">
                Seller campus
              </label>
              <select
                id="search-campus"
                value={campus}
                onChange={(event) => update('campus', event.target.value)}
                className="w-full rounded-xl border border-border bg-white px-4 py-2.5 text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary"
              >
                <option value="">All campuses</option>
                {CAMPUSES.map((campusOption) => (
                  <option key={campusOption} value={campusOption}>{campusOption}</option>
                ))}
              </select>
              {isAuthenticated && (
                <p className="mt-1 text-xs text-text-muted">
                  Campus filters match listings from student sellers who selected a campus in their profile.
                </p>
              )}
            </div>

            <div>
              <h3 className="text-sm font-medium text-text-primary mb-3">Condition</h3>
              <div className="flex flex-wrap gap-2">
                {conditions.map((option) => (
                  <CategoryChip
                    key={option.label}
                    label={option.label}
                    isSelected={condition === (option.value ?? '')}
                    onClick={() => update('condition', option.value)}
                  />
                ))}
              </div>
            </div>
          </div>
        )}

        <div className="mt-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-text-primary">Results</h2>
            <span className="text-xs font-medium text-text-muted">
              {data ? `${data.totalElements} items` : ''}
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
            <ProductGridSkeleton count={8} />
          ) : results.length === 0 ? (
            <EmptyState
              icon={PackageX}
              title="No results found"
              description="Try adjusting your filters or search terms."
            />
          ) : (
            <>
              {favourites.error && (
                <p role="alert" className="mb-4 text-sm text-error">
                  {favourites.error}
                </p>
              )}
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3.5">
                {results.map((product) => (
                  <ProductCard
                    key={product.id}
                    {...product}
                    isFavourite={favourites.isFavourite(product.id)}
                    onFavouriteToggle={() => onFavouriteToggle(product.id)}
                    onClick={() => navigate(`/product/${product.id}`)}
                  />
                ))}
              </div>

              {data.totalPages > 1 && (
                <nav
                  className="flex items-center justify-center gap-4 mt-8"
                  aria-label="Search results pages"
                >
                  <button
                    onClick={() => update('page', page - 1, { resetPage: false })}
                    disabled={data.first}
                    className="text-sm font-medium text-primary disabled:text-text-muted disabled:cursor-not-allowed"
                  >
                    Previous
                  </button>
                  <span className="text-sm text-text-secondary">
                    Page {data.page + 1} of {data.totalPages}
                  </span>
                  <button
                    onClick={() => update('page', page + 1, { resetPage: false })}
                    disabled={data.last}
                    className="text-sm font-medium text-primary disabled:text-text-muted disabled:cursor-not-allowed"
                  >
                    Next
                  </button>
                </nav>
              )}
            </>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default Search;