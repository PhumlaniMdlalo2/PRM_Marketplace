import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertCircle, ChevronDown, LogOut, MapPin, Shield, Store, User,
} from 'lucide-react';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import BackButton from '../components/ui/BackButton';
import Layout from '../components/layout/Layout';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../auth/useAuth';
import { changePassword } from '../api/auth';
import { getMyVendorProfile, createVendorProfile, updateVendorProfile } from '../api/vendorProfile';
import { createAddress, deleteAddress, listAddresses, setDefaultAddress } from '../api/addresses';
import { createReport, listMyReports } from '../api/reports';

/**
 * Settings.
 *
 * The rows here are the ones the server can actually do something about, each backed by a real
 * endpoint. Tapping a row expands it in place rather than navigating, because every destination is a
 * short form and adding a route per form would leave the back button pointing at nothing useful.
 *
 * What is not here: notification preferences, privacy choices, a subscription, help pages, terms and
 * data-saver settings. None of those have endpoints, and a row that navigates nowhere is worse than
 * no row at all.
 */

/** One expandable settings row. The button is the whole target, not just the label. */
const Row = ({ icon: Icon, label, hint, expanded, onToggle, danger, children }) => (
  <div className={danger ? 'text-error' : 'text-text-primary'}>
    <button
      type="button"
      onClick={onToggle}
      aria-expanded={Boolean(expanded)}
      className={`w-full flex items-center gap-3.5 px-4 py-3.5 hover:bg-lavender/50 active:bg-lavender transition-colors text-left ${
        danger ? 'text-error' : ''
      }`}
    >
      <span className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${
        danger ? 'bg-error/10' : 'bg-lavender'
      }`}>
        <Icon size={17} className={danger ? 'text-error' : 'text-primary'} />
      </span>
      <span className="flex-1 min-w-0">
        <span className="block font-medium text-[15px]">{label}</span>
        {hint && <span className="block text-xs text-text-muted mt-0.5">{hint}</span>}
      </span>
      {/* Only rows that expand carry the chevron: on a row that navigates, it would promise a
          disclosure that never happens. */}
      {children && (
        <ChevronDown
          size={17}
          className={`text-text-muted shrink-0 transition-transform duration-200 ${
            expanded ? 'rotate-180' : ''
          }`}
        />
      )}
    </button>
    {expanded && children && <div className="px-4 pb-4">{children}</div>}
  </div>
);

/** Success or failure of the panel above it, so the two are never confused for each other. */
const Feedback = ({ error, success }) => {
  if (!error && !success) return null;
  return (
    <p
      role={error ? 'alert' : 'status'}
      className={`mt-3 text-sm ${error ? 'text-red-800' : 'text-green-800'}`}
    >
      {error || success}
    </p>
  );
};

const Panel = ({ children }) => <div className="pt-1 space-y-3">{children}</div>;

const ChangePasswordPanel = () => {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [busy, setBusy] = useState(false);

  const update = (name) => (event) => setForm((f) => ({ ...f, [name]: event.target.value }));

  const submit = async (event) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    if (!form.currentPassword) return setError('Enter your current password');
    if (!form.newPassword) return setError('Enter a new password');
    if (form.newPassword !== form.confirmPassword) {
      return setError('The two new passwords do not match');
    }
    if (form.newPassword === form.currentPassword) {
      return setError('The new password is the same as the current one');
    }

    setBusy(true);
    try {
      await changePassword(form.currentPassword, form.newPassword);
      setSuccess('Password updated.');
      setForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (caught) {
      setError(caught.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Panel>
      <form onSubmit={submit} className="space-y-3" noValidate>
        <Input
          label="Current password"
          name="currentPassword"
          type="password"
          value={form.currentPassword}
          onChange={update('currentPassword')}
        />
        <Input
          label="New password"
          name="newPassword"
          type="password"
          value={form.newPassword}
          onChange={update('newPassword')}
        />
        <Input
          label="Confirm new password"
          name="confirmPassword"
          type="password"
          value={form.confirmPassword}
          onChange={update('confirmPassword')}
        />
        <Feedback error={error} success={success} />
        <Button type="submit" disabled={busy}>{busy ? 'Updating…' : 'Update password'}</Button>
      </form>
    </Panel>
  );
};

const SellerProfilePanel = ({ role }) => {
  const loadProfile = useCallback(() => getMyVendorProfile(), []);
  const { data: profile, loading, error: loadError, run } = useAsync(loadProfile);

  const [form, setForm] = useState(null);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [busy, setBusy] = useState(false);

  // Only a VENDOR account may hold a seller profile, so for anyone else there is nothing to show and
  // nothing to try: the server answers 400 for every attempt.
  if (role !== 'VENDOR') {
    return (
      <Panel>
        <p className="text-sm text-text-secondary">
          Selling on the marketplace needs a seller profile, which is only available on a vendor
          account. This account is signed in as {role.toLowerCase()}.
        </p>
      </Panel>
    );
  }

  if (loading) return <Panel><div className="h-24 animate-pulse rounded-xl bg-lavender" /></Panel>;

  // A 404 here means "no profile yet", which is the create form rather than a failure.
  const missing = loadError?.status === 404;
  if (!missing && !profile) {
    return <Panel><Feedback error={loadError?.message || 'Could not load your seller profile.'} /></Panel>;
  }

  const values = form ?? {
    businessName: profile?.businessName ?? '',
    registrationNo: profile?.registrationNo ?? '',
  };
  const set = (name) => (event) => setForm({ ...values, [name]: event.target.value });

  const submit = async (event) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    if (!values.businessName.trim()) return setError('Business name is required');

    setBusy(true);
    try {
      if (profile) {
        await updateVendorProfile(profile.id, values);
        setSuccess('Seller profile updated.');
      } else {
        await createVendorProfile(values);
        setSuccess('Seller profile created. You can list items now.');
      }
      setForm(null);
      // Refetched rather than patched locally: verified and ratingAvg are the server's to set, and
      // a create in particular has to be re-read to get its id.
      await run();
    } catch (caught) {
      setError(caught.status === 400 && !profile
        ? 'The server rejected that. A vendor account may only hold one seller profile.'
        : caught.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Panel>
      {profile && (
        <p className="text-sm text-text-secondary">
          {profile.verified
            ? 'This seller profile is verified.'
            : 'Not verified yet. Verified sellers show a badge on their listings.'}
        </p>
      )}
      <form onSubmit={submit} className="space-y-3" noValidate>
        <Input
          label="Business name"
          name="businessName"
          value={values.businessName}
          onChange={set('businessName')}
        />
        <Input
          label="Registration number"
          name="registrationNo"
          placeholder="Optional"
          value={values.registrationNo}
          onChange={set('registrationNo')}
        />
        <Feedback error={error} success={success} />
        <Button type="submit" disabled={busy}>
          {busy ? 'Saving…' : profile ? 'Save seller profile' : 'Create seller profile'}
        </Button>
      </form>
    </Panel>
  );
};

const AddressesPanel = () => {
  const load = useCallback(() => listAddresses(), []);
  const { data: addresses, loading, error: loadError, run } = useAsync(load);

  const [form, setForm] = useState({
    line1: '', line2: '', suburb: '', city: '', province: '', postalCode: '', country: 'South Africa',
  });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const set = (name) => (event) => setForm((f) => ({ ...f, [name]: event.target.value }));

  const submit = async (event) => {
    event.preventDefault();
    setError(null);
    if (!form.line1.trim()) return setError('Street address is required');
    if (!form.city.trim()) return setError('City is required');

    setBusy(true);
    try {
      await createAddress({ ...form, defaultAddress: false });
      setForm({
        line1: '', line2: '', suburb: '', city: '', province: '', postalCode: '', country: 'South Africa',
      });
      await run();
    } catch (caught) {
      setError(caught.message);
    } finally {
      setBusy(false);
    }
  };

  const makeDefault = async (id) => {
    setError(null);
    try {
      await setDefaultAddress(id);
      await run();
    } catch (caught) {
      setError(caught.message);
    }
  };

  const remove = async (id) => {
    setError(null);
    try {
      await deleteAddress(id);
      await run();
    } catch (caught) {
      setError(caught.message);
    }
  };

  if (loading) return <Panel><div className="h-24 animate-pulse rounded-xl bg-lavender" /></Panel>;

  return (
    <Panel>
      {loadError && <Feedback error={loadError.message} />}
      {addresses?.length === 0 && (
        <p className="text-sm text-text-secondary">No addresses saved yet.</p>
      )}
      <ul className="space-y-2">
        {addresses?.map((address) => (
          <li key={address.id} className="rounded-xl bg-lavender px-3 py-2.5">
            <p className="text-sm text-text-primary">
              {address.singleLine || address.line1}
              {address.defaultAddress && (
                <span className="ml-2 text-xs font-medium text-primary">Default</span>
              )}
            </p>
            <div className="mt-1.5 flex gap-3">
              {!address.defaultAddress && (
                <button type="button" onClick={() => makeDefault(address.id)} className="text-xs font-medium text-primary hover:underline">
                  Make default
                </button>
              )}
              <button type="button" onClick={() => remove(address.id)} className="text-xs font-medium text-error hover:underline">
                Remove
              </button>
            </div>
          </li>
        ))}
      </ul>

      <form onSubmit={submit} className="space-y-3 pt-2 border-t border-border mt-2" noValidate>
        <p className="text-sm font-medium text-text-primary">Add an address</p>
        <Input label="Street address" name="line1" value={form.line1} onChange={set('line1')} />
        <Input label="Suburb" name="suburb" placeholder="Optional" value={form.suburb} onChange={set('suburb')} />
        <div className="grid grid-cols-2 gap-3">
          <Input label="City" name="city" value={form.city} onChange={set('city')} />
          <Input label="Province" name="province" value={form.province} onChange={set('province')} />
        </div>
        <Input label="Postal code" name="postalCode" value={form.postalCode} onChange={set('postalCode')} />
        <Feedback error={error} />
        <Button type="submit" variant="secondary" disabled={busy}>
          {busy ? 'Adding…' : 'Add address'}
        </Button>
      </form>
    </Panel>
  );
};

const ReportProblemPanel = () => {
  const load = useCallback(() => listMyReports(), []);
  const { data: reports, loading, run } = useAsync(load);

  const [form, setForm] = useState({ targetType: 'PRODUCT', targetId: '', reason: '' });
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [busy, setBusy] = useState(false);

  const set = (name) => (event) => setForm((f) => ({ ...f, [name]: event.target.value }));

  const submit = async (event) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    if (!form.targetId.trim()) return setError('What is the id of the item or person?');
    if (!form.reason.trim()) return setError('Say what went wrong');

    setBusy(true);
    try {
      await createReport({
        targetType: form.targetType,
        targetId: form.targetId.trim(),
        reason: form.reason.trim(),
      });
      setForm({ targetType: form.targetType, targetId: '', reason: '' });
      setSuccess('Report filed. It is filed under your account, not somebody else\'s.');
      await run();
    } catch (caught) {
      setError(caught.status === 400
        ? 'The server would not accept that report. Check the id and try again.'
        : caught.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Panel>
      {loading && <div className="h-16 animate-pulse rounded-xl bg-lavender" />}
      {reports?.length > 0 && (
        <ul className="space-y-2">
          {reports.map((report) => (
            <li key={report.id} className="rounded-xl bg-lavender px-3 py-2.5 text-sm">
              <span className="font-medium text-text-primary">{report.targetType}</span>
              <span className="text-text-muted"> · {report.reason}</span>
              <span className="block text-xs text-text-muted mt-0.5">
                {report.resolved ? 'Resolved' : `Status: ${report.status}`}
              </span>
            </li>
          ))}
        </ul>
      )}

      <form onSubmit={submit} className="space-y-3 pt-2 border-t border-border mt-2" noValidate>
        <div>
          <label className="block text-sm font-medium text-text-primary mb-1.5">What is it about</label>
          <div className="flex flex-wrap gap-2">
            {['PRODUCT', 'USER', 'MESSAGE', 'OTHER'].map((option) => (
              <button
                key={option}
                type="button"
                onClick={() => setForm((f) => ({ ...f, targetType: option }))}
                aria-pressed={form.targetType === option}
                className={`px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 active:scale-95 ${
                  form.targetType === option
                    ? 'bg-primary text-white shadow-md shadow-primary/20'
                    : 'bg-white border border-border text-text-primary hover:bg-lavender'
                }`}
              >
                {option.toLowerCase()}
              </button>
            ))}
          </div>
        </div>
        <Input
          label="Id"
          name="targetId"
          placeholder="Paste the id from the page URL"
          value={form.targetId}
          onChange={set('targetId')}
        />
        <div className="w-full">
          <label className="block text-sm font-medium text-text-primary mb-1.5">What went wrong</label>
          <textarea
            name="reason"
            rows={3}
            placeholder="Describe the problem"
            value={form.reason}
            onChange={set('reason')}
            className="w-full px-4 py-3 bg-white border border-border rounded-xl text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all duration-200 resize-none"
          />
        </div>
        <Feedback error={error} success={success} />
        <Button type="submit" variant="secondary" disabled={busy}>
          {busy ? 'Filing…' : 'File report'}
        </Button>
      </form>
    </Panel>
  );
};

const section = 'bg-white border border-border rounded-2xl overflow-hidden divide-y divide-border';

const Settings = () => {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(null);

  const toggle = (key) => setOpen((current) => (current === key ? null : key));
  const openFor = (key) => open === key;

  return (
    <Layout>
      <div className="app-container py-6 pb-24 max-w-2xl mx-auto">
        <div className="flex items-center gap-4 mb-6">
          <BackButton />
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Settings</h1>
        </div>

        <div className="space-y-6">
          <section>
            <h2 className="text-xs font-semibold uppercase tracking-wider text-text-muted mb-2">
              Account
            </h2>
            <div className={section}>
              <Row
                icon={User}
                label="Edit profile"
                hint={user?.email}
                onToggle={() => navigate('/profile/edit')}
              />
              <Row
                icon={Shield}
                label="Change password"
                expanded={openFor('password')}
                onToggle={() => toggle('password')}
              >
                <ChangePasswordPanel />
              </Row>
              <Row
                icon={Store}
                label="Seller profile"
                hint="What your listings are published under"
                expanded={openFor('seller')}
                onToggle={() => toggle('seller')}
              >
                <SellerProfilePanel role={user?.role} />
              </Row>
              <Row
                icon={MapPin}
                label="Delivery addresses"
                expanded={openFor('addresses')}
                onToggle={() => toggle('addresses')}
              >
                <AddressesPanel />
              </Row>
            </div>
          </section>

          <section>
            <h2 className="text-xs font-semibold uppercase tracking-wider text-text-muted mb-2">
              Support
            </h2>
            <div className={section}>
              <Row
                icon={AlertCircle}
                label="Report a problem"
                expanded={openFor('report')}
                onToggle={() => toggle('report')}
              >
                <ReportProblemPanel />
              </Row>
            </div>
          </section>

          <section>
            <h2 className="text-xs font-semibold uppercase tracking-wider text-text-muted mb-2">
              Session
            </h2>
            <div className={section}>
              <Row icon={LogOut} label="Log out" danger onToggle={() => signOut()} />
            </div>
          </section>
        </div>
      </div>
    </Layout>
  );
};

export default Settings;