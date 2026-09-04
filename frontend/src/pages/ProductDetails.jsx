import { Heart, Star, ShieldCheck, MessageSquare } from 'lucide-react';
import { useState } from 'react';
import Button from '../components/ui/Button';
import Avatar from '../components/ui/Avatar';
import BackButton from '../components/ui/BackButton';
import ProductCard from '../components/ui/ProductCard';
import Layout from '../components/layout/Layout';

const ProductDetails = () => {
  const [isFavourite, setIsFavourite] = useState(false);
  
  const product = {
    name: 'Mid-century oak coffee table',
    price: 'R1,450',
    condition: 'Like New',
    description: 'Solid oak coffee table in excellent condition. Minor scuff on one leg, otherwise looks brand new. Dimensions: 120 x 60 x 45cm. Cash on collection preferred.',
    seller: {
      name: 'Thando Nkosi',
      rating: 4.8,
      reviews: 23,
      location: 'Cape Town',
      since: 'Member since 2024',
    },
  };
  
  const similarProducts = [
    { id: 1, name: 'Vintage teak bookshelf', price: 'R890', location: 'Cape Town, 3km away', image: null },
    { id: 2, name: 'Three-seater leather sofa', price: 'R3,650', location: 'Cape Town, 8km away', image: null },
  ];
  
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
              <div className="w-full h-full flex items-center justify-center text-text-muted">
                <span className="text-sm font-medium">{product.name}</span>
              </div>
            </div>
            
            <div className="py-6 md:py-0">
              <div className="flex items-center gap-2 mb-2">
                <span className="text-xs font-semibold text-primary bg-primary-muted px-2.5 py-1 rounded-md">
                  {product.condition}
                </span>
                <span className="text-xs text-text-secondary flex items-center gap-1">
                  <ShieldCheck size={14} className="text-success" /> Verified seller
                </span>
              </div>
              
              <h1 className="text-2xl font-bold text-text-primary tracking-tight text-wrap-balance">{product.name}</h1>
              <p className="text-2xl font-bold text-primary mt-2 tracking-tight">{product.price}</p>
              <p className="text-sm text-text-secondary mt-1">{product.seller.location}</p>
              
              <div className="mt-7">
                <h3 className="text-base font-semibold text-text-primary mb-2">Description</h3>
                <p className="text-text-secondary leading-relaxed text-wrap-pretty max-w-[65ch]">{product.description}</p>
              </div>
              
              <div className="mt-7 flex items-center gap-4 p-4 rounded-2xl bg-lavender">
                <Avatar size="lg" alt={product.seller.name} />
                <div>
                  <p className="font-semibold text-text-primary">{product.seller.name}</p>
                  <div className="flex items-center gap-1.5 mt-1">
                    <Star size={14} className="fill-warning text-warning" />
                    <span className="text-sm font-medium text-text-primary">{product.seller.rating}</span>
                    <span className="text-sm text-text-secondary">({product.seller.reviews} reviews)</span>
                  </div>
                  <p className="text-xs text-text-muted mt-0.5">{product.seller.since}</p>
                </div>
              </div>
            </div>
          </div>
          
          <div className="mt-8">
            <h3 className="text-base font-semibold text-text-primary mb-4">Similar items</h3>
            <div className="flex gap-3.5 overflow-x-auto pb-4 scrollbar-hide">
              {similarProducts.map((item) => (
                <div key={item.id} className="flex-shrink-0 w-40">
                  <ProductCard
                    name={item.name}
                    price={item.price}
                    location={item.location}
                    image={item.image}
                  />
                </div>
              ))}
            </div>
          </div>
        </div>
        
        <div className="fixed bottom-0 left-0 right-0 bg-white/95 backdrop-blur border-t border-border p-4 z-40 safe-area-inset-bottom">
          <div className="app-container flex gap-3">
            <Button variant="secondary" className="flex-1">
              <MessageSquare size={18} />
              Contact
            </Button>
            <Button className="flex-[1.4]">
              Buy now
            </Button>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default ProductDetails;
