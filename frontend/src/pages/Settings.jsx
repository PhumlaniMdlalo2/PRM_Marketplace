import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertCircle, ChevronDown, Gavel, LogOut, MapPin, Shield, Store, User,
} from 'lucide-react';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import BackButton from '../components/ui/BackButton';
import Layout from '../components/layout/Layout';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../auth/useAuth';
import { changePassword } from '../api/auth';
import {
  getMyVendorProfile, createVendorProfile, updateVendorProfile,
  getMyPayoutDetails, updateMyPayoutDetails,
} from '../api/vendorProfile';
import { confirmPaymentReceipt, listSellerPayments } from '../api/payments';
import {
  createAddress, deleteAddress, listAddresses, setDefaultAddress, updateAddress,
} from '../api/addresses';
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

  // Vendor accounts get a profile during signup; students may apply later while keeping their
  // STUDENT role. Other account types cannot create seller profiles.
  if (role !== 'VENDOR' && role !== 'STUDENT') {
    return (
      <Panel>
        <p className="text-sm text-text-secondary">
          Selling on the marketplace is available to student and vendor accounts. This account is
          signed in as {role.toLowerCase()}.
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
        const updated = await updateVendorProfile(profile.id, values);
        setSuccess(updated.verified
          ? 'Seller profile updated.'
          : 'Seller details updated. Admin approval is required before you can publish listings.');
      } else {
        await createVendorProfile(values);
        setSuccess('Seller application submitted. Admin approval is required before you can list items.');
      }
      setForm(null);
      // Refetched rather than patched locally: verified and ratingAvg are the server's to set, and
      // a create in particular has to be re-read to get its id.
      await run();
    } catch (caught) {
      setError(caught.status === 400 && !profile
        ? 'The server rejected that. This account may only hold one seller profile.'
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
            : 'Pending admin approval. You cannot publish listings until your seller profile is approved.'}
        </p>
      )}
      <form onSubmit={submit} className="space-y-3" noValidate>
        <Input
          label="Business name"
          name="businessName"
          value={values.businessName}
          onChange={set('businessName')}
          maxLength={120}
        />
        <Input
          label="Registration number"
          name="registrationNo"
          placeholder="Optional"
          value={values.registrationNo}
          onChange={set('registrationNo')}
          maxLength={120}
        />
        <Feedback error={error} success={success} />
        <Button type="submit" disabled={busy}>
          {busy ? 'Saving…' : profile ? 'Save seller profile' : 'Apply to become a seller'}
        </Button>
      </form>
      {profile?.verified && (
        <>
          <PayoutDetailsPanel />
          <SellerPaymentsPanel />
        </>
      )}
    </Panel>
  );
};

const PayoutDetailsPanel = () => {
  const load = useCallback(() => getMyPayoutDetails(), []);
  const { data, loading, error: loadError, run } = useAsync(load);
  const [form, setForm] = useState(null);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [busy, setBusy] = useState(false);
  const values = form ?? {
    accountHolder: data?.accountHolder ?? '',
    bankName: data?.bankName ?? '',
    accountNumber: data?.accountNumber ?? '',
    branchCode: data?.branchCode ?? '',
    accountType: data?.accountType ?? 'CURRENT',
  };
  const set = (name) => (event) => setForm({ ...values, [name]: event.target.value });

  const submit = async (event) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    if (!/^[0-9]{6,20}$/.test(values.accountNumber.trim())) {
      setError('Enter an account number containing 6 to 20 digits.');
      return;
    }
    if (!/^[0-9]{4,10}$/.test(values.branchCode.trim())) {
      setError('Enter a branch code containing 4 to 10 digits.');
      return;
    }
    setBusy(true);
    try {
      await updateMyPayoutDetails(values);
      setForm(null);
      setSuccess('EFT payout details saved. They are only shared with buyers who choose EFT at checkout.');
      await run();
    } catch (caught) {
      setError(caught.message);
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <div className="h-24 animate-pulse rounded-xl bg-lavender" />;
  if (loadError && loadError.status !== 404) {
    return <Feedback error={loadError.message || 'Could not load payout details.'} />;
  }

  return (
    <div className="border-t border-border pt-4">
      <h3 className="font-semibold text-text-primary">EFT payout details</h3>
      <p className="mt-1 text-xs text-text-secondary">
        We check the account and branch code formats, but cannot verify that the account exists or belongs to you.
        Confirm the details with your bank. Buyers see them only for orders assigned to your shop. Never enter card numbers or security codes.
      </p>
      <form onSubmit={submit} className="mt-3 space-y-3" noValidate>
        <Input label="Account holder" name="accountHolder" value={values.accountHolder}
          onChange={set('accountHolder')} maxLength={120} />
        <Input label="Bank" name="bankName" value={values.bankName}
          onChange={set('bankName')} maxLength={120} />
        <Input label="Account number" name="accountNumber" inputMode="numeric"
          value={values.accountNumber} onChange={set('accountNumber')} maxLength={20} />
        <Input label="Branch code" name="branchCode" inputMode="numeric"
          value={values.branchCode} onChange={set('branchCode')} maxLength={10} />
        <label className="block text-sm font-medium text-text-primary">
          Account type
          <select
            name="accountType"
            value={values.accountType}
            onChange={set('accountType')}
            className="mt-1 block w-full rounded-xl border border-border bg-white px-3 py-2 text-text-primary"
          >
            <option value="CURRENT">Current</option>
            <option value="SAVINGS">Savings</option>
            <option value="TRANSMISSION">Transmission</option>
          </select>
        </label>
        <Feedback error={error} success={success} />
        <Button type="submit" disabled={busy}>
          {busy ? 'Saving…' : 'Save EFT details'}
        </Button>
      </form>
    </div>
  );
};

const SellerPaymentsPanel = () => {
  const load = useCallback(() => listSellerPayments(), []);
  const { data: payments, loading, error: loadError, run } = useAsync(load);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState(null);

  const confirm = async (payment) => {
    setBusyId(payment.paymentId);
    setError(null);
    try {
      await confirmPaymentReceipt(payment.paymentId);
      await run();
    } catch (caught) {
      setError(caught.message);
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="border-t border-border pt-4">
      <h3 className="font-semibold text-text-primary">Payments to confirm</h3>
      <p className="mt-1 text-xs text-text-secondary">
        Confirm an EFT only after funds clear in your bank account. Confirm cash only after you receive it at pickup.
      </p>
      {loading ? (
        <div className="mt-3 h-20 animate-pulse rounded-xl bg-lavender" />
      ) : loadError ? (
        <Feedback error={loadError.message || 'Could not load seller payments.'} />
      ) : !payments?.length ? (
        <p className="mt-3 text-sm text-text-secondary">No seller payments yet.</p>
      ) : (
        <ul className="mt-3 space-y-3">
          {payments.map((payment) => (
            <li key={payment.paymentId} className="rounded-xl border border-border p-3">
              <p className="text-sm font-medium text-text-primary">
                {payment.method === 'CASH_ON_PICKUP' ? 'Cash on pickup' : 'EFT'} · {payment.status}
              </p>
              <p className="mt-1 text-sm text-text-secondary">
                {payment.amount} | Order {payment.orderId}
              </p>
              <p className="text-xs text-text-muted">Reference: {payment.transactionReference}</p>
              {payment.status === 'PENDING' && (
                <Button
                  size="sm"
                  className="mt-3"
                  onClick={() => confirm(payment)}
                  disabled={Boolean(busyId)}
                >
                  {busyId === payment.paymentId
                    ? 'Confirming…'
                    : payment.method === 'CASH_ON_PICKUP'
                      ? 'Confirm cash received'
                      : 'Confirm cleared EFT'}
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}
      <Feedback error={error} />
    </div>
  );
};

const EMPTY_ADDRESS = {
  line1: '', line2: '', suburb: '', city: '', province: '', postalCode: '', country: 'South Africa',
};

const AddressesPanel = () => {
  const load = useCallback(() => listAddresses(), []);
  const { data: addresses, loading, error: loadError, run } = useAsync(load);

  const [form, setForm] = useState(EMPTY_ADDRESS);
  // The address being edited, or null when the form is adding a new one. Editing in place keeps the
  // list and the form in the same panel, which is where the user already is.
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const set = (name) => (event) => setForm((f) => ({ ...f, [name]: event.target.value }));

  const startEdit = (address) => {
    setEditingId(address.id);
    setError(null);
    setForm({
      line1: address.line1 ?? '',
      line2: address.line2 ?? '',
      suburb: address.suburb ?? '',
      city: address.city ?? '',
      province: address.province ?? '',
      postalCode: address.postalCode ?? '',
      // defaultAddress is carried through untouched. It is a primitive boolean on the entity, so
      // omitting it from the update body would arrive as false and unset the default as a side
      // effect of editing a street name.
      country: address.country ?? 'South Africa',
      defaultAddress: address.defaultAddress ?? false,
    });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setForm(EMPTY_ADDRESS);
    setError(null);
  };

  const submit = async (event) => {
    event.preventDefault();
    setError(null);
    if (!form.line1.trim()) return setError('Street address is required');
    if (!form.city.trim()) return setError('City is required');

    setBusy(true);
    try {
      if (editingId) {
        await updateAddress(editingId, form);
        cancelEdit();
      } else {
        await createAddress({ ...form, defaultAddress: false });
        setForm(EMPTY_ADDRESS);
      }
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
      // Editing a row that no longer exists would leave the form pointed at a deleted address.
      if (editingId === id) cancelEdit();
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
              <button
                type="button"
                onClick={() => startEdit(address)}
                aria-pressed={editingId === address.id}
                className={`text-xs font-medium hover:underline ${
                  editingId === address.id ? 'text-text-primary' : 'text-primary'
                }`}
              >
                {editingId === address.id ? 'Editing' : 'Edit'}
              </button>
              <button type="button" onClick={() => remove(address.id)} className="text-xs font-medium text-error hover:underline">
                Remove
              </button>
            </div>
          </li>
        ))}
      </ul>

      <form onSubmit={submit} className="space-y-3 pt-2 border-t border-border mt-2" noValidate>
        <p className="text-sm font-medium text-text-primary">
          {editingId ? 'Edit address' : 'Add an address'}
        </p>
        <Input label="Street address" name="line1" value={form.line1} onChange={set('line1')} />
        <Input label="Suburb" name="suburb" placeholder="Optional" value={form.suburb} onChange={set('suburb')} />
        <div className="grid grid-cols-2 gap-3">
          <Input label="City" name="city" value={form.city} onChange={set('city')} />
          <Input label="Province" name="province" value={form.province} onChange={set('province')} />
        </div>
        <Input label="Postal code" name="postalCode" value={form.postalCode} onChange={set('postalCode')} />
        <Feedback error={error} />
        <div className="flex gap-2">
          <Button type="submit" variant="secondary" disabled={busy}>
            {busy
              ? 'Saving…'
              : editingId ? 'Save address' : 'Add address'}
          </Button>
          {editingId && (
            <Button type="button" variant="ghost" onClick={cancelEdit}>
              Cancel
            </Button>
          )}
        </div>
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
                {STATUS_LABELS[report.status] ?? report.status}
                {report.resolutionNotes ? ` · ${report.resolutionNotes}` : ''}
              </span>
            </li>
          ))}
        </ul>
      )}

      <form onSubmit={submit} className="space-y-3 pt-2 border-t border-border mt-2" noValidate>
        <div>
          <label className="block text-sm font-medium text-text-primary mb-1.5">What is it about</label>
          <div className="flex flex-wrap gap-2">
            {TARGET_OPTIONS.map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={() => setForm((f) => ({ ...f, targetType: option.value }))}
                aria-pressed={form.targetType === option.value}
                className={`px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 active:scale-95 ${
                  form.targetType === option.value
                    ? 'bg-primary text-white shadow-md shadow-primary/20'
                    : 'bg-white border border-border text-text-primary hover:bg-lavender'
                }`}
              >
                {option.label}
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

/** What a report is about, in the same words the moderation queue uses for it. */
const TARGET_OPTIONS = [
  { value: 'PRODUCT', label: 'listing' },
  { value: 'USER', label: 'account' },
  { value: 'MESSAGE', label: 'message' },
  { value: 'BULLETIN_POST', label: 'post' },
  { value: 'COMMENT', label: 'comment' },
  { value: 'REVIEW', label: 'review' },
];

const STATUS_LABELS = {
  OPEN: 'Open',
  UNDER_REVIEW: 'Under review',
  RESOLVED: 'Resolved',
};

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
                hint="Anything you cannot report from its own page"
                expanded={openFor('report')}
                onToggle={() => toggle('report')}
              >
                <ReportProblemPanel />
              </Row>
              {/* The queue is a destination rather than a form, so it carries no chevron — a row
                  with children promises an expansion that would never come. */}
              {user?.role === 'ADMIN' && (
                <>
                  <Row
                    icon={Shield}
                    label="Admin dashboard"
                    hint="Marketplace reports, sellers and accounts"
                    onToggle={() => navigate('/admin')}
                  />
                  <Row
                    icon={Gavel}
                    label="Moderation"
                    hint="Reports waiting on a decision"
                    onToggle={() => navigate('/admin/moderation')}
                  />
                </>
              )}
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