import { useCallback } from 'react';
import {
  Settings,
  ChevronRight,
  Package,
  Heart,
  MessageCircle,
  Plus,
  Store,
} from 'lucide-react';
import Avatar from '../components/ui/Avatar';
import Layout from '../components/layout/Layout';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { useAsync } from '../hooks/useAsync';
import { getMe } from '../api/users';
// The seller-profile check is a 404 rather than an error for anyone without one, so it stays a
// module-level import: a missing profile is a normal state this page reports, not a failure.
import { getMyVendorProfile } from '../api/vendorProfile';
import { listOrders } from '../api/orders';
import { countSavedItems } from '../api/savedItems';
import { unreadCount } from '../api/messages';

/**
 * The caller's own account, from `GET /api/users/me`.
 *
 * The three counts are fetched too. This page used to show a fixed 3 orders, 12 saved items and 5
 * messages, which is worse than showing nothing: the numbers looked like the user's own and there was
 * no way for them to be anything but wrong. They come from the endpoint that answers each count
 * directly, and `allSettled` is what keeps one failure local — a count that did not load is left blank
 * instead of being reported as zero, which would claim the account is empty when in fact nothing was
 * heard back.
 */

const ROLE_LABEL = {
  STUDENT: 'Student',
  VENDOR: 'Vendor',
  FACULTY: 'Faculty',
};

/** Reads a settled promise's value, or null when it rejected. */
const value = (settled) => (settled.status === 'fulfilled' ? settled.value : null);

const Profile = () => {
  const { user: sessionUser } = useAuth();

  const load = useCallback(async () => {
    const me = await getMe();

    const [orders, saved, unread, seller] = await Promise.allSettled([
      listOrders().then((list) => list.length),
      countSavedItems(),
      unreadCount(),
      // Only a vendor can hold one, so asking on any other role would just be a guaranteed 404.
      me.role === 'VENDOR' ? getMyVendorProfile().then(() => true) : Promise.resolve(false),
    ]);

    return {
      me,
      orders: value(orders),
      saved: value(saved),
      unread: value(unread),
      hasSellerProfile: seller.status === 'fulfilled' ? seller.value : null,
    };
  }, []);

  const { data, loading, error } = useAsync(load);

  // The session copy stands in until /me answers, so the name is on screen from the first paint
  // rather than after a round trip.
  const profile = data?.me ?? sessionUser;
  const roleLabel = ROLE_LABEL[profile?.role] ?? profile?.role ?? '';
  const isVendor = profile?.role === 'VENDOR';

  const menuItems = [
    { icon: Package, label: 'My orders', path: '/orders', count: data?.orders },
    { icon: Heart, label: 'Saved items', path: '/saved', count: data?.saved },
    { icon: MessageCircle, label: 'Messages', path: '/messages', count: data?.unread },
    // Only a vendor account has listings to see, so this row is not rendered for anyone else.
    // Before this the page offered no route to them at all: a seller could create a listing and
    // then had no way back to it.
    ...(isVendor ? [{ icon: Store, label: 'My listings', path: '/listing/mine' }] : []),
  ];

  const badge = (text, primary = false) => (
    <span
      className={`inline-block mt-1.5 mr-1.5 text-xs font-semibold bg-white/70 px-2 py-0.5 rounded-md ${
        primary ? 'text-primary' : 'text-text-secondary'
      }`}
    >
      {text}
    </span>
  );

  return (
    <Layout>
      <div className="app-container py-6 max-w-2xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Profile</h1>
          <Link
            to="/settings"
            className="p-2 rounded-full text-text-secondary hover:text-text-primary hover:bg-lavender transition-colors"
            aria-label="Settings"
          >
            <Settings size={22} />
          </Link>
        </div>

        {error && (
          <p role="alert" className="mb-4 text-sm text-error">
            Could not load your profile: {error.message}
          </p>
        )}

        <div className="flex items-center gap-4 p-4 bg-lavender/70 rounded-3xl">
          <Avatar src={profile?.avatarUrl || undefined} alt={profile?.name ?? 'Account'} size="lg" />
          <div className="min-w-0">
            <h2 className="text-lg font-bold text-text-primary truncate">
              {profile?.name ?? 'Your account'}
            </h2>
            <p className="text-sm text-text-secondary truncate">{profile?.email}</p>
            {roleLabel && badge(roleLabel, true)}
            {data?.hasSellerProfile === true && badge('Seller profile set up')}
          </div>
        </div>

        <div className="mt-6 space-y-2.5">
          {menuItems.map((item) => (
            <Link
              key={item.label}
              to={item.path}
              className="flex items-center gap-4 p-4 bg-white border border-border rounded-2xl hover:border-lavender-dark hover:bg-lavender/40 active:scale-[0.99] transition-all duration-200"
            >
              <span className="w-9 h-9 rounded-xl bg-lavender flex items-center justify-center flex-shrink-0">
                <item.icon size={18} className="text-primary" />
              </span>
              <span className="flex-1 font-medium text-text-primary">{item.label}</span>
              {item.count !== null && item.count !== undefined && (
                <span className="text-xs font-semibold bg-primary-muted text-primary px-2 py-0.5 rounded-md">
                  {item.count}
                </span>
              )}
              <ChevronRight size={18} className="text-text-muted" />
            </Link>
          ))}
        </div>

        {/* Selling needs a vendor account, so the shortcut is only offered to one. A student pressing
            it used to land on a page telling them a seller profile was needed, which is not a thing
            they can have. */}
        {isVendor && (
          <Link
            to="/listing/create"
            className="mt-6 flex items-center justify-center gap-2 py-3.5 bg-primary text-white font-semibold rounded-2xl shadow-md shadow-primary/20 hover:bg-primary-hover active:scale-[0.98] transition-all duration-200"
          >
            <Plus size={20} />
            Sell an item
          </Link>
        )}

        {isVendor && data && !data.hasSellerProfile && (
          <p className="mt-3 text-xs text-text-secondary text-center">
            <Store size={12} className="inline mr-1 -mt-0.5" aria-hidden="true" />
            You need a seller profile before you can list anything.{' '}
            <Link to="/settings" className="text-primary font-medium underline">
              Set one up
            </Link>
          </p>
        )}

        <Link
          to="/profile/edit"
          className={`${isVendor ? 'mt-3' : 'mt-6'} block w-full text-center py-3.5 bg-lavender text-text-primary font-medium rounded-2xl hover:bg-lavender-dark active:scale-[0.98] transition-all duration-200`}
        >
          Edit profile
        </Link>

        {loading && (
          <p className="sr-only" role="status">
            Loading your profile
          </p>
        )}
      </div>
    </Layout>
  );
};

export default Profile;