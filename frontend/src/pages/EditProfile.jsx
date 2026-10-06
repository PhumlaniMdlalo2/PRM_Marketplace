import { useState } from 'react';
import { Camera } from 'lucide-react';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import BackButton from '../components/ui/BackButton';
import Layout from '../components/layout/Layout';
import Avatar from '../components/ui/Avatar';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { updateMe } from '../api/users';

/**
 * Edits the caller's own profile against `PUT /api/users/me`.
 *
 * Only three fields are editable, and they are the three the endpoint accepts. The form this replaced
 * also collected a password, a date of birth and a country, none of which the server has anywhere to
 * put: sending them would have been a silent no-op, so a user could have filled the form in, pressed
 * save and watched none of it stick. Passwords have their own route, which is linked below rather than
 * duplicated here, because it re-hashes the value and revokes the sessions it issued.
 *
 * Email is shown but not editable. The server deliberately keeps it out of `UpdateProfileRequest`
 * because changing it invalidates the verified flag that the emailed code established, and there is no
 * confirm-the-new-address flow to replace it with. Offering the field would be offering a save that
 * cannot happen.
 */
const EditProfile = () => {
  const { user, applyUser } = useAuth();
  const [formData, setFormData] = useState({ name: '', phone: '', avatarUrl: '' });
  const [errors, setErrors] = useState({});
  const [status, setStatus] = useState(null);
  const [saving, setSaving] = useState(false);

  // Seeded from the session, which is the account as of the last sign-in. An account edited in
  // another tab would show its older values here until the page is revisited; that is the trade for
  // a form that is ready to type into on arrival rather than one that fills in a render later.
  const [seeded, setSeeded] = useState(false);
  if (!seeded && user) {
    setFormData({ name: user.name ?? '', phone: user.phone ?? '', avatarUrl: user.avatarUrl ?? '' });
    setSeeded(true);
  }

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((current) => ({ ...current, [name]: value }));
    setStatus(null);
    if (errors[name]) setErrors((current) => ({ ...current, [name]: undefined }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const next = {};
    const name = formData.name.trim();
    const avatarUrl = formData.avatarUrl.trim();
    if (!name) next.name = 'Name is required';
    else if (name.length > 120) next.name = 'Name must be at most 120 characters';
    if (formData.phone.trim().length > 32) next.phone = 'Phone number must be at most 32 characters';
    if (avatarUrl && !/^https?:\/\/.+/.test(avatarUrl)) {
      next.avatarUrl = 'Avatar URL must start with http:// or https://';
    }
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setSaving(true);
    setStatus(null);
    try {
      // Every field is sent, including the ones left blank: the server replaces phone and avatarUrl
      // outright, so omitting one would keep the old value rather than clear it.
      const saved = await updateMe({
        name,
        phone: formData.phone.trim(),
        avatarUrl,
      });
      // The header, nav and listings all read the name and avatar from the session, so the saved
      // account has to replace the stored copy or the page still shows the old details.
      applyUser(saved);
      setStatus({ ok: true, message: 'Profile saved' });
    } catch (error) {
      setStatus({ ok: false, message: error.message || 'Could not save your profile' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Layout showNav={false}>
      <div className="app-container py-6 max-w-xl mx-auto">
        <div className="flex items-center gap-4 mb-6">
          <BackButton />
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Edit profile</h1>
        </div>

        <div className="flex justify-center mb-8">
          <div className="relative">
            <Avatar src={formData.avatarUrl.trim() || undefined} alt={formData.name} size="xl" />
            {/* No camera button here any more. Avatars are still an address rather than an upload:
                the field is validated as an http(s) URL, so a file picker would have had nowhere to
                put its result. Listing photos do upload — see the listing form — but changing that
                for avatars would mean changing what this field means everywhere it is read. */}
            <span
              className="absolute bottom-0 right-0 w-9 h-9 bg-primary rounded-xl flex items-center justify-center text-white shadow-lg shadow-primary/30"
              aria-hidden="true"
            >
              <Camera size={16} />
            </span>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <Input
            label="Name"
            name="name"
            error={errors.name}
            value={formData.name}
            onChange={handleChange}
            autoComplete="name"
          />

          <Input
            label="Phone number"
            name="phone"
            type="tel"
            error={errors.phone}
            hint="Optional. Leave blank to remove it."
            value={formData.phone}
            onChange={handleChange}
            autoComplete="tel"
          />

          <Input
            label="Avatar URL"
            name="avatarUrl"
            type="url"
            error={errors.avatarUrl}
            hint="Optional link to a picture of you."
            placeholder="https://example.com/me.jpg"
            value={formData.avatarUrl}
            onChange={handleChange}
            autoComplete="photo"
          />

          <Input
            label="Email"
            name="email"
            value={user?.email ?? ''}
            onChange={() => {}}
            readOnly
            disabled
            hint="Your email cannot be changed here. Changing it needs a confirmation step, so it has a flow of its own."
          />

          {status && (
            <p
              role="status"
              className={`text-sm font-medium ${status.ok ? 'text-primary' : 'text-error'}`}
            >
              {status.message}
            </p>
          )}

          <div className="pt-4">
            <Button type="submit" size="lg" disabled={saving || !user}>
              {saving ? 'Saving...' : 'Save changes'}
            </Button>
          </div>
        </form>

        <p className="mt-6 text-xs text-text-secondary text-center">
          Changing your password is a separate step, because it re-hashes the value and signs out your
          other devices.{' '}
          <Link to="/settings" className="text-primary font-medium underline">
            Change password
          </Link>
        </p>
      </div>
    </Layout>
  );
};

export default EditProfile;