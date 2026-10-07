import { useState } from 'react';
import { Camera, MapPin, Package } from 'lucide-react';
import Button from '../ui/Button';
import Input from '../ui/Input';
import {
  CONDITION_OPTIONS,
  PRODUCT_CATEGORY_OPTIONS,
  SERVICE_CATEGORY_OPTIONS,
} from '../../lib/listingForm';

/**
 * The listing form, shared by create and edit.
 *
 * Presentational on purpose: it owns no data fetching and knows nothing about products, so both
 * pages stay responsible for their own load, validation timing and navigation. Callers mount it with
 * a `key` tied to the product id, which is what lets it take its initial values straight from props
 * instead of copying them into state from an effect after the data lands.
 *
 * The photo picker follows the same rule: the file input hands the chosen file up through
 * `onPickImage` and renders whatever address the values hold — the upload itself, and its failure,
 * belong to the page.
 */
const ListingEditor = ({
  values,
  errors,
  onChange,
  onSubmit,
  submitting = false,
  submitError,
  submitLabel,
  onPickImage,
  uploading = false,
  uploadError,
  showRetire = false,
}) => {
  const listingType = values.listingType ?? 'PRODUCT';
  const isService = listingType === 'SERVICE';
  const categoryOptions = isService ? SERVICE_CATEGORY_OPTIONS : PRODUCT_CATEGORY_OPTIONS;
  const [customCategory, setCustomCategory] = useState(
    Boolean(values.category) && !categoryOptions.includes(values.category),
  );

  const field = (name) => ({
    name,
    value: values[name],
    error: errors[name],
    onChange: (event) => onChange(event.target.name, event.target.value),
  });

  return (
    <form onSubmit={onSubmit} className="space-y-4" noValidate>
      <fieldset>
        <legend className="mb-2 text-sm font-medium text-text-primary">What are you listing?</legend>
        <div className="flex gap-2" role="group" aria-label="Listing type">
          {[
            { value: 'PRODUCT', label: 'Product' },
            { value: 'SERVICE', label: 'Service' },
          ].map((type) => (
            <button
              key={type.value}
              type="button"
              aria-pressed={listingType === type.value}
              onClick={() => {
                if (listingType === type.value) return;
                onChange('listingType', type.value);
                onChange('category', '');
                setCustomCategory(false);
              }}
              className={`min-h-11 rounded-xl border px-4 text-sm font-semibold transition-colors active:scale-[0.98] ${
                listingType === type.value
                  ? 'border-primary bg-primary-muted text-primary'
                  : 'border-border bg-white text-text-secondary hover:bg-lavender'
              }`}
            >
              {type.label}
            </button>
          ))}
        </div>
      </fieldset>

      <div className="mb-6">
        <label htmlFor="listing-image" className="block text-sm font-medium text-text-primary mb-2">
          {isService ? 'Service image' : 'Product image'}
        </label>
        {values.imageUrl ? (
          <div className="aspect-video bg-lavender rounded-2xl overflow-hidden mb-3">
            <img
              src={values.imageUrl}
              alt={values.name || 'Listing image'}
              className="w-full h-full object-cover"
            />
          </div>
        ) : (
          <div className="mb-3 flex items-start gap-3 rounded-2xl bg-lavender px-4 py-3">
            <Camera size={18} className="mt-0.5 shrink-0 text-primary" aria-hidden="true" />
            <p className="text-sm text-text-secondary">
              No photo yet. Pick one below — a listing with a photo sells better, but this one
              still works without one.
            </p>
          </div>
        )}
        <input
          id="listing-image"
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          onChange={(event) => {
            const file = event.target.files?.[0];
            // Cleared straight away so choosing the same photo twice still registers: a change
            // event needs the input's value to have moved.
            event.target.value = '';
            if (file) onPickImage(file);
          }}
          disabled={uploading}
          className="block w-full text-sm text-text-secondary file:mr-3 file:cursor-pointer file:rounded-full file:border-0 file:bg-lavender file:px-4 file:py-2 file:text-sm file:font-medium file:text-text-primary hover:file:bg-primary/10"
        />
        {uploading && (
          <p className="mt-1 text-sm text-text-secondary" role="status">
            Uploading photo…
          </p>
        )}
        {uploadError && (
          <p className="mt-1 text-sm text-error" role="alert">
            &bull; {uploadError}
          </p>
        )}
      </div>

      <Input
        label={isService ? 'Service name' : 'Product name'}
        placeholder={isService ? 'Enter the service you offer' : 'Enter a descriptive title'}
        {...field('name')}
      />

      <div className="w-full">
        <label className="block text-sm font-medium text-text-primary mb-1.5">
          {isService ? 'Service category' : 'Product category'}
        </label>
        {customCategory ? (
          <div className="flex flex-wrap items-center gap-3">
            <Input
              placeholder={isService ? 'Enter a service category' : 'Enter a product category'}
              className="flex-1 min-w-[12rem]"
              {...field('category')}
            />
            <button
              type="button"
              onClick={() => { onChange('category', ''); setCustomCategory(false); }}
              className="text-sm font-medium text-primary hover:underline"
            >
              Pick from the usual ones
            </button>
          </div>
        ) : (
          <div className="flex flex-wrap gap-2">
            {categoryOptions.map((category) => (
              <button
                key={category}
                type="button"
                onClick={() => onChange('category', category)}
                aria-pressed={values.category === category}
                className={`px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 active:scale-95 ${
                  values.category === category
                    ? 'bg-primary text-white shadow-md shadow-primary/20'
                    : 'bg-white border border-border text-text-primary hover:bg-lavender'
                }`}
              >
                {category}
              </button>
            ))}
            <button
              type="button"
              onClick={() => setCustomCategory(true)}
              className="px-4 py-2 rounded-full text-sm font-medium bg-white border border-border text-text-primary hover:bg-lavender transition-all duration-200 active:scale-95"
            >
              Other
            </button>
          </div>
        )}
        {errors.category && <p className="mt-1 text-sm text-error">&bull; {errors.category}</p>}
      </div>

      <Input
        label={isService ? 'Service price' : 'Product price'}
        type="number"
        placeholder="Enter price in Rand"
        {...field('price')}
      />

      <Input
        label={isService ? 'How many bookings are available' : 'How many are available'}
        type="number"
        placeholder="1"
        icon={Package}
        {...field('stock')}
      />

      {!isService && (
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">Condition</label>
          <div className="flex flex-wrap gap-2">
            {CONDITION_OPTIONS.map((condition) => (
              <button
                key={condition.value}
                type="button"
                onClick={() => onChange('condition', condition.value)}
                aria-pressed={values.condition === condition.value}
                className={`px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 active:scale-95 ${
                  values.condition === condition.value
                    ? 'bg-primary text-white shadow-md shadow-primary/20'
                    : 'bg-white border border-border text-text-primary hover:bg-lavender'
                }`}
              >
                {condition.label}
              </button>
            ))}
          </div>
        </div>
      )}

      <div className="w-full">
        <label className="block text-sm font-medium text-text-primary mb-1.5">
          {isService ? 'Service description' : 'Product description'}
        </label>
        <textarea
          name="description"
          rows={4}
          placeholder={isService
            ? 'Describe the service, what is included and any booking details'
            : 'Describe condition, dimensions, and any notes for buyers'}
          value={values.description}
          onChange={(event) => onChange(event.target.name, event.target.value)}
          className={`w-full px-4 py-3 bg-white border ${
            errors.description ? 'border-error' : 'border-border'
          } rounded-xl text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all duration-200 resize-none`}
        />
        {errors.description && <p className="mt-1 text-sm text-error">&bull; {errors.description}</p>}
      </div>

      <Input label="Location" placeholder="City, province" icon={MapPin} {...field('location')} />

      {showRetire && (
        <label className="flex items-start gap-3 rounded-2xl bg-lavender px-4 py-3 cursor-pointer">
          <input
            type="checkbox"
            name="active"
            checked={!values.active}
            onChange={(event) => onChange('active', !event.target.checked)}
            className="mt-1 h-4 w-4 accent-primary"
          />
          <span className="text-sm text-text-secondary">
            <span className="block font-medium text-text-primary">Retire this listing</span>
            A retired listing disappears from search and cannot be added to a cart. Existing orders
            are untouched, and un-ticking this brings it back.
          </span>
        </label>
      )}

      {submitError && (
        <div role="alert" className="rounded-2xl bg-red-50 border border-red-200 px-4 py-3">
          <p className="text-sm text-red-800">{submitError}</p>
        </div>
      )}

      <div className="pt-4">
        <Button type="submit" size="lg" disabled={submitting}>
          {submitting ? 'Saving…' : submitLabel}
        </Button>
      </div>
    </form>
  );
};

export default ListingEditor;