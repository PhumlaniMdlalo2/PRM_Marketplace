import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, Globe } from 'lucide-react';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';

const Login = () => {
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    rememberMe: false,
  });
  const [errors, setErrors] = useState({});
  
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({ ...formData, [name]: type === 'checkbox' ? checked : value });
    if (errors[name]) setErrors({ ...errors, [name]: undefined });
  };
  
  const validate = () => {
    const next = {};
    if (!formData.email.trim()) next.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) next.email = 'Enter a valid email address';
    if (!formData.password) next.password = 'Password is required';
    else if (formData.password.length < 6) next.password = 'Password must be at least 6 characters';
    return next;
  };
  
  const handleSubmit = (e) => {
    e.preventDefault();
    const validationErrors = validate();
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;
    // Handle login
    console.log('Login:', formData);
  };
  
  return (
    <div className="min-h-dvh bg-white px-6 pt-12 pb-8">
      <div className="w-full max-w-md mx-auto">
      <div>
        <h1 className="text-3xl font-bold text-text-primary tracking-tight text-wrap-balance">Welcome back</h1>
        <p className="mt-2 text-text-secondary text-wrap-pretty">
          Don't have an account?{' '}
          <Link to="/signup" className="text-primary font-semibold hover:underline">Sign Up</Link>
        </p>
      </div>
      
      <form onSubmit={handleSubmit} className="mt-8 space-y-4" noValidate>
        <Input
          label="Email"
          type="email"
          name="email"
          placeholder="you@university.ac.za"
          icon={Mail}
          error={errors.email}
          value={formData.email}
          onChange={handleChange}
          autoComplete="email"
        />
        
        <Input
          label="Password"
          type="password"
          name="password"
          placeholder="Enter your password"
          error={errors.password}
          value={formData.password}
          onChange={handleChange}
          autoComplete="current-password"
        />
        
        <div className="flex items-center justify-between">
          <label className="flex items-center gap-2 cursor-pointer select-none">
            <input
              type="checkbox"
              name="rememberMe"
              checked={formData.rememberMe}
              onChange={handleChange}
              className="w-4 h-4 rounded border-border text-primary focus:ring-primary accent-primary"
            />
            <span className="text-sm text-text-secondary">Remember me</span>
          </label>
          <Link to="/forgot-password" className="text-sm text-primary font-semibold hover:underline">
            Forgot Password?
          </Link>
        </div>
        
        <div className="pt-4">
          <Button type="submit" size="lg">
            Log in
          </Button>
        </div>
      </form>
      
      <div className="mt-8" role="separator" aria-label="Or continue with">
        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-border"></div>
          </div>
          <div className="relative flex justify-center text-sm">
            <span className="px-3 bg-white text-text-muted">or</span>
          </div>
        </div>
        
        <div className="mt-6 space-y-3">
          <Button variant="secondary" size="lg">
            <Globe size={18} />
            Continue with Google
          </Button>
          <Button variant="secondary" size="lg">
            Continue with Facebook
          </Button>
        </div>
      </div>
      </div>
    </div>
  );
};

export default Login;
