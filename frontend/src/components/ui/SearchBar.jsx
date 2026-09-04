import { Search } from 'lucide-react';

const SearchBar = ({ 
  placeholder = 'Search for an item', 
  value, 
  onChange, 
  className = '' 
}) => {
  return (
    <div className={`relative w-full ${className}`}>
      <span className="absolute left-4 top-1/2 -translate-y-1/2 text-text-muted pointer-events-none flex items-center" aria-hidden="true">
        <Search size={20} />
      </span>
      <input
        type="text"
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        aria-label="Search"
        className="w-full pl-12 pr-4 py-3.5 bg-lavender rounded-full text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary/40 focus:bg-white transition-all duration-200 hover:bg-lavender-dark"
      />
    </div>
  );
};

export default SearchBar;
