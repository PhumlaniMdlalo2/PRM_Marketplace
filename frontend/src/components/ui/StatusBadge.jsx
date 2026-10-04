/**
 * An order's lifecycle badge.
 *
 * The colours used to be keyed by lowercase `pending`/`completed`/`cancelled`, which no longer
 * matches anything the server sends. OrderStatus has six values — PENDING, CONFIRMED, SHIPPED,
 * DELIVERED, CANCELLED, REFUNDED — and every one of them used to fall through to the amber
 * "pending" style, so a delivered order looked like it was still waiting to be picked.
 *
 * The API's uppercase name is the key. The lowercase forms are kept as aliases so a caller passing
 * an already-lowercased value still gets the intended colour rather than the fallback.
 */

const AMBER = 'bg-[#E8A838]/10 text-[#E8A838]';
const GREEN = 'bg-[#34B37A]/10 text-[#34B37A]';
const BLUE = 'bg-[#4A90D9]/10 text-[#4A90D9]';
const RED = 'bg-[#E05B6A]/10 text-[#E05B6A]';
const GREY = 'bg-text-muted/10 text-text-secondary';

const STATUS_STYLES = {
  PENDING: AMBER,
  CONFIRMED: AMBER,
  SHIPPED: BLUE,
  DELIVERED: GREEN,
  COMPLETED: GREEN,
  CANCELLED: RED,
  REFUNDED: GREY,
};

const STATUS_LABELS = {
  PENDING: 'Pending',
  CONFIRMED: 'Confirmed',
  SHIPPED: 'Shipped',
  DELIVERED: 'Delivered',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
  REFUNDED: 'Refunded',
};

const StatusBadge = ({ status }) => {
  const key = String(status ?? '').toUpperCase();
  const style = STATUS_STYLES[key] ?? AMBER;
  const label = STATUS_LABELS[key] ?? (status || 'Unknown');

  return (
    <span className={`px-2.5 py-1 rounded-md text-xs font-semibold ${style} inline-flex items-center gap-1`}>
      <span className="w-1.5 h-1.5 rounded-full bg-current" aria-hidden="true" />
      {label}
    </span>
  );
};

export default StatusBadge;