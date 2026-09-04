import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';

const SignUp = () => {
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    dateOfBirth: '',
    phoneNumber: '',
    password: '',
  });
  const [errors, setErrors] = useState({});
  
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
    if (errors[name]) setErrors({ ...errors, [name]: undefined });
  };
  
  const validate = () => {
    const next = {};
    if (!formData.fullName.trim()) next.fullName = 'Full name is required';
    else if (formData.fullName.trim().split(' ').length < 2) next.fullName = 'Please enter your full name';
    
    if (!formData.email.trim()) next.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) next.email = 'Enter a valid email address';
    
    if (!formData.dateOfBirth) next.dateOfBirth = 'Date of birth is required';
    
    if (!formData.phoneNumber.trim()) next.phoneNumber = 'Phone number is required';
    else if (!/^[+\d][\d\s-]{8,}$/.test(formData.phoneNumber.trim())) next.phoneNumber = 'Enter a valid phone number';
    
    if (!formData.password) next.password = 'Password is required';
    else if (formData.password.length < 8) next.password = 'Password must be at least 8 characters';
    
    return next;
  };
  
  const handleSubmit = (e) => {
    e.preventDefault();
    const validationErrors = validate();
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;
    // Handle sign up
    console.log('Sign up:', formData);
  };
  
  return (
    <div className="min-h-dvh bg-white px-6 py-8">
      <div className="w-full max-w-md mx-auto">
      <button className="p-2 -ml-2 text-text-secondary hover:text-text-primary transition-colors" aria-label="Go back">
        <ArrowLeft size={24} />
      </button>
      
      <div className="mt-6">
        <h1 className="text-3xl font-bold text-text-primary tracking-tight text-wrap-balance">Create your account</h1>
        <p className="mt-2 text-text-secondary text-wrap-pretty">
          Already have an account?{' '}
          <Link to="/login" className="text-primary font-semibold hover:underline">Login</Link>
        </p>
      </div>
      
      <form onSubmit={handleSubmit} className="mt-8 space-y-4" noValidate>
        <Input
          label="Full Name"
          name="fullName"
          placeholder="Enter your full name"
          error={errors.fullName}
          value={formData.fullName}
          onChange={handleChange}
          autoComplete="name"
        />
        
        <Input
          label="University Email"
          type="email"
          name="email"
          placeholder="e.g., you@cput.ac.za"
          error={errors.email}
          value={formData.email}
          onChange={handleChange}
          autoComplete="email"
        />
        
        <Input
          label="Date of Birth"
          type="date"
          name="dateOfBirth"
          error={errors.dateOfBirth}
          value={formData.dateOfBirth}
          onChange={handleChange}
        />
        
        <Input
          label="Phone Number"
          type="tel"
          name="phoneNumber"
          placeholder="e.g., +27 82 123 4567"
          error={errors.phoneNumber}
          value={formData.phoneNumber}
          onChange={handleChange}
          autoComplete="tel"
        />
        
        <Input
          label="Set Password"
          type="password"
          name="password"
          placeholder="At least 8 characters"
          error={errors.password}
          value={formData.password}
          onChange={handleChange}
          autoComplete="new-password"
        />
        
        <div className="pt-4">
          <Button type="submit" size="lg">
            Register
          </Button>
        </div>
      </form>
      </div>
    </div>
  );
};

export default SignUp;
