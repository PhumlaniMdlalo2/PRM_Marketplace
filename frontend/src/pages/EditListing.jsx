import { useState } from 'react';
import { MapPin } from 'lucide-react';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import BackButton from '../components/ui/BackButton';
import Layout from '../components/layout/Layout';

const EditListing = () => {
  const [formData, setFormData] = useState({
    name: 'Mid-century oak coffee table',
    price: '1450',
    description: 'Solid oak table in excellent condition. Minor scuff on one leg.',
    location: 'Cape Town',
    condition: 'Like New',
  });
  
  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };
  
  const handleSubmit = (e) => {
    e.preventDefault();
    // Handle update
    console.log('Update listing:', formData);
  };
  
  const conditions = ['New', 'Like New', 'Good', 'Fair'];
  
  return (
    <Layout>
      <div className="app-container py-6 pb-24 max-w-2xl mx-auto">
        <div className="flex items-center gap-4 mb-2">
          <BackButton />
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Edit listing</h1>
        </div>
        <p className="text-text-secondary mb-6 ml-12">Update your listing details</p>
        
        <div className="mb-6">
          <label className="block text-sm font-medium text-text-primary mb-2">
            Product image
          </label>
          <div className="aspect-video bg-lavender rounded-2xl overflow-hidden">
            <div className="w-full h-full flex items-center justify-center text-text-muted">
              Current image
            </div>
          </div>
        </div>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Product name"
            name="name"
            value={formData.name}
            onChange={handleChange}
          />
          
          <Input
            label="Product price"
            name="price"
            type="number"
            value={formData.price}
            onChange={handleChange}
          />
          
          <div className="w-full">
            <label className="block text-sm font-medium text-text-primary mb-1.5">
              Product description
            </label>
            <textarea
              name="description"
              rows={4}
              value={formData.description}
              onChange={handleChange}
              className="w-full px-4 py-3 bg-white border border-border rounded-xl text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all duration-200 resize-none"
            />
          </div>
          
          <Input
            label="Location"
            name="location"
            icon={MapPin}
            value={formData.location}
            onChange={handleChange}
          />
          
          <div>
            <label className="block text-sm font-medium text-text-primary mb-2">
              Condition
            </label>
            <div className="flex flex-wrap gap-2">
              {conditions.map((condition) => (
                <button
                  key={condition}
                  type="button"
                  onClick={() => setFormData({ ...formData, condition })}
                  aria-pressed={formData.condition === condition}
                  className={`px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 active:scale-95 ${
                    formData.condition === condition
                      ? 'bg-primary text-white shadow-md shadow-primary/20'
                      : 'bg-white border border-border text-text-primary hover:bg-lavender'
                  }`}
                >
                  {condition}
                </button>
              ))}
            </div>
          </div>
          
          <div className="pt-4">
            <Button type="submit" size="lg">
              Save changes
            </Button>
          </div>
        </form>
      </div>
    </Layout>
  );
};

export default EditListing;
