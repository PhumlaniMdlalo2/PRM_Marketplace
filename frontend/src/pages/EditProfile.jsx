import { useState } from 'react';
import { Camera } from 'lucide-react';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import BackButton from '../components/ui/BackButton';
import Layout from '../components/layout/Layout';

const EditProfile = () => {
  const [formData, setFormData] = useState({
    name: 'Naledi Zulu',
    email: 'naledi.zulu@cput.ac.za',
    password: '',
    dateOfBirth: '2001-04-12',
    country: 'South Africa',
  });
  const [errors, setErrors] = useState({});
  
  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    if (errors[e.target.name]) setErrors({ ...errors, [e.target.name]: undefined });
  };
  
  const handleSubmit = (e) => {
    e.preventDefault();
    const next = {};
    if (!formData.name.trim()) next.name = 'Name is required';
    if (!formData.email.trim()) next.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) next.email = 'Enter a valid email address';
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    // Handle save
    console.log('Save changes:', formData);
  };
  
  return (
    <Layout showNav={false}>
      <div className="app-container py-6 max-w-xl mx-auto">
        <div className="flex items-center gap-4 mb-6">
          <BackButton />
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Edit profile</h1>
        </div>
        
        <div className="flex justify-center mb-8">
          <div className="relative">
            <div className="w-24 h-24 bg-lavender rounded-[2rem] flex items-center justify-center">
              <span className="text-3xl font-bold text-text-muted">
                {formData.name.charAt(0)}
              </span>
            </div>
            <button 
              className="absolute bottom-0 right-0 w-9 h-9 bg-primary rounded-xl flex items-center justify-center text-white shadow-lg shadow-primary/30 hover:bg-primary-hover active:scale-90 transition-all duration-200"
              aria-label="Change profile photo"
            >
              <Camera size={16} />
            </button>
          </div>
        </div>
        
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <Input
            label="Name"
            name="name"
            error={errors.name}
            value={formData.name}
            onChange={handleChange}
            autoComplete="name"
          />
          
          <Input
            label="Email"
            type="email"
            name="email"
            error={errors.email}
            value={formData.email}
            onChange={handleChange}
            autoComplete="email"
          />
          
          <Input
            label="Password"
            type="password"
            name="password"
            placeholder="Leave blank to keep current"
            hint="Leave blank to keep your current password"
            value={formData.password}
            onChange={handleChange}
            autoComplete="new-password"
          />
          
          <Input
            label="Date of birth"
            type="date"
            name="dateOfBirth"
            value={formData.dateOfBirth}
            onChange={handleChange}
          />
          
          <Input
            label="Country / region"
            name="country"
            value={formData.country}
            onChange={handleChange}
          />
          
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

export default EditProfile;
