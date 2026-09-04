import { Heart } from 'lucide-react';
import { useState } from 'react';

const ProductCard = ({ 
  name, 
  price, 
  location, 
  image, 
  isFavourite = false, 
  onFavouriteToggle,
  onClick 
}) => {
  const [favourite, setFavourite] = useState(isFavourite);
  
  const handleFavouriteToggle = (e) => {
    e.stopPropagation();
    const next = !favourite;
    setFavourite(next);
    onFavouriteToggle?.(next);
  };
  
  return (
    <article 
      onClick={onClick}
      className="bg-white rounded-2xl overflow-hidden cursor-pointer group transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-primary/10 active:scale-[0.99]"
      aria-label={`${name}, ${price}, ${location}`}
    >
      <div className="relative aspect-square bg-lavender m-1.5 rounded-2xl overflow-hidden">
        {image ? (
          <img 
            src={image} 
            alt={name}
            className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <span className="text-xs font-medium text-text-muted/60">{name}</span>
          </div>
        )}
        <button
          onClick={handleFavouriteToggle}
          className="absolute top-2.5 right-2.5 p-2 bg-white/90 backdrop-blur rounded-full shadow-sm hover:bg-white transition-all duration-200 active:scale-90"
          aria-label={favourite ? `Remove ${name} from favourites` : `Add ${name} to favourites`}
        >
          <Heart 
            size={17} 
            className={`transition-all duration-200 ${favourite ? 'fill-error text-error' : 'text-text-muted'}`} 
          />
        </button>
      </div>
      <div className="px-3 pb-3 pt-2">
        <h3 className="font-medium text-text-primary text-sm leading-snug line-clamp-2">
          {name}
        </h3>
        <p className="font-semibold text-text-primary mt-1.5 text-[15px]">{price}</p>
        <p className="text-xs text-text-secondary mt-1 flex items-center gap-1">
          <span className="w-1 h-1 rounded-full bg-text-muted inline-block" aria-hidden="true" />
          {location}
        </p>
      </div>
    </article>
  );
};

export default ProductCard;
