import { useState } from 'react';
import { Upload, MapPin } from 'lucide-react';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import BackButton from '../components/ui/BackButton';
import Layout from '../components/layout/Layout';

const CreateListing = () => {
  const [formData, setFormData] = useState({
    name: '',
    price: '',
    description: '',
    location: '',
    condition: 'New',
  });
  const [errors, setErrors] = useState({});
  
  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    if (errors[e.target.name]) setErrors({ ...errors, [e.target.name]: undefined });
  };
  
  const handleSubmit = (e) => {
    e.preventDefault();
    const next = {};
    if (!formData.name.trim()) next.name = 'Product name is required';
    if (!formData.price.trim()) next.price = 'Price is required';
    else if (isNaN(Number(formData.price)) || Number(formData.price) <= 0) next.price = 'Enter a valid price';
    if (!formData.description.trim()) next.description = 'Description is required';
    if (!formData.location.trim()) next.location = 'Location is required';
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    // Handle create
    console.log('Create listing:', formData);
  };
  
  const conditions = ['New', 'Like New', 'Good', 'Fair'];
  
  return (
    <Layout>
      <div className="app-container py-6 pb-24 max-w-2xl mx-auto">
        <div className="flex items-center gap-4 mb-6">
          <BackButton />
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Create listing</h1>
        </div>
        
        <div className="mb-6">
          <label className="block text-sm font-medium text-text-primary mb-2">
            Product image
          </label>
          <div className="aspect-video bg-lavender rounded-2xl flex flex-col items-center justify-center cursor-pointer hover:bg-lavender-dark active:scale-[0.99] transition-all duration-200 border-2 border-dashed border-lavender-dark">
            <span className="w-12 h-12 rounded-2xl bg-white flex items-center justify-center mb-2 shadow-sm">
              <Upload size={22} className="text-primary" />
            </span>
            <p className="text-sm font-medium text-text-secondary">Tap to upload an image</p>
            <p className="text-xs text-text-muted mt-1">PNG, JPG up to 5MB</p>
          </div>
        </div>
        
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <Input
            label="Product name"
            name="name"
            placeholder="Enter a descriptive title"
            error={errors.name}
            value={formData.name}
            onChange={handleChange}
          />
          
          <Input
            label="Product price"
            name="price"
            type="number"
            placeholder="Enter price in Rand"
            error={errors.price}
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
              placeholder="Describe condition, dimensions, and any notes for buyers"
              value={formData.description}
              onChange={handleChange}
              className={`w-full px-4 py-3 bg-white border ${errors.description ? 'border-error' : 'border-border'} rounded-xl text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all duration-200 resize-none`}
            />
            {errors.description && (
              <p className="mt-1 text-sm text-error">• {errors.description}</p>
            )}
          </div>
          
          <Input
            label="Location"
            name="location"
            placeholder="Enter pickup location"
            icon={MapPin}
            error={errors.location}
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
              Create listing
            </Button>
          </div>
        </form>
      </div>
    </Layout>
  );
};

export default CreateListing;
