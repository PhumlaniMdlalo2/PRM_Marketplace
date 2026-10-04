/**
 * Presentation helpers for turning API payloads into the strings the UI shows.
 *
 * These live in one place because the same three values - price, location, condition - appear on the
 * home grid, the search results and the product page, and formatting them differently in each is how
 * "R1 450" on one screen and "1450" on another happens.
 */

// en-ZA with ZAR is deliberate: the catalogue is priced in rand, and the default locale of the
// browser is not necessarily the one that writes "R1 450".
const priceFormatter = new Intl.NumberFormat('en-ZA', {
  style: 'currency',
  currency: 'ZAR',
  maximumFractionDigits: 0,
});

export const formatPrice = (value) => {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return '';
  return priceFormatter.format(amount);
};

/** "Cape Town, Western Cape", degrading gracefully when only part is known. */
export const formatLocation = ({ city, province } = {}) =>
  [city, province].filter(Boolean).join(', ');

const CONDITION_LABELS = {
  NEW: 'New',
  LIKE_NEW: 'Like new',
  GOOD: 'Good',
  FAIR: 'Fair',
  POOR: 'Poor',
};

export const formatCondition = (value) => CONDITION_LABELS[value] ?? value ?? '';

/**
 * Reshapes a Product into what ProductCard expects.
 *
 * ProductCard takes already-formatted strings, so this is where the raw API numbers and codes stop.
 */
export const toCardProps = (product) => ({
  id: product.id,
  name: product.name ?? 'Untitled listing',
  price: formatPrice(product.price),
  location: formatLocation(product) || 'Location not set',
// The server sends an empty string rather than null for a listing with no image, and `??` only
  // catches null/undefined - so an empty src would reach the <img> and render as a broken image
  // instead of the placeholder tile.
  image: product.imageUrl || null,
});