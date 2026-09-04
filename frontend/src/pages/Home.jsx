import { useState, useEffect, useRef, forwardRef } from 'react';
import { useLocation } from 'react-router-dom';
import { Sparkles } from 'lucide-react';
import SearchBar from '../components/ui/SearchBar';
import CategoryChip from '../components/ui/CategoryChip';
import ProductCard from '../components/ui/ProductCard';
import { ProductGridSkeleton } from '../components/ui/Skeleton';
import Layout from '../components/layout/Layout';

const Home = () => {
  const { hash } = useLocation();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [loading, setLoading] = useState(true);
  const forYouRef = useRef(null);

  const categories = ['Furniture', 'Electronics', 'Fashion', 'Bikes'];

  useEffect(() => {
    const timer = setTimeout(() => setLoading(false), 800);
    return () => clearTimeout(timer);
  }, []);

  const products = [
    { id: 1, name: 'Mid-century oak coffee table', price: 'R1450', location: 'Cape Town, 4km away', image: null },
    { id: 2, name: 'Vintage teak bookshelf', price: 'R890', location: 'Durban, 11km away', image: null },
    { id: 3, name: 'MacBook Air 2022', price: 'R8,450', location: 'Johannesburg, 2km away', image: null },
    { id: 4, name: 'Trek mountain bike - 29"', price: 'R4,200', location: 'Pretoria, 7km away', image: null },
    { id: 5, name: 'Three-seater leather sofa', price: 'R3,650', location: 'Cape Town, 3km away', image: null },
    { id: 6, name: 'PlayStation 5 + 2 controllers', price: 'R6,300', location: 'Port Elizabeth, 15km away', image: null },
  ];

  useEffect(() => {
    if (hash === '#for-you' && forYouRef.current) {
      forYouRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [hash]);

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
        
        <div className="mt-7">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-text-primary">Featured items</h2>
            <span className="text-xs font-medium text-text-muted">{products.length} results</span>
          </div>
          {loading ? (
            <ProductGridSkeleton count={6} />
          ) : products.length === 0 ? (
            <div className="py-12 text-center">
              <p className="text-text-secondary">No items match your search</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3.5">
              {products.map((product) => (
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

        <ForYouSection ref={forYouRef} />
      </div>
    </Layout>
  );
};

export default Home;

const FOR_YOU_PRODUCTS = [
  { id: 1, name: 'Mid-century oak coffee table', price: 'R1450', location: 'Cape Town, 4km away', image: null },
  { id: 2, name: 'Vintage teak bookshelf', price: 'R890', location: 'Durban, 11km away', image: null },
  { id: 3, name: 'MacBook Air 2022', price: 'R8,450', location: 'Johannesburg, 2km away', image: null },
  { id: 4, name: 'Trek mountain bike - 29"', price: 'R4,200', location: 'Pretoria, 7km away', image: null },
  { id: 5, name: 'Three-seater leather sofa', price: 'R3,650', location: 'Cape Town, 3km away', image: null },
  { id: 6, name: 'PlayStation 5 + 2 controllers', price: 'R6,300', location: 'Port Elizabeth, 15km away', image: null },
  { id: 7, name: 'Vintage vinyl record player', price: 'R1,200', location: 'Bloemfontein, 9km away', image: null },
  { id: 8, name: 'IKEA desk - white', price: 'R750', location: 'Johannesburg, 6km away', image: null },
];

const HIGHLIGHTED = [0, 2, 4];

const ForYouSection = forwardRef((props, ref) => {
  const [fyCategory, setFyCategory] = useState('All');
  const [items, setItems] = useState(() => FOR_YOU_PRODUCTS);
  const [fyLoading, setFyLoading] = useState(false);
  const requestId = useRef(0);
  const fyCategories = ['All', 'Furniture', 'Electronics', 'Fashion', 'Bikes'];

  const pickForCategory = (category) =>
    category === 'All' ? FOR_YOU_PRODUCTS : HIGHLIGHTED.map((i) => FOR_YOU_PRODUCTS[i]);

  const loadForYou = (category) => {
    const id = ++requestId.current;
    setFyLoading(true);
    setTimeout(() => {
      if (id !== requestId.current) return;
      setItems(pickForCategory(category));
      setFyLoading(false);
    }, 500);
  };

  const applyFilter = (category) => {
    setFyCategory(category);
    loadForYou(category);
  };

  return (
    <section ref={ref} id="for-you" className="mt-12 border-t border-border pt-8 scroll-mt-20">
      <div className="flex items-center gap-3 mb-5 max-w-2xl">
        <div className="w-11 h-11 rounded-2xl bg-primary/10 flex items-center justify-center">
          <Sparkles size={22} className="text-primary" />
        </div>
        <div className="flex-1">
          <h2 className="text-xl font-bold text-text-primary tracking-tight leading-tight">For You</h2>
          <p className="text-sm text-text-secondary">Picked based on what you browse</p>
        </div>
      </div>

      <div className="flex gap-2 overflow-x-auto pb-4 scrollbar-hide" role="tablist" aria-label="For you categories">
        {fyCategories.map((category) => (
          <CategoryChip
            key={category}
            label={category}
            isSelected={fyCategory === category}
            onClick={() => applyFilter(category)}
          />
        ))}
      </div>

      <div className="mt-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-text-primary">
            {fyCategory === 'All' ? 'Recommended for you' : `${fyCategory} picks`}
          </h3>
          <span className="text-xs font-medium text-text-muted">{items.length} items</span>
        </div>
        {fyLoading ? (
          <ProductGridSkeleton count={8} />
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3.5">
            {items.map((product) => (
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
    </section>
  );
});
