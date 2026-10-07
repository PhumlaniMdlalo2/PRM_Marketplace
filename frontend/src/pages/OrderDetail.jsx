import { useCallback, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Package } from 'lucide-react';
import Layout from '../components/layout/Layout';
import StatusBadge from '../components/ui/StatusBadge';
import Button from '../components/ui/Button';
import EmptyState from '../components/ui/EmptyState';
import { cancelOrder, getOrder, listOrderItems } from '../api/orders';
import {
  isPaymentSimulationEnabled, listPaymentInstructions, simulatePayment, updatePaymentStatus,
} from '../api/payments';
import { useAsync } from '../hooks/useAsync';
import { formatPrice } from '../lib/format';
import { startConversation } from '../api/messages';

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

const trackingStages = (fulfillmentMethod) => [
  'PENDING',
  'CONFIRMED',
  'SHIPPED',
  'DELIVERED',
].map((status) => ({
  status,
  label: status === 'PENDING'
    ? 'Order placed'
    : status === 'CONFIRMED'
      ? 'Seller confirmed'
      : status === 'SHIPPED'
        ? fulfillmentMethod === 'MEETUP' ? 'Ready for meetup' : 'On the way'
        : fulfillmentMethod === 'MEETUP' ? 'Collected' : 'Delivered',
}));

const OrderDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const loadOrder = useCallback(() => getOrder(id), [id]);
  const { data: order, loading, error, run } = useAsync(loadOrder);

  const loadItems = useCallback(() => listOrderItems(id), [id]);
  const { data: items, error: itemsError } = useAsync(loadItems);

  const loadPaymentInfo = useCallback(async () => {
    const [instructions, simulationEnabled] = await Promise.all([
      listPaymentInstructions(id),
      isPaymentSimulationEnabled(),
    ]);
    return { instructions, simulationEnabled };
  }, [id]);
  const {
    data: paymentInfo,
    loading: paymentInfoLoading,
    error: paymentInfoError,
    run: reloadPaymentInfo,
  } = useAsync(loadPaymentInfo);

  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState(null);
  const [paymentAction, setPaymentAction] = useState(null);
  const [paymentError, setPaymentError] = useState(null);

  const contactSeller = async (sellerUserId) => {
    if (!sellerUserId || paymentAction) return;
    setPaymentAction('CONTACT');
    setPaymentError(null);
    try {
      const conversation = await startConversation(sellerUserId);
      navigate(`/messages/${conversation.id}`);
    } catch (caught) {
      setPaymentError(caught.message || 'Could not open a conversation with the seller.');
    } finally {
      setPaymentAction(null);
    }
  };

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

  const handlePayment = async (paymentId, outcome) => {
    if (!paymentId || paymentAction) return;

    setPaymentAction(outcome);
    setPaymentError(null);
    try {
      await simulatePayment(paymentId, outcome);
      await reloadPaymentInfo();
    } catch (caught) {
      setPaymentError(caught.message || 'The sandbox result could not be recorded.');
    } finally {
      setPaymentAction(null);
    }
  };

  const retryPayment = async (paymentId) => {
    if (!paymentId || paymentAction) return;

    setPaymentAction('RETRY');
    setPaymentError(null);
    try {
      await updatePaymentStatus(paymentId, 'PENDING');
      await reloadPaymentInfo();
    } catch (caught) {
      setPaymentError(caught.message || 'The payment could not be retried.');
    } finally {
      setPaymentAction(null);
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
  const paymentInstructions = paymentInfo?.instructions ?? [];
  const isMeetup = order.fulfillmentMethod === 'MEETUP';
  const completedStage = trackingStages(order.fulfillmentMethod).findIndex(
    (stage) => stage.status === order.status,
  );

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
          <p className="mt-2 text-xs text-text-muted">Order reference: {order.id}</p>
          <p className="mt-3 text-sm font-medium text-text-primary">
            {isMeetup ? 'Meet the seller' : 'Delivery'}
          </p>
          {isMeetup ? (
            <p className="mt-1 text-sm text-text-secondary">
              Arrange a safe time and place with each seller through marketplace messages.
            </p>
          ) : (
            <>
              {order.shippingAddress?.singleLine && (
                <p className="mt-2 text-sm text-text-secondary">
                  <span className="block text-xs font-medium uppercase tracking-wider text-text-muted">
                    Delivering to
                  </span>
                  {order.shippingAddress.singleLine}
                </p>
              )}
              {order.estimatedDeliveryDate && (
                <p className="mt-3 text-sm text-text-secondary">
                  Estimated delivery by{' '}
                  <time dateTime={order.estimatedDeliveryDate}>
                    {formatDate(order.estimatedDeliveryDate)}
                  </time>
                  . This is an estimate, not live courier tracking.
                </p>
              )}
            </>
          )}
        </section>

        <section className="mt-4 rounded-2xl border border-border bg-white p-4" aria-labelledby="tracking-heading">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 id="tracking-heading" className="font-semibold text-text-primary">Order tracking</h2>
              <p className="mt-1 text-sm text-text-secondary">
                {order.status === 'CANCELLED' || order.status === 'REFUNDED'
                  ? `This order is ${order.status.toLowerCase()}.`
                  : trackingStages(order.fulfillmentMethod)[Math.max(completedStage, 0)].label}
              </p>
            </div>
            <StatusBadge status={order.status} />
          </div>
          {order.status !== 'CANCELLED' && order.status !== 'REFUNDED' && (
            <ol className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4" aria-label="Order progress">
              {trackingStages(order.fulfillmentMethod).map((stage, index) => {
                const isComplete = index <= completedStage;
                return (
                  <li key={stage.status} className="flex items-start gap-2">
                    <span className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-xs ${
                      isComplete ? 'bg-primary text-white' : 'border border-border text-text-muted'
                    }`} aria-hidden="true">
                      {isComplete ? '✓' : index + 1}
                    </span>
                    <span className={`text-xs leading-5 ${isComplete ? 'font-medium text-text-primary' : 'text-text-muted'}`}>
                      {stage.label}
                    </span>
                  </li>
                );
              })}
            </ol>
          )}
        </section>

        <section className="mt-4 rounded-2xl border border-border bg-white p-4" aria-labelledby="payment-heading">
          <h2 id="payment-heading" className="font-semibold text-text-primary">Payment</h2>
          {paymentInfoLoading && !paymentInfo ? (
            <div className="mt-3 h-12 animate-pulse rounded-xl bg-lavender" role="status">
              <span className="sr-only">Loading payment status</span>
            </div>
          ) : paymentInfoError ? (
            <p className="mt-2 text-sm text-error" role="alert">
              Payment status could not be loaded: {paymentInfoError.message}
            </p>
          ) : !paymentInstructions.length ? (
            <p className="mt-2 text-sm text-text-secondary">No payment attempt is recorded for this order.</p>
          ) : (
            <div className="mt-3 space-y-3">
              {paymentInstructions.map((payment) => (
                <div key={payment.paymentId} className="rounded-xl border border-border p-3">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold text-text-primary">
                        {payment.method === 'CASH_ON_PICKUP' ? 'Cash on pickup' : payment.method}
                        {payment.sellerName ? ` · ${payment.sellerName}` : ''}
                      </p>
                      <p className="mt-1 text-sm text-text-secondary">
                        {payment.status} · {formatPrice(payment.amount)}
                      </p>
                    </div>
                  </div>
                  <p className="mt-1 text-xs text-text-muted">
                    Payment reference: {payment.transactionReference}
                  </p>
                  {payment.method === 'EFT' && payment.payoutDetails && (
                    <dl className="mt-3 grid grid-cols-1 gap-1 text-sm text-text-secondary">
                      <div>Bank: {payment.payoutDetails.bankName}</div>
                      <div>Account holder: {payment.payoutDetails.accountHolder}</div>
                      <div>Account number: {payment.payoutDetails.accountNumber}</div>
                      <div>Branch code: {payment.payoutDetails.branchCode}</div>
                      <div>Account type: {payment.payoutDetails.accountType}</div>
                      <div>Use reference: {payment.transactionReference}</div>
                    </dl>
                  )}
                  {payment.method === 'EFT' && payment.status === 'PENDING' && (
                    <p className="mt-2 text-xs text-text-muted">
                      Transfer directly to this seller. The seller confirms only after the funds have cleared.
                    </p>
                  )}
                  {payment.method === 'CASH_ON_PICKUP' && payment.status === 'PENDING' && (
                    <p className="mt-2 text-xs text-text-muted">
                      Pay this seller in cash when you collect the items. The seller will confirm receipt.
                    </p>
                  )}
                  {payment.sellerUserId && payment.method !== 'SANDBOX' && (
                    <Button size="sm" variant="secondary" className="mt-3"
                      onClick={() => contactSeller(payment.sellerUserId)}
                      disabled={Boolean(paymentAction)}>
                      {paymentAction === 'CONTACT' ? 'Opening messages…' : 'Message seller'}
                    </Button>
                  )}
                  {paymentInfo.simulationEnabled && payment.method === 'SANDBOX'
                    && payment.status === 'PENDING' && (
                      <div className="mt-3 rounded-xl border border-primary/20 bg-primary-muted p-3">
                        <p className="text-xs text-text-secondary">Test only. No money moves.</p>
                        <div className="mt-2 flex flex-wrap gap-2">
                          <Button size="sm" onClick={() => handlePayment(payment.paymentId, 'SUCCESS')}
                            disabled={Boolean(paymentAction)}>
                            {paymentAction === 'SUCCESS' ? 'Recording…' : 'Simulate success'}
                          </Button>
                          <Button size="sm" variant="secondary"
                            onClick={() => handlePayment(payment.paymentId, 'FAILURE')}
                            disabled={Boolean(paymentAction)}>
                            {paymentAction === 'FAILURE' ? 'Recording…' : 'Simulate failure'}
                          </Button>
                        </div>
                      </div>
                  )}
                  {payment.method === 'SANDBOX' && payment.status === 'FAILED'
                    && paymentInfo.simulationEnabled && (
                      <Button size="sm" className="mt-3" onClick={() => retryPayment(payment.paymentId)}
                        disabled={Boolean(paymentAction)}>
                        {paymentAction === 'RETRY' ? 'Starting retry…' : 'Retry sandbox payment'}
                      </Button>
                  )}
                </div>
              ))}
              {paymentError && <p className="text-sm text-error" role="alert">{paymentError}</p>}
            </div>
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