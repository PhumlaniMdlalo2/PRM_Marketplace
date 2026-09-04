import ProductCard from '../components/ui/ProductCard';
import BackButton from '../components/ui/BackButton';
import EmptyState from '../components/ui/EmptyState';
import { HeartOff } from 'lucide-react';
import Layout from '../components/layout/Layout';

const SavedItems = () => {
  const savedProducts = [
    { id: 1, name: 'Vintage teak bookshelf', price: 'R890', location: 'Durban, 10km away', image: null },
    { id: 2, name: 'Three-seater leather sofa', price: 'R3,650', location: 'Cape Town, 2km away', image: null },
    { id: 3, name: 'PlayStation 5 + controllers', price: 'R6,300', location: 'Port Elizabeth, 15km away', image: null },
    { id: 4, name: 'Giant mountain bike - 29"', price: 'R4,200', location: 'Pretoria, 7km away', image: null },
  ];
  
  return (
    <Layout>
      <div className="app-container py-6">
        <div className="flex items-center gap-4 mb-2">
          <BackButton />
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Saved items</h1>
        </div>
        <p className="text-text-secondary mb-6 ml-12">{savedProducts.length} items you've favourited</p>
        
        {savedProducts.length === 0 ? (
          <EmptyState
            icon={HeartOff}
            title="Nothing saved yet"
            description="Tap the heart on any item to save it here for later."
          />
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3.5">
            {savedProducts.map((product) => (
              <ProductCard
                key={product.id}
                name={product.name}
                price={product.price}
                location={product.location}
                image={product.image}
                isFavourite={true}
              />
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
};

export default SavedItems;
