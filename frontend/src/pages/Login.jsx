import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Mail, Globe } from 'lucide-react';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { useAuth } from '../auth/useAuth';

const Login = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { signIn, isAuthenticated } = useAuth();
  const [formData, setFormData] = useState({
    // Verification sends the address back here after a successful code check, so the user is not
    // asked to retype the email they just confirmed.
    email: location.state?.email ?? '',
    password: '',
    // Ticked by default, and this is the load-bearing part of the token storage decision: the
    // token goes to localStorage and survives a browser restart. Unticking it moves the token to
    // sessionStorage so the session ends with the tab. Un-ticked-by-default would have quietly
    // made every ordinary sign-in session-scoped.
    rememberMe: true,
  });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Where to go once signed in. ProtectedRoute puts the path the user was trying to reach in
  // location.state.from, so following a link to /cart while signed out does not dump them on the
  // home page afterwards.
  //
  // Only same-site paths are honoured. "//evil.example" also starts with a slash but the browser
  // reads it as a protocol-relative URL, so a value arriving in router state must never be passed
  // to navigate unchecked.
  const attempted = location.state?.from;
  const destination =
    typeof attempted === 'string' && attempted.startsWith('/') && !attempted.startsWith('//')
      ? attempted
      : '/';

  // Someone already signed in has no business on this page. This has to be an effect: navigating
  // during render is not allowed, and returning early would render the redirect page instead of
  // this one for a frame.
  useEffect(() => {
    if (isAuthenticated) navigate(destination, { replace: true });
  }, [isAuthenticated, destination, navigate]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({ ...formData, [name]: type === 'checkbox' ? checked : value });
    if (errors[name]) setErrors({ ...errors, [name]: undefined });
    if (formError) setFormError(null);
  };

  const validate = () => {
    const next = {};
    if (!formData.email.trim()) next.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) next.email = 'Enter a valid email address';
    if (!formData.password) next.password = 'Password is required';
    else if (formData.password.length < 6) next.password = 'Password must be at least 6 characters';
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
      await signIn(formData.email.trim(), formData.password, { persistent: formData.rememberMe });
      navigate(destination, { replace: true });
    } catch (caught) {
      // The backend answers a wrong email and a wrong password identically on purpose, so there
      // is no way to tell the user which one was wrong without helping whoever is guessing.
      setFormError(caught.message);
      if (caught.fieldErrors) setErrors(caught.fieldErrors);
    } finally {
      setSubmitting(false);
    }
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
        {formError && (
          <div
            role="alert"
            className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
          >
            {formError}
          </div>
        )}

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
          <Button type="submit" size="lg" disabled={submitting}>
            {submitting ? 'Logging in…' : 'Log in'}
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
