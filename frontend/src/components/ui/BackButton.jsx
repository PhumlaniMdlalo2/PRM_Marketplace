import { ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const BackButton = ({ className = '', label = 'Go back' }) => {
  const navigate = useNavigate();
  
  return (
    <button
      onClick={() => navigate(-1)}
      className={`p-2 -ml-2 rounded-full hover:bg-lavender active:scale-90 transition-all duration-200 ${className}`}
      aria-label={label}
    >
      <ArrowLeft size={24} />
    </button>
  );
};

export default BackButton;
