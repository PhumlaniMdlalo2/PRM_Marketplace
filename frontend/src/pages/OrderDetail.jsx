import { useCallback, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Package } from 'lucide-react';
import Layout from '../components/layout/Layout';
import StatusBadge from '../components/ui/StatusBadge';
import Button from '../components/ui/Button';
import EmptyState from '../components/ui/EmptyState';
import { cancelOrder, getOrder, listOrderItems } from '../api/orders';
import { useAsync } from '../hooks/useAsync';
import { formatPrice } from '../lib/format';

/**
 * One order in full.
 *
 * `/orders` summarises each order to a single line and throws the rest away, which left no way to see
 * what was actually bought on a multi-item order, where it was going, or to cancel one. All three
 * are here.
 *
 * The lines and the order header are fetched separately because there is no endpoint that returns
 * both. A failure to load the lines must not hide the order itself: the status, total and delivery
 * address are still worth showing on their own.
 */

/** Cancellation is a move the server validates, so the UI mirrors the statuses it accepts. */
const CANCELLABLE = ['PENDING', 'CONFIRMED'];

const formatDate = (value) => {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleDateString('en-ZA');
};

const OrderDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const loadOrder = useCallback(() => getOrder(id), [id]);
  const { data: order, loading, error, run } = useAsync(loadOrder);

  const loadItems = useCallback(() => listOrderItems(id), [id]);
  const { data: items, error: itemsError } = useAsync(loadItems);

  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState(null);

  const cancel = async () => {
    setCancelError(null);
    setCancelling(true);
    try {
      await cancelOrder(id);
      // Refetched rather than patched locally: the status the server settled on may not be the one
      // that was asked for, and cancellation also returns stock.
      await run();
    } catch (caught) {
      setCancelError(caught.status === 400
        ? 'This order can no longer be cancelled. Only orders that are pending or confirmed can be.'
        : caught.message);
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return (
      <Layout>
        <div className="app-container py-6 max-w-2xl mx-auto">
          <div className="h-8 w-40 animate-pulse rounded-xl bg-lavender mb-6" />
          <div className="h-64 animate-pulse rounded-2xl bg-lavender" />
        </div>
      </Layout>
    );
  }

  if (error || !order) {
    return (
      <Layout>
        <div className="app-container py-6 max-w-2xl mx-auto">
          <EmptyState
            icon={Package}
            title="Order not found"
            description={error?.message ?? 'That order does not exist, or it is not yours to see.'}
          />
          <div className="mt-4 text-center">
            <Link to="/orders" className="text-primary font-medium underline">Back to orders</Link>
          </div>
        </div>
      </Layout>
    );
  }

  const canCancel = CANCELLABLE.includes(order.status);

  return (
    <Layout>
      <div className="app-container py-6 max-w-2xl mx-auto pb-24">
        <div className="flex items-center gap-4 mb-6">
          <button
            type="button"
            onClick={() => navigate('/orders')}
            className="p-2 -ml-2 rounded-full text-text-secondary hover:text-text-primary hover:bg-lavender transition-colors"
            aria-label="Back to orders"
          >
            <ArrowLeft size={20} />
          </button>
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Order</h1>
        </div>

        <section className="bg-white border border-border rounded-2xl p-4">
          <div className="flex items-center justify-between gap-3">
            <p className="text-sm text-text-secondary">
              Placed {formatDate(order.createdAt)}
            </p>
            <StatusBadge status={order.status} />
          </div>
          <p className="mt-2 text-2xl font-bold text-text-primary">
            {formatPrice(order.totalAmount)}
          </p>
          {order.shippingAddress?.singleLine && (
            <p className="mt-3 text-sm text-text-secondary">
              <span className="block text-xs font-medium uppercase tracking-wider text-text-muted">
                Delivering to
              </span>
              {order.shippingAddress.singleLine}
            </p>
          )}
        </section>

        <h2 className="mt-6 text-xs font-semibold uppercase tracking-wider text-text-muted mb-2">
          Items
        </h2>

        {itemsError ? (
          <p role="alert" className="text-sm text-error">
            Could not load the items on this order.
          </p>
        ) : (items?.length ?? 0) === 0 && !itemsError ? (
          <p className="text-sm text-text-secondary">No items on this order.</p>
        ) : (
          <ul className="space-y-2">
            {items?.map((item) => (
              <li key={item.id} className="bg-white border border-border rounded-2xl p-3 flex items-center gap-3">
                <span className="w-12 h-12 bg-lavender rounded-xl flex-shrink-0 overflow-hidden">
                  {item.product?.imageUrl && (
                    <img src={item.product.imageUrl} alt="" className="w-full h-full object-cover" />
                  )}
                </span>
                <span className="flex-1 min-w-0">
                  <span className="block text-sm font-medium text-text-primary truncate">
                    {item.product?.name ?? 'Removed listing'}
                  </span>
                  <span className="block text-xs text-text-muted mt-0.5">
                    {item.quantity} × {formatPrice(item.priceAtPurchase)}
                  </span>
                </span>
                <span className="text-sm font-semibold text-text-primary">
                  {formatPrice(Number(item.priceAtPurchase ?? 0) * (item.quantity ?? 0))}
                </span>
              </li>
            ))}
          </ul>
        )}

        {cancelError && (
          <div className="mt-5 p-4 bg-red-50 border border-red-200 rounded-2xl" role="alert">
            <p className="text-sm text-red-800">{cancelError}</p>
          </div>
        )}

        {canCancel && (
          <div className="mt-6">
            <Button
              variant="secondary"
              disabled={cancelling}
              onClick={cancel}
            >
              {cancelling ? 'Cancelling…' : 'Cancel this order'}
            </Button>
            <p className="mt-2 text-xs text-text-muted">
              Cancelling returns the items to stock. It only works while an order is pending or
              confirmed.
            </p>
          </div>
        )}
      </div>
    </Layout>
  );
};

export default OrderDetail;