
import { GOODS_CATEGORIES, SERVICE_CATEGORIES } from './marketplaceCategories';

/**
 * Everything the create and edit listing forms share: the option lists, the one place a listing's
 * fields are read and written, and the validation.
 *
 * This is shared because the two forms were already near-identical mocks, and the rules the server
 * enforces in ProductFactory live in one place here so a change to either side is a one-line diff.
 */

/** Mirrors the ProductCondition enum. The labels are what the UI shows, the values are what it sends. */
export const CONDITION_OPTIONS = [
  { value: 'NEW', label: 'New' },
  { value: 'LIKE_NEW', label: 'Like new' },
  { value: 'GOOD', label: 'Good' },
  { value: 'FAIR', label: 'Fair' },
  { value: 'POOR', label: 'Poor' },
];

/** Suggested values for the marketplace's free-text category field. Sellers can still add their own. */
export const PRODUCT_CATEGORY_OPTIONS = GOODS_CATEGORIES.map(({ value }) => value.split(',')[0]);
export const SERVICE_CATEGORY_OPTIONS = SERVICE_CATEGORIES.map(({ value }) => value);
export const CATEGORY_OPTIONS = [...new Set([
  ...PRODUCT_CATEGORY_OPTIONS,
  ...SERVICE_CATEGORY_OPTIONS,
])];

export const listingTypeForCategory = (category) =>
  SERVICE_CATEGORY_OPTIONS.some((option) => option.toLowerCase() === category?.toLowerCase())
    ? 'SERVICE'
    : 'PRODUCT';

/** Blank form. Quantity starts at 1 rather than 0, because 0 would publish an item nobody can buy. */
export const emptyListingForm = () => ({
  listingType: 'PRODUCT',
  name: '',
  category: '',
  price: '',
  stock: '1',
  description: '',
  location: '',
  condition: 'NEW',
  active: true,
  imageUrl: '',
});

/**
 * The product stores city and province separately but the form shows one location field, typed as
 * "Cape Town, Western Cape". Splitting on the first comma only, so a suburb containing a comma
 * cannot end up half-swallowed: "Chopin, 12 Nelson Mandela St, Western Cape" still reads as the city
 * "Chopin".
 */
export const parseLocation = (value) => {
  const [city, ...rest] = String(value ?? '').split(',');
  return { city: city.trim(), province: rest.join(',').trim() };
};

/** The inverse of parseLocation, for filling the form from a product the server already has. */
export const formatLocation = ({ city, province } = {}) =>
  [city, province].filter(Boolean).join(', ');

/** Turns a stored product into form values. Price is stringified because the inputs are text. */
export const listingFromProduct = (product) => ({
  listingType: listingTypeForCategory(product.category ?? ''),
  name: product.name ?? '',
  category: product.category ?? '',
  price: product.price != null ? String(product.price) : '',
  stock: product.stockQuantity != null ? String(product.stockQuantity) : '',
  description: product.description ?? '',
  location: formatLocation(product),
  condition: product.condition ?? 'NEW',
  active: product.active ?? true,
  // A listing without a photo stores null; the form holds '' so the field is always a string.
  imageUrl: product.imageUrl ?? '',
});

/**
 * Form values into a request body.
 *
 * No `active` field: the server ignores it on update, so sending it would look like it worked while
 * doing nothing. Retiring is a separate call, which EditListing makes after saving.
 *
 * `imageUrl` travels with the rest of the values rather than being spliced in by the caller: it
 * starts as whatever the listing already has and becomes the new address when a photo is uploaded.
 * It is always sent, because the server replaces the field with whatever non-null value it is
 * given, including an empty string — omitting it is not the way to leave it alone.
 */
export const toListingPayload = (values) => {
  const { city, province } = parseLocation(values.location);
  return {
    name: values.name.trim(),
    description: values.description.trim(),
    price: Number(values.price),
    stockQuantity: Number(values.stock),
    category: values.category.trim(),
    condition: values.condition,
    city,
    province,
    imageUrl: values.imageUrl,
  };
};

/**
 * The same rules ProductFactory enforces on the server, so the message can sit next to the field that
 * caused it. The server's rejection carries no body, so this is the only place the user is told
 * anything. Location and description are required here but not on the server: a listing nobody can
 * collect from is not worth publishing.
 */
export const validateListing = (values) => {
  const errors = {};

  if (!values.name.trim()) {
    errors.name = values.listingType === 'SERVICE' ? 'Service name is required' : 'Product name is required';
  }

  if (!String(values.price).trim()) {
    errors.price = 'Price is required';
  } else if (!Number.isFinite(Number(values.price)) || Number(values.price) <= 0) {
    errors.price = 'Enter a price greater than zero';
  }

  if (!values.category.trim()) errors.category = 'Choose a category';

  if (!String(values.stock).trim()) {
    errors.stock = 'How many are available';
  } else if (!Number.isInteger(Number(values.stock)) || Number(values.stock) < 0) {
    errors.stock = 'Enter zero or a whole number';
  }

  if (!values.description.trim()) errors.description = 'Description is required';

  if (!parseLocation(values.location).city) errors.location = 'Location is required';

  return errors;
};
