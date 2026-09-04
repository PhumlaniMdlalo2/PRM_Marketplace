import { ArrowLeft, Search } from 'lucide-react';
import Button from '../components/ui/Button';
import { useNavigate, Link } from 'react-router-dom';

const NotFound = () => {
  const navigate = useNavigate();
  
  return (
    <div className="min-h-dvh bg-white flex flex-col items-center justify-center px-6 text-center">
      <div className="w-24 h-24 rounded-[2rem] bg-lavender flex items-center justify-center mb-6">
        <span className="text-4xl font-bold text-primary">PRM</span>
      </div>
      <p className="text-sm font-semibold text-primary uppercase tracking-wider">404</p>
      <h1 className="text-3xl font-bold text-text-primary mt-2 text-wrap-balance">
        This page seems to have gone missing
      </h1>
      <p className="mt-3 text-text-secondary max-w-[300px] text-wrap-pretty">
        The link you followed may be broken, or the page may have been removed.
      </p>
      
      <div className="mt-8 w-full max-w-[300px] space-y-3">
        <Button size="lg" onClick={() => navigate('/')}>
          Back to marketplace
        </Button>
        <Button variant="secondary" size="lg" onClick={() => navigate(-1)}>
          <ArrowLeft size={16} />
          Go back
        </Button>
      </div>
      
      <div className="mt-10 text-xs text-text-muted">
        <Link to="/search" className="hover:text-primary transition-colors inline-flex items-center gap-1">
          <Search size={12} /> Try searching instead
        </Link>
      </div>
    </div>
  );
};

export default NotFound;
