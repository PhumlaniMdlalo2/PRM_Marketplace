import { useState } from 'react';
import { Minus, Plus, Lock } from 'lucide-react';
import Button from '../components/ui/Button';
import BackButton from '../components/ui/BackButton';
import Layout from '../components/layout/Layout';

const formatPrice = (value) => `R${value.toLocaleString()}`;

const Cart = () => {
  const [items, setItems] = useState([
    {
      id: 1,
      name: 'Blue flower print heels',
      size: '38',
      originalPrice: 1500,
      price: 1200,
      quantity: 1,
      image: null,
    },
    {
      id: 2,
      name: 'Golden ankle heels',
      size: '36',
      originalPrice: null,
      price: 780,
      quantity: 1,
      image: null,
    },
  ]);
  
  const [paymentMethod, setPaymentMethod] = useState('card');
  
  const updateQuantity = (id, delta) => {
    setItems(items.map(item => 
      item.id === id 
        ? { ...item, quantity: Math.max(1, item.quantity + delta) }
        : item
    ));
  };
  
  const cartTotal = items.reduce((sum, item) => sum + (item.price * item.quantity), 0);
  const shipping = 50;
  const total = cartTotal + shipping;
  
  const paymentOptions = [
    { id: 'card', label: 'Credit / debit card' },
    { id: 'eft', label: 'EFT' },
    { id: 'wallet', label: 'Digital wallet' },
  ];
  
  return (
    <Layout showNav={false}>
      <div className="app-container py-6 pb-10">
        <div className="flex items-center gap-4 mb-2">
          <BackButton />
          <div>
            <h1 className="text-2xl font-bold text-text-primary tracking-tight">Your cart</h1>
            <p className="text-sm text-text-secondary">{items.length} {items.length === 1 ? 'item' : 'items'}</p>
          </div>
        </div>
        
        <div className="lg:grid lg:grid-cols-[minmax(0,1fr)_340px] lg:items-start lg:gap-8">
          <div>
        <div className="mt-6 space-y-3.5">
          {items.map((item) => (
            <div key={item.id} className="flex gap-4 p-4 bg-white border border-border rounded-2xl">
              <div className="w-20 h-20 bg-lavender rounded-xl flex-shrink-0"></div>
              <div className="flex-1 min-w-0">
                <h3 className="font-semibold text-text-primary text-[15px] leading-snug">{item.name}</h3>
                <p className="text-sm text-text-secondary mt-0.5">Size: {item.size}</p>
                <div className="flex items-center gap-2 mt-1.5">
                  {item.originalPrice && (
                    <span className="text-xs text-text-muted line-through">{formatPrice(item.originalPrice)}</span>
                  )}
                  <span className="font-bold text-text-primary">{formatPrice(item.price)}</span>
                </div>
                <div className="flex items-center justify-between mt-3">
                  <div className="flex items-center gap-2.5">
                    <button 
                      onClick={() => updateQuantity(item.id, -1)}
                      className="w-8 h-8 rounded-lg border border-border flex items-center justify-center hover:bg-lavender hover:border-lavender-dark active:scale-90 transition-all duration-200"
                      aria-label={`Decrease quantity of ${item.name}`}
                    >
                      <Minus size={14} />
                    </button>
                    <span className="font-semibold w-5 text-center">{item.quantity}</span>
                    <button 
                      onClick={() => updateQuantity(item.id, 1)}
                      className="w-8 h-8 rounded-lg border border-border flex items-center justify-center hover:bg-lavender hover:border-lavender-dark active:scale-90 transition-all duration-200"
                      aria-label={`Increase quantity of ${item.name}`}
                    >
                      <Plus size={14} />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
        
        {items.length === 0 && (
          <div className="py-16 text-center">
            <p className="text-text-secondary">Your cart is empty</p>
            <p className="text-sm text-text-muted mt-1">Browse the marketplace to add items.</p>
          </div>
        )}
        
        <div className="mt-8">
          <h3 className="font-semibold text-text-primary mb-3">You might also like</h3>
          <div className="flex gap-3.5 overflow-x-auto pb-4 scrollbar-hide">
            {[
              { id: 1, name: 'Beige short boot', price: 650 },
              { id: 2, name: 'Leather tote bag', price: 480 },
              { id: 3, name: 'Ribbed knitwear', price: 390 },
            ].map((rec) => (
              <div key={rec.id} className="flex-shrink-0 w-32">
                <div className="w-32 h-32 bg-lavender rounded-xl"></div>
                <p className="text-sm font-medium text-text-primary mt-2 leading-snug">{rec.name}</p>
                <p className="text-sm font-semibold text-text-primary mt-0.5">{formatPrice(rec.price)}</p>
              </div>
            ))}
          </div>
        </div>
        
        <div className="mt-8">
          <h3 className="font-semibold text-text-primary mb-3">Payment method</h3>
          <div className="space-y-2.5">
            {paymentOptions.map((option) => (
              <label
                key={option.id}
                className={`flex items-center gap-3 p-4 border rounded-2xl cursor-pointer transition-all duration-200 ${
                  paymentMethod === option.id 
                    ? 'border-primary bg-primary-muted shadow-sm shadow-primary/10' 
                    : 'border-border hover:border-lavender-dark hover:bg-lavender/30'
                }`}
              >
                <input
                  type="radio"
                  name="payment"
                  value={option.id}
                  checked={paymentMethod === option.id}
                  onChange={(e) => setPaymentMethod(e.target.value)}
                  className="w-4 h-4 text-primary focus:ring-primary accent-primary"
                />
                <span className="text-text-primary font-medium">{option.label}</span>
              </label>
            ))}
          </div>
        </div>
          </div>
          
          <div className="lg:sticky lg:top-20 mt-8 lg:mt-0">
        <div className="p-5 bg-lavender/60 rounded-2xl">
          <h3 className="font-semibold text-text-primary mb-4">Order summary</h3>
          <div className="space-y-2.5">
            <div className="flex justify-between">
              <span className="text-text-secondary">Subtotal</span>
              <span className="text-text-primary font-medium">{formatPrice(cartTotal)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-text-secondary">Shipping</span>
              <span className="text-text-primary font-medium">{formatPrice(shipping)}</span>
            </div>
            <div className="border-t border-border-lavender pt-3 mt-1">
              <div className="flex justify-between">
                <span className="font-bold text-text-primary">Total</span>
                <span className="font-bold text-text-primary">{formatPrice(total)}</span>
              </div>
            </div>
          </div>
          <Button size="lg" className="mt-5">
            <Lock size={16} />
            Proceed to buy · {formatPrice(total)}
          </Button>
        </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default Cart;
