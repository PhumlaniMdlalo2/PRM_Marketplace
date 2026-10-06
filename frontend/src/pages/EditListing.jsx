import { useCallback, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { PackageX } from 'lucide-react';
import Button from '../components/ui/Button';
import BackButton from '../components/ui/BackButton';
import ListingEditor from '../components/listing/ListingEditor';
import Layout from '../components/layout/Layout';
import { useAsync } from '../hooks/useAsync';
import { getById, reactivate, retire, update } from '../api/products';
import { uploadImage } from '../api/uploads';
import { listingFromProduct, toListingPayload, validateListing } from '../lib/listingForm';

/**
 * The form half of the edit page, split out so it can take the product's values as initial state.
 *
 * Mounted with a key of the product id, so a different listing gets a fresh set of values from
 * scratch. That is deliberate: copying them into state from an effect after the fetch resolves is the
 * pattern that leaves one render showing the previous listing's data.
 */
const EditListingForm = ({ product }) => {
  const navigate = useNavigate();
  const [values, setValues] = useState(() => listingFromProduct(product));
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState(null);

  const handleChange = (name, value) => {
    setValues((current) => ({ ...current, [name]: value }));
    setErrors((current) => (current[name] ? { ...current, [name]: undefined } : current));
  };

  // Replacing the photo is done by uploading first and saving second, so the address in the values
  // is always the one this listing will carry. A failed upload leaves the stored photo alone and
  // never blocks saving the rest of the form.
  const handlePickImage = async (file) => {
    setUploading(true);
    setUploadError(null);
    try {
      handleChange('imageUrl', await uploadImage(file));
    } catch (error) {
      setUploadError(error.message || 'The photo could not be uploaded. The listing keeps its current one.');
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const nextErrors = validateListing(values);
    setErrors(nextErrors);
    setSubmitError(null);
    if (Object.keys(nextErrors).length > 0) return;

    setSubmitting(true);
    try {
      // imageUrl is in the values like every other field: it starts as whatever the listing
      // already has and becomes the new address when a photo is uploaded. It is always sent,
      // because the server replaces the field with whatever non-null value it is given, so
      // leaving it out of the body would wipe the stored address.
      await update(product.id, toListingPayload(values));

      // Retirement is a separate call because the server ignores active on update. Only made when
      // the box actually moved, so saving an unrelated typo does not fire a second request.
      if (values.active !== product.active) {
        if (values.active) await reactivate(product.id);
        else await retire(product.id);
      }

      navigate(`/product/${product.id}`, { replace: true });
    } catch (error) {
      setSubmitError(error.status === 404
        ? 'This listing is no longer yours to edit. It may have been removed, or the link may be wrong.'
        : error.message);
      setSubmitting(false);
    }
  };

  return (
    <ListingEditor
      values={values}
      errors={errors}
      onChange={handleChange}
      onSubmit={handleSubmit}
      submitting={submitting}
      submitError={submitError}
      submitLabel="Save changes"
      onPickImage={handlePickImage}
      uploading={uploading}
      uploadError={uploadError}
      showRetire
    />
  );
};

const EditListing = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const loadProduct = useCallback(() => getById(id), [id]);
  const { data: product, loading, error } = useAsync(loadProduct);

  // The update route answers 404 both for a listing that does not exist and for one belonging to
  // another vendor, on purpose, so this page cannot confirm whether a given listing exists. The
  // message says so rather than picking one of the two.
  if (!loading && (error || !product)) {
    return (
      <Layout>
        <div className="app-container py-6 pb-24 max-w-2xl mx-auto">
          <div className="flex items-center gap-4 mb-6">
            <BackButton />
            <h1 className="text-2xl font-bold text-text-primary tracking-tight">Edit listing</h1>
          </div>
          <div className="rounded-2xl bg-lavender p-6 text-center">
            <PackageX size={28} className="mx-auto mb-3 text-primary" aria-hidden="true" />
            <h2 className="text-lg font-bold text-text-primary mb-1">Listing unavailable</h2>
            <p className="text-sm text-text-secondary mb-4">
              {error?.status === 404
                ? 'It does not exist, or it belongs to another seller.'
                : error?.message || 'It could not be loaded.'}
            </p>
            <Button variant="secondary" onClick={() => navigate(-1)}>Go back</Button>
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="app-container py-6 pb-24 max-w-2xl mx-auto">
        <div className="flex items-center gap-4 mb-2">
          <BackButton />
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Edit listing</h1>
        </div>
        <p className="text-text-secondary mb-6 ml-12">Update your listing details</p>

        {loading || !product ? (
          <div className="h-96 animate-pulse rounded-2xl bg-lavender" role="status">
            <span className="sr-only">Loading listing</span>
          </div>
        ) : (
          <EditListingForm key={product.id} product={product} />
        )}
      </div>
    </Layout>
  );
};

export default EditListing;