import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { useAuth } from '../auth/useAuth';

// The date-of-birth field that used to sit on this form is gone. The backend has no column for it
// and no endpoint that accepts it, so it was collecting something that went nowhere. Persisting it
// is a schema change (migration plus a retention decision), not a frontend change — put it back
// once there is somewhere to put it.
const SignUp = () => {
  const navigate = useNavigate();
  const { signUp, isAuthenticated } = useAuth();
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phoneNumber: '',
    password: '',
  });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isAuthenticated) navigate('/', { replace: true });
  }, [isAuthenticated, navigate]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
    if (errors[name]) setErrors({ ...errors, [name]: undefined });
    if (formError) setFormError(null);
  };

  const validate = () => {
    const next = {};
    if (!formData.fullName.trim()) next.name = 'Full name is required';
    else if (formData.fullName.trim().split(' ').length < 2) next.name = 'Please enter your full name';

    if (!formData.email.trim()) next.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) next.email = 'Enter a valid email address';

    if (!formData.phoneNumber.trim()) next.phone = 'Phone number is required';
    else if (!/^[+\d][\d\s-]{8,}$/.test(formData.phoneNumber.trim())) next.phone = 'Enter a valid phone number';

    if (!formData.password) next.password = 'Password is required';
    else if (formData.password.length < 8) next.password = 'Password must be at least 8 characters';

    return next;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validationErrors = validate();
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;

    setSubmitting(true);
    setFormError(null);
    try {
      // The field names are the backend's, which is why the keys above are `name` and `phone`
      // even though the inputs are labelled full name and phone number. That is deliberate: the
      // server's fieldErrors come back keyed the same way, so they land on the right input without
      // a translation table.
      await signUp({
        name: formData.fullName.trim(),
        email: formData.email.trim(),
        phone: formData.phoneNumber.trim(),
        password: formData.password,
      });
      // No session is stored here. The account is unverified and the server refuses to issue a
      // usable token for one, so the next step is the emailed code.
      navigate('/verification', { replace: true, state: { email: formData.email.trim() } });
    } catch (caught) {
      setFormError(caught.message);
      if (caught.fieldErrors) setErrors(caught.fieldErrors);
    } finally {
      setSubmitting(false);
    }
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
        {formError && (
          <div
            role="alert"
            className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
          >
            {formError}
          </div>
        )}

        <Input
          label="Full Name"
          name="fullName"
          placeholder="Enter your full name"
          error={errors.name}
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
          label="Phone Number"
          type="tel"
          name="phoneNumber"
          placeholder="e.g., +27 82 123 4567"
          error={errors.phone}
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
          <Button type="submit" size="lg" disabled={submitting}>
            {submitting ? 'Creating account…' : 'Register'}
          </Button>
        </div>
      </form>
      </div>
    </div>
  );
};

export default SignUp;
