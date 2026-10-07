import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowUpRight, Check, CircleAlert, Plus, Settings2, Store } from 'lucide-react';
import Layout from '../components/layout/Layout';
import Button from '../components/ui/Button';
import { listMine } from '../api/products';
import { confirmPaymentReceipt, listSellerPayments } from '../api/payments';
import { listSellerOrders, updateOrderStatus } from '../api/orders';
import { useAsync } from '../hooks/useAsync';
import { formatPrice } from '../lib/format';

const SellerHome = ({ user, profile, profileLoading, profileError, onRetry }) => {
  const canManageShop = Boolean(profile?.verified);
  const loadListings = useCallback(() => listMine(), []);
  const loadPayments = useCallback(() => listSellerPayments(), []);
  const loadOrders = useCallback(() => listSellerOrders(), []);
  const {
    data: listingsData,
    loading: listingsLoading,
    error: listingsError,
  } = useAsync(loadListings, { immediate: canManageShop });
  const {
    data: paymentsData,
    loading: paymentsLoading,
    error: paymentsError,
    run: reloadPayments,
  } = useAsync(loadPayments, { immediate: canManageShop });
  const {
    data: ordersData,
    loading: ordersLoading,
    error: ordersError,
    run: reloadOrders,
  } = useAsync(loadOrders, { immediate: canManageShop });
  const [busyPaymentId, setBusyPaymentId] = useState(null);
  const [busyOrderId, setBusyOrderId] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [actionMessage, setActionMessage] = useState(null);

  const listings = listingsData ?? [];
  const activeListings = listings.filter((listing) => listing.active);
  const pendingPayments = (paymentsData ?? []).filter((payment) => payment.status === 'PENDING');
  const orders = ordersData ?? [];

  const confirmReceipt = async (payment) => {
    setBusyPaymentId(payment.paymentId);
    setActionError(null);
    setActionMessage(null);
    try {
      await confirmPaymentReceipt(payment.paymentId);
      setActionMessage('Payment marked as received.');
      await reloadPayments();
    } catch (error) {
      setActionError(error.message || 'Could not confirm this payment.');
    } finally {
      setBusyPaymentId(null);
    }
  };

  const advanceOrder = async (order) => {
    const nextStatus = {
      PENDING: 'CONFIRMED',
      CONFIRMED: 'SHIPPED',
      SHIPPED: 'DELIVERED',
    }[order.status];
    if (!nextStatus || busyOrderId) return;

    setBusyOrderId(order.id);
    setActionError(null);
    setActionMessage(null);
    try {
      await updateOrderStatus(order.id, nextStatus);
      setActionMessage('Order progress updated. The buyer has been emailed.');
      await reloadOrders();
    } catch (error) {
      setActionError(error.status === 404
        ? 'Could not update the order. Confirm all payment first, then try again.'
        : error.message || 'Could not update this order.');
    } finally {
      setBusyOrderId(null);
    }
  };

  const storeName = profile?.businessName || user?.name || 'Your shop';

  return (
    <Layout>
      <div className="app-container max-w-6xl py-7">
        <section className="rounded-3xl bg-lavender px-6 py-7 sm:px-9 sm:py-9">
          <div className="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
            <div className="max-w-2xl">
              <p className="text-sm font-semibold text-primary">Seller workspace</p>
              <h1 className="mt-2 text-3xl font-bold tracking-tight text-text-primary sm:text-4xl">
                {profile?.verified ? `Good to see you, ${storeName}` : 'Set up your shop'}
              </h1>
              <p className="mt-3 max-w-xl text-sm leading-6 text-text-secondary">
                {profile?.verified
                  ? 'Manage your listings and follow up on payments from one place.'
                  : 'Your buyer catalogue stays available while you complete seller setup.'}
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              {profile?.verified ? (
                <>
                  <Link
                    to="/listing/create"
                    className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-primary-hover active:scale-[0.98] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                  >
                    <Plus size={17} aria-hidden="true" />
                    Add a listing
                  </Link>
                  <Link
                    to="/listing/mine"
                    className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-primary/20 bg-white px-4 py-2.5 text-sm font-semibold text-text-primary transition hover:bg-white/70 active:scale-[0.98] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                  >
                    Manage listings
                  </Link>
                </>
              ) : (
                <Link
                  to="/settings"
                  className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-primary-hover active:scale-[0.98] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                >
                  <Settings2 size={17} aria-hidden="true" />
                  Seller settings
                </Link>
              )}
            </div>
          </div>
        </section>

        {profileLoading ? (
          <div className="mt-6 h-28 animate-pulse rounded-2xl bg-lavender" role="status">
            <span className="sr-only">Loading seller profile</span>
          </div>
        ) : profileError && profileError.status !== 404 ? (
          <div className="mt-6 flex flex-col gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 sm:flex-row sm:items-center sm:justify-between">
            <p role="alert" className="text-sm text-red-800">
              {profileError.message || 'Could not load your seller profile.'}
            </p>
            <Button variant="secondary" size="sm" onClick={onRetry}>Try again</Button>
          </div>
        ) : !profile?.verified ? (
          <div className="mt-6 flex gap-3 rounded-2xl border border-border bg-white p-5">
            <CircleAlert className="mt-0.5 shrink-0 text-primary" size={20} aria-hidden="true" />
            <div>
              <h2 className="font-semibold text-text-primary">
                {profile ? 'Your seller application is being reviewed' : 'Create your seller profile'}
              </h2>
              <p className="mt-1 text-sm leading-6 text-text-secondary">
                {profile
                  ? 'Admin approval is required before you can publish listings. You can update your shop details in Settings.'
                  : 'Add your shop details in Settings. Admin approval is required before you can publish listings.'}
              </p>
              <Link to="/settings" className="mt-3 inline-flex items-center gap-1 text-sm font-semibold text-primary hover:underline">
                Open seller settings <ArrowUpRight size={15} aria-hidden="true" />
              </Link>
            </div>
          </div>
        ) : (
          <>
            <section className="mt-7 grid grid-cols-1 gap-4 sm:grid-cols-2" aria-label="Shop overview">
              <article className="rounded-2xl border border-border bg-white p-5">
                <p className="text-sm text-text-secondary">Active listings</p>
                <p className="mt-2 text-3xl font-semibold tabular-nums text-text-primary">
                  {listingsLoading ? '...' : activeListings.length}
                </p>
                <Link to="/listing/mine" className="mt-3 inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline">
                  View your listings <ArrowUpRight size={15} aria-hidden="true" />
                </Link>
              </article>
              <article className="rounded-2xl border border-border bg-white p-5">
                <p className="text-sm text-text-secondary">Payments to confirm</p>
                <p className="mt-2 text-3xl font-semibold tabular-nums text-text-primary">
                  {paymentsLoading ? '...' : pendingPayments.length}
                </p>
                <Link to="/settings" className="mt-3 inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline">
                  Payment settings <ArrowUpRight size={15} aria-hidden="true" />
                </Link>
              </article>
            </section>

            <section className="mt-9" aria-labelledby="seller-listings-heading">
              <div className="flex items-end justify-between gap-4">
                <div>
                  <h2 id="seller-listings-heading" className="text-xl font-semibold tracking-tight text-text-primary">
                    Your listings
                  </h2>
                  <p className="mt-1 text-sm text-text-secondary">Keep stock and availability up to date.</p>
                </div>
                <Link to="/listing/mine" className="shrink-0 text-sm font-semibold text-primary hover:underline">
                  See all
                </Link>
              </div>

              {listingsError ? (
                <p role="alert" className="mt-4 rounded-xl bg-red-50 p-4 text-sm text-red-800">
                  {listingsError.message || 'Could not load your listings.'}
                </p>
              ) : listingsLoading ? (
                <div className="mt-4 h-24 animate-pulse rounded-2xl bg-lavender" role="status">
                  <span className="sr-only">Loading your listings</span>
                </div>
              ) : listings.length === 0 ? (
                <div className="mt-4 flex flex-col items-start gap-3 rounded-2xl border border-dashed border-border p-6">
                  <Store size={22} className="text-primary" aria-hidden="true" />
                  <div>
                    <h3 className="font-semibold text-text-primary">Your shop is ready for its first listing</h3>
                    <p className="mt-1 text-sm text-text-secondary">Add an item with clear photos, a price and available quantity.</p>
                  </div>
                  <Link to="/listing/create" className="text-sm font-semibold text-primary hover:underline">
                    Create your first listing
                  </Link>
                </div>
              ) : (
                <ul className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
                  {listings.slice(0, 4).map((listing) => (
                    <li key={listing.id} className="flex min-w-0 items-center gap-4 rounded-2xl border border-border bg-white p-4">
                      <div className="h-16 w-16 shrink-0 overflow-hidden rounded-xl bg-lavender">
                        {listing.imageUrl && <img src={listing.imageUrl} alt="" className="h-full w-full object-cover" />}
                      </div>
                      <div className="min-w-0 flex-1">
                        <Link to={`/product/${listing.id}`} className="block truncate font-semibold text-text-primary hover:underline">
                          {listing.name}
                        </Link>
                        <p className="mt-1 text-sm text-text-secondary">
                          {formatPrice(listing.price)} <span aria-hidden="true">·</span> {listing.stockQuantity} in stock
                        </p>
                      </div>
                      <span className={`shrink-0 text-xs font-medium ${listing.active ? 'text-green-800' : 'text-text-muted'}`}>
                        {listing.active ? 'On sale' : 'Paused'}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="mt-9" aria-labelledby="seller-orders-heading">
              <div>
                <h2 id="seller-orders-heading" className="text-xl font-semibold tracking-tight text-text-primary">
                  Orders to prepare
                </h2>
                <p className="mt-1 max-w-2xl text-sm leading-6 text-text-secondary">
                  Update an order as you confirm it, send it, or make it ready for meetup.
                </p>
              </div>
              {ordersError ? (
                <p role="alert" className="mt-4 rounded-xl bg-red-50 p-4 text-sm text-red-800">
                  {ordersError.message || 'Could not load seller orders.'}
                </p>
              ) : ordersLoading ? (
                <div className="mt-4 h-20 animate-pulse rounded-2xl bg-lavender" role="status">
                  <span className="sr-only">Loading orders to prepare</span>
                </div>
              ) : orders.length === 0 ? (
                <p className="mt-4 rounded-2xl bg-lavender/50 p-5 text-sm text-text-secondary">
                  No orders to prepare yet.
                </p>
              ) : (
                <ul className="mt-4 space-y-3">
                  {orders.slice(0, 6).map((order) => {
                    const action = {
                      PENDING: 'Confirm order',
                      CONFIRMED: order.fulfillmentMethod === 'MEETUP' ? 'Ready for meetup' : 'Mark as sent',
                      SHIPPED: order.fulfillmentMethod === 'MEETUP' ? 'Mark as collected' : 'Mark as delivered',
                    }[order.status];
                    return (
                      <li key={order.id} className="flex flex-col gap-3 rounded-2xl border border-border bg-white p-4 sm:flex-row sm:items-center sm:justify-between">
                        <div className="min-w-0">
                          <Link to={`/orders/${order.id}`} className="font-semibold text-text-primary hover:underline">
                            Order {order.id}
                          </Link>
                          <p className="mt-1 text-sm text-text-secondary">
                            {formatPrice(order.totalAmount)} <span aria-hidden="true">·</span>{' '}
                            {order.fulfillmentMethod === 'MEETUP' ? 'Meetup' : 'Delivery'} <span aria-hidden="true">·</span>{' '}
                            {order.status}
                          </p>
                          {order.estimatedDeliveryDate && (
                            <p className="mt-1 text-xs text-text-muted">
                              Estimated delivery by {new Date(`${order.estimatedDeliveryDate}T00:00:00`).toLocaleDateString('en-ZA')}
                            </p>
                          )}
                        </div>
                        {action && (
                          <Button size="sm" disabled={Boolean(busyOrderId)} onClick={() => advanceOrder(order)}>
                            {busyOrderId === order.id ? 'Updating…' : action}
                          </Button>
                        )}
                      </li>
                    );
                  })}
                </ul>
              )}
            </section>

            <section className="mt-9" aria-labelledby="seller-payments-heading">
              <div>
                <h2 id="seller-payments-heading" className="text-xl font-semibold tracking-tight text-text-primary">
                  Payments to confirm
                </h2>
                <p className="mt-1 max-w-2xl text-sm leading-6 text-text-secondary">
                  Confirm EFT only after funds clear. Confirm cash after you receive it at pickup.
                </p>
              </div>
              {paymentsError ? (
                <p role="alert" className="mt-4 rounded-xl bg-red-50 p-4 text-sm text-red-800">
                  {paymentsError.message || 'Could not load seller payments.'}
                </p>
              ) : actionError ? (
                <p role="alert" className="mt-4 rounded-xl bg-red-50 p-4 text-sm text-red-800">{actionError}</p>
              ) : actionMessage ? (
                <p role="status" className="mt-4 text-sm text-green-800">{actionMessage}</p>
              ) : null}
              {paymentsLoading ? (
                <div className="mt-4 h-20 animate-pulse rounded-2xl bg-lavender" role="status">
                  <span className="sr-only">Loading payments to confirm</span>
                </div>
              ) : pendingPayments.length === 0 ? (
                <p className="mt-4 rounded-2xl bg-lavender/50 p-5 text-sm text-text-secondary">
                  No payments need confirmation right now.
                </p>
              ) : (
                <ul className="mt-4 space-y-3">
                  {pendingPayments.map((payment) => (
                    <li key={payment.paymentId} className="flex flex-col gap-3 rounded-2xl border border-border bg-white p-4 sm:flex-row sm:items-center sm:justify-between">
                      <div className="min-w-0">
                        <p className="font-semibold text-text-primary">
                          {payment.method === 'CASH_ON_PICKUP' ? 'Cash on pickup' : 'EFT'}
                          <span className="mx-2 text-text-muted" aria-hidden="true">·</span>
                          {formatPrice(payment.amount)}
                        </p>
                        <p className="mt-1 truncate text-xs text-text-secondary">
                          Order {payment.orderId} <span aria-hidden="true">·</span> Ref {payment.transactionReference}
                        </p>
                      </div>
                      <Button
                        size="sm"
                        disabled={Boolean(busyPaymentId)}
                        onClick={() => confirmReceipt(payment)}
                      >
                        <Check size={15} aria-hidden="true" />
                        {busyPaymentId === payment.paymentId
                          ? 'Confirming…'
                          : payment.method === 'CASH_ON_PICKUP'
                            ? 'Confirm cash received'
                            : 'Confirm cleared EFT'}
                      </Button>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </>
        )}
      </div>
    </Layout>
  );
};

export default SellerHome;
