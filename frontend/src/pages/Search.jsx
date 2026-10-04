import { useCallback, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { SlidersHorizontal, PackageX } from 'lucide-react';
import SearchBar from '../components/ui/SearchBar';
import CategoryChip from '../components/ui/CategoryChip';
import ProductCard from '../components/ui/ProductCard';
import EmptyState from '../components/ui/EmptyState';
import { ProductGridSkeleton } from '../components/ui/Skeleton';
import Layout from '../components/layout/Layout';
import { search } from '../api/products';
import { toCardProps } from '../lib/format';
import { useAsync } from '../hooks/useAsync';

const PAGE_SIZE = 24;

const categories = ['Furniture', 'Electronics', 'Home', 'Books', 'Fashion', 'Bikes'];

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
  // Filters live in the URL so a search can be linked and survives a refresh. The keyword is seeded
  // from ?q=, which is how Home's search bar hands off to this page.
  const [params, setParams] = useSearchParams();

  const searchQuery = params.get('q') ?? '';
  const selectedCategory = params.get('category') ?? 'All';
  const condition = params.get('condition') ?? '';
  const city = params.get('city') ?? '';
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
    () =>
      search({
        keyword: searchQuery || undefined,
        category: selectedCategory === 'All' ? undefined : selectedCategory,
        condition: condition || undefined,
        city: city || undefined,
        maxPrice: maxPrice >= MAX_PRICE ? undefined : maxPrice,
        activeOnly: true,
        page,
        size: PAGE_SIZE,
      }),
    [searchQuery, selectedCategory, condition, city, maxPrice, page],
  );

  const { data, loading, error } = useAsync(loadResults);

  const results = (data?.content ?? []).map(toCardProps);

  return (
    <Layout>
      <div className="app-container py-6">
        <h1 className="text-2xl font-bold text-text-primary tracking-tight mb-6">Search</h1>

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

        {showFilters && (
          <div className="space-y-5 pb-2 max-w-2xl">
            <div>
              <h3 className="text-sm font-medium text-text-primary mb-3">Category</h3>
              <div className="flex flex-wrap gap-2">
                {categories.map((category) => (
                  <CategoryChip
                    key={category}
                    label={category}
                    isSelected={selectedCategory === category}
                    onClick={() => update('category', category === 'All' ? '' : category)}
                  />
                ))}
              </div>
            </div>

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
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3.5">
                {results.map((product) => (
                  <ProductCard
                    key={product.id}
                    {...product}
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