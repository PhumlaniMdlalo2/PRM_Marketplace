import { useState } from 'react';
import { SlidersHorizontal } from 'lucide-react';
import SearchBar from '../components/ui/SearchBar';
import CategoryChip from '../components/ui/CategoryChip';
import ProductCard from '../components/ui/ProductCard';
import EmptyState from '../components/ui/EmptyState';
import { PackageX } from 'lucide-react';
import Layout from '../components/layout/Layout';

const Search = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [priceRange, setPriceRange] = useState([0, 10000]);
  const [distance, setDistance] = useState(50);
  const [condition, setCondition] = useState('All');
  const [showFilters, setShowFilters] = useState(false);
  
  const categories = ['Furniture', 'Electronics', 'Home', 'Books', 'Fashion', 'Bikes'];
  const conditions = ['All', 'New', 'Like New', 'Good', 'Fair'];
  
  const products = [
    { id: 1, name: 'Oak dining chairs (set of 4)', price: 'R1,450', location: 'Cape Town, 5km away', image: null },
    { id: 2, name: 'Reading lamp - brass', price: 'R320', location: 'Durban, 10km away', image: null },
    { id: 3, name: 'iPhone 13 - 128GB', price: 'R8,450', location: 'Johannesburg, 3km away', image: null },
    { id: 4, name: 'Giant mountain bike', price: 'R4,200', location: 'Pretoria, 7km away', image: null },
  ];
  
  const filteredProducts = products.filter(() => true);
  
  return (
    <Layout>
      <div className="app-container py-6">
        <h1 className="text-2xl font-bold text-text-primary tracking-tight mb-6">Search</h1>
        
        <div className="max-w-2xl">
          <SearchBar 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
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
                    onClick={() => setSelectedCategory(category)}
                  />
                ))}
              </div>
            </div>
            
            <div>
              <div className="flex justify-between mb-2">
                <h3 className="text-sm font-medium text-text-primary">Price range</h3>
                <span className="text-sm text-text-secondary">R{priceRange[0].toLocaleString()} - R{priceRange[1].toLocaleString()}</span>
              </div>
              <input
                type="range"
                min="0"
                max="10000"
                step="100"
                value={priceRange[1]}
                onChange={(e) => setPriceRange([priceRange[0], parseInt(e.target.value)])}
                className="w-full h-2 bg-lavender rounded-full appearance-none cursor-pointer accent-primary"
                aria-label="Maximum price"
              />
            </div>
            
            <div>
              <div className="flex justify-between mb-2">
                <h3 className="text-sm font-medium text-text-primary">Distance</h3>
                <span className="text-sm text-text-secondary">{distance} km</span>
              </div>
              <input
                type="range"
                min="1"
                max="100"
                value={distance}
                onChange={(e) => setDistance(parseInt(e.target.value))}
                className="w-full h-2 bg-lavender rounded-full appearance-none cursor-pointer accent-primary"
                aria-label="Maximum distance"
              />
            </div>
            
            <div>
              <h3 className="text-sm font-medium text-text-primary mb-3">Condition</h3>
              <div className="flex flex-wrap gap-2">
                {conditions.map((cond) => (
                  <CategoryChip 
                    key={cond}
                    label={cond} 
                    isSelected={condition === cond}
                    onClick={() => setCondition(cond)}
                  />
                ))}
              </div>
            </div>
          </div>
        )}
        
        <div className="mt-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-text-primary">Results</h2>
            <span className="text-xs font-medium text-text-muted">{filteredProducts.length} items</span>
          </div>
          {filteredProducts.length === 0 ? (
            <EmptyState
              icon={PackageX}
              title="No results found"
              description="Try adjusting your filters or search terms."
            />
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3.5">
              {filteredProducts.map((product) => (
                <ProductCard
                  key={product.id}
                  name={product.name}
                  price={product.price}
                  location={product.location}
                  image={product.image}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default Search;
