import { Heart } from 'lucide-react';

/**
 * A product card. The heart is controlled: `isFavourite` says whether the product is saved and
 * `onFavouriteToggle` reports a tap, so the parent decides what happens and a save the server
 * rejects cannot leave a filled heart lying about the database.
 *
 * There used to be a second mode here where the card kept its own state and the heart flipped
 * locally with nothing persisted. Every page that used it is now wired, so the mode is gone: a heart
 * that appears to work but is forgotten on refresh is worse than one the user is told does not work.
 */
const ProductCard = ({
  name,
  price,
  location,
  image,
  isFavourite = false,
  onFavouriteToggle,
  onClick
}) => {
  const handleFavouriteToggle = (e) => {
    // The card is wrapped in a link on some pages, so without this the heart navigates as well.
    e.stopPropagation();
    e.preventDefault();
    if (onFavouriteToggle) onFavouriteToggle();
  };

  return (
    <article
      onClick={onClick}
      onKeyDown={(event) => {
        if (event.target === event.currentTarget && (event.key === 'Enter' || event.key === ' ')) {
          event.preventDefault();
          onClick?.();
        }
      }}
      role="group"
      tabIndex={onClick ? 0 : undefined}
      className="vendra-product group"
      aria-label={`${name}, ${price}, ${location}`}
    >
      <div className="vendra-product-image">
        {image ? (
          <img 
            src={image} 
            alt={name}
            className="vendra-product-photo"
          />
        ) : (
          <div className="vendra-product-placeholder">
            <span>{name}</span>
          </div>
        )}
        <button
          onClick={handleFavouriteToggle}
          className="vendra-save"
          aria-label={isFavourite ? `Remove ${name} from favourites` : `Add ${name} to favourites`}
          aria-pressed={isFavourite}
        >
          <Heart
            size={17}
            className={`transition-all duration-200 ${isFavourite ? 'fill-error text-error' : 'text-text-primary'}`}
          />
        </button>
      </div>
      <div className="vendra-product-copy">
        <p className="vendra-product-price">{price}</p>
        <h3>{name}</h3>
        <p className="vendra-product-location">{location}</p>
      </div>
    </article>
  );
};

export default ProductCard;
