import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Store } from 'lucide-react';
import Button from '../components/ui/Button';
import BackButton from '../components/ui/BackButton';
import ListingEditor from '../components/listing/ListingEditor';
import Layout from '../components/layout/Layout';
import { useAsync } from '../hooks/useAsync';
import { create } from '../api/products';
import { getMyVendorProfile } from '../api/vendorProfile';
import { useAuth } from '../auth/useAuth';
import { uploadImage } from '../api/uploads';
import { emptyListingForm, toListingPayload, validateListing } from '../lib/listingForm';

/**
 * Publishes a new listing.
 *
 * The seller profile check is not decoration: the server resolves the listing's vendor from the token
 * and rejects the create with a bodiless 400 when the account has no profile, which would otherwise
 * surface as a form that refuses to submit for no stated reason.
 */
const CreateListing = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [values, setValues] = useState(emptyListingForm);
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState(null);

  const loadSeller = useCallback(() => getMyVendorProfile(), []);
  const { data: sellerProfile, loading, error: sellerError } = useAsync(loadSeller);

  const handleChange = (name, value) => {
    setValues((current) => ({ ...current, [name]: value }));
    setErrors((current) => (current[name] ? { ...current, [name]: undefined } : current));
  };

  // The photo is stored before the listing exists, and its address rides in the values like any
  // other field. A failed upload never blocks publishing: the listing is valid without one, so the
  // message says what happened and the form carries on with an empty address.
  const handlePickImage = async (file) => {
    setUploading(true);
    setUploadError(null);
    try {
      handleChange('imageUrl', await uploadImage(file));
    } catch (error) {
      setUploadError(error.message || 'The photo could not be uploaded. The listing works without one.');
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
      const created = await create(toListingPayload(values));
      navigate(`/product/${created.id}`, { replace: true });
    } catch (error) {
      // The seller profile was checked on mount, so a 400 here is a rule the server factory enforces
      // and the client-side validation should already have caught. Kept as a fallback rather than
      // trusting the two lists to stay identical forever.
      setSubmitError(error.status === 400
        ? 'The server rejected those details. Check the name, price, category and quantity.'
        : error.message);
      setSubmitting(false);
    }
  };

  // Both vendor accounts and students with a seller profile can sell, but neither may publish until
  // admin approves the profile.
  const canApplyToSell = user?.role === 'VENDOR' || user?.role === 'STUDENT';
  const needsSellerProfile = sellerError?.status === 404 && canApplyToSell;
  const pendingApproval = sellerProfile && !sellerProfile.verified;
  const cannotSell = user?.role && !canApplyToSell;

  return (
    <Layout>
      <div className="app-container py-6 pb-24 max-w-2xl mx-auto">
        <div className="flex items-center gap-4 mb-6">
          <BackButton />
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Create listing</h1>
        </div>

        {cannotSell ? (
          <div className="rounded-2xl bg-lavender p-6 text-center">
            <Store size={28} className="mx-auto mb-3 text-primary" aria-hidden="true" />
            <h2 className="text-lg font-bold text-text-primary mb-1">Seller profile needed</h2>
            <p className="text-sm text-text-secondary">
              This account is signed in as {user.role.toLowerCase()}. Student and vendor accounts
              can apply to sell from Settings.
            </p>
          </div>
        ) : needsSellerProfile ? (
          <div className="rounded-2xl bg-lavender p-6 text-center">
            <Store size={28} className="mx-auto mb-3 text-primary" aria-hidden="true" />
            <h2 className="text-lg font-bold text-text-primary mb-1">Seller profile needed</h2>
            <p className="text-sm text-text-secondary mb-4">
              Apply for a seller profile in Settings. Admin approval is required before listings
              can be published.
            </p>
            <Button onClick={() => navigate('/settings')}>Open seller settings</Button>
          </div>
        ) : pendingApproval ? (
          <div className="rounded-2xl bg-lavender p-6 text-center">
            <Store size={28} className="mx-auto mb-3 text-primary" aria-hidden="true" />
            <h2 className="text-lg font-bold text-text-primary mb-1">Seller approval pending</h2>
            <p className="text-sm text-text-secondary">
              Admin must approve your seller profile before you can publish listings. You can
              update your seller details in Settings while you wait.
            </p>
            <Button className="mt-4" onClick={() => navigate('/settings')}>Open seller settings</Button>
          </div>
        ) : loading ? (
          // Nothing interactive until the profile answer arrives: an account without one cannot
          // publish anything, and showing the form first would flash it at somebody who then gets
          // told they cannot use it.
          <div className="h-64 animate-pulse rounded-2xl bg-lavender" role="status">
            <span className="sr-only">Checking seller profile</span>
          </div>
        ) : (
          <ListingEditor
            values={values}
            errors={errors}
            onChange={handleChange}
            onSubmit={handleSubmit}
            submitting={submitting}
            submitError={submitError}
            submitLabel="Create listing"
            onPickImage={handlePickImage}
            uploading={uploading}
            uploadError={uploadError}
          />
        )}
      </div>
    </Layout>
  );
};

export default CreateListing;