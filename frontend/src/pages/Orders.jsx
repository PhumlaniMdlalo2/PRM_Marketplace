import { useCallback, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, PackageOpen } from 'lucide-react';
import StatusBadge from '../components/ui/StatusBadge';
import EmptyState from '../components/ui/EmptyState';
import Layout from '../components/layout/Layout';
import { listOrderItems, listOrders } from '../api/orders';
import { useAsync } from '../hooks/useAsync';
import { formatPrice } from '../lib/format';

/**
 * The tabs are buckets, not statuses.
 *
 * OrderStatus has six values, and showing six tabs for a buyer with two orders is noise. The split
 * is by what the buyer can do about it: still being dealt with, on its way, or finished with.
 * REFUNDED sits with cancelled rather than on its own because both mean the sale did not go through
 * for this order.
 */
const TABS = [
  { id: 'all', label: 'All', statuses: null },
  { id: 'pending', label: 'Pending', statuses: ['PENDING', 'CONFIRMED'] },
  { id: 'shipped', label: 'Shipped', statuses: ['SHIPPED', 'DELIVERED'] },
  { id: 'cancelled', label: 'Cancelled', statuses: ['CANCELLED', 'REFUNDED'] },
];

const formatDate = (value) => {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleDateString('en-ZA');
};

// Shared so that `orders ?? EMPTY` keeps a stable identity across renders. An inline `?? []` is a
// fresh array every render, which makes it an unusable dependency for the counts memo below.
const EMPTY = [];

const Orders = () => {
  const [activeTab, setActiveTab] = useState('all');

  /**
   * One request for the orders, then one per order for its lines.
   *
   * There is no endpoint that returns orders with their items included, so this is N+1 by necessity
   * rather than by choice. It is bounded by the caller's own order count and the requests are
   * independent, so they go out together rather than in a chain. A single order failing to load its
   * lines must not blank the whole page — the order still has a status and a total.
   */
  const loadOrders = useCallback(async () => {
    const orders = await listOrders();
    return Promise.all(
      orders.map(async (order) => {
        try {
          return { ...order, items: await listOrderItems(order.id) };
        } catch {
          return { ...order, items: [] };
        }
      }),
    );
  }, []);

  const { data: orders, loading, error } = useAsync(loadOrders);

  const all = orders ?? EMPTY;
  const counts = useMemo(() => Object.fromEntries(
    TABS.map((tab) => [tab.id, tab.statuses
      ? all.filter((order) => tab.statuses.includes(order.status)).length
      : all.length]),
  ), [all]);

  const tab = TABS.find((entry) => entry.id === activeTab) ?? TABS[0];
  const visible = tab.statuses
    ? all.filter((order) => tab.statuses.includes(order.status))
    : all;

  /**
   * A one-line summary of what was bought. An order is a cart at the moment it was placed, so it can
   * hold several listings; naming only the first would misrepresent a multi-item order.
   */
  const describe = (order) => {
    const lines = order.items ?? [];
    if (lines.length === 0) return 'Order details unavailable';
    const [first] = lines;
    const extra = lines.length - 1;
    const name = first.product?.name ?? 'Removed listing';
    return extra > 0 ? `${name} +${extra} more` : name;
  };

  return (
    <Layout>
      <div className="app-container py-6 max-w-2xl mx-auto">
        <h1 className="text-2xl font-bold text-text-primary tracking-tight">Your orders</h1>
        <p className="text-text-secondary mt-1">
          {loading ? 'Loading…' : `${all.length} total ${all.length === 1 ? 'order' : 'orders'}`}
        </p>

        {error && (
          <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-2xl" role="alert">
            <p className="text-sm text-red-800">{error.message}</p>
          </div>
        )}

        <div className="mt-6 flex gap-2 border-b border-border pb-3 overflow-x-auto scrollbar-hide">
          {TABS.map((entry) => (
            <button
              key={entry.id}
              onClick={() => setActiveTab(entry.id)}
              aria-pressed={activeTab === entry.id}
              className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all duration-200 active:scale-95 ${
                activeTab === entry.id
                  ? 'bg-primary text-white shadow-md shadow-primary/20'
                  : 'bg-lavender text-text-secondary hover:bg-lavender-dark'
              }`}
            >
              {entry.label}
              {loading ? '' : ` (${counts[entry.id] ?? 0})`}
            </button>
          ))}
        </div>

        <div className="mt-5 space-y-3">
          {!loading && visible.length === 0 ? (
            <EmptyState
              icon={PackageOpen}
              title={`No ${tab.label.toLowerCase()} orders`}
              description="When you place an order, it will show up here by status."
            />
          ) : (
            visible.map((order) => (
              /* The whole row is the link to the order's detail page, which is where the line items
                 and the cancel action live. It was a static div with a chevron on it, which promised
                 somewhere to go. */
              <Link
                key={order.id}
                to={`/orders/${order.id}`}
                className="w-full bg-white border border-border rounded-2xl p-4 flex items-center gap-4 text-left hover:bg-lavender/40 transition-colors"
              >
                <span className="w-14 h-14 bg-lavender rounded-xl flex-shrink-0 overflow-hidden">
                  {order.items?.[0]?.product?.imageUrl && (
                    <img
                      src={order.items[0].product.imageUrl}
                      alt=""
                      className="w-full h-full object-cover"
                    />
                  )}
                </span>
                <span className="flex-1 min-w-0">
                  <span className="block font-semibold text-text-primary truncate">
                    {describe(order)}
                  </span>
                  <span className="block text-sm text-text-secondary mt-0.5">
                    {formatPrice(order.totalAmount)}
                  </span>
                  <span className="block text-xs text-text-muted mt-0.5">
                    {formatDate(order.createdAt)}
                    {order.shippingAddress?.singleLine ? ` · ${order.shippingAddress.singleLine}` : ''}
                  </span>
                </span>
                <span className="flex flex-col items-end gap-2">
                  <StatusBadge status={order.status} />
                  <ChevronRight size={18} className="text-text-muted" aria-hidden="true" />
                </span>
              </Link>
            ))
          )}
        </div>
      </div>
    </Layout>
  );
};

export default Orders;