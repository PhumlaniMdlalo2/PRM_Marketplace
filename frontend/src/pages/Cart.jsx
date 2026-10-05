import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Minus, Plus, Lock, Trash2, MapPin } from 'lucide-react';
import Button from '../components/ui/Button';
import BackButton from '../components/ui/BackButton';
import Layout from '../components/layout/Layout';
import { listCartItems, removeCartItem, setCartItemQuantity } from '../api/cart';
import { checkoutCart } from '../api/orders';
import { listAddresses } from '../api/addresses';
import { listAll } from '../api/products';
import { useAsync } from '../hooks/useAsync';
import { formatPrice } from '../lib/format';

/**
 * A cart line as this page needs it.
 *
 * The server returns a CartItem whose `product` is the whole listing. Flattening the two together
 * here keeps the markup free of `item.product.name` chains, and gives one place to describe a line
 * whose product has since been deleted — the row has to disappear, but not take the page with it.
 */
const toLine = (item) => ({
  id: item.id,
  productId: item.product?.id ?? null,
  name: item.product?.name ?? 'Listing no longer available',
  price: Number(item.product?.price ?? 0),
  originalPrice: Number(item.product?.originalPrice ?? 0) || null,
  quantity: item.quantity,
  condition: item.product?.condition ?? null,
  image: item.product?.imageUrl || null,
});

const Cart = () => {
  const navigate = useNavigate();

  const loadCart = useCallback(async () => (await listCartItems()).map(toLine), []);
  const loadSuggestions = useCallback(async () => await listAll(), []);
  const loadAddresses = useCallback(() => listAddresses(), []);

  const { data: lines, loading, error, setData } = useAsync(loadCart);
  // Recommendations are secondary: a failure here must not put an error banner above a working cart.
  const { data: catalogue } = useAsync(loadSuggestions);
  const { data: addresses, loading: addressesLoading } = useAsync(loadAddresses);

  const [busyLine, setBusyLine] = useState(null);
  const [checkoutError, setCheckoutError] = useState(null);
  const [placing, setPlacing] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState('CARD');
  const [chosenAddressId, setChosenAddressId] = useState(null);

  const items = lines ?? [];
  const savedAddresses = addresses ?? [];

  /**
   * The address actually being shipped to, derived on each render rather than synced into state.
   *
   * A choice only sticks while it still names one of the caller's own addresses; deleting the
   * address you picked falls back to their default, and having no addresses at all leaves nothing
   * selected. That also means the default lands on screen without an effect writing state after the
   * first paint.
   */
  const shippingAddressId = savedAddresses.some((address) => address.id === chosenAddressId)
    ? chosenAddressId
    : (savedAddresses.find((address) => address.defaultAddress) ?? savedAddresses[0])?.id ?? null;

  const hasAddresses = savedAddresses.length > 0;

  /**
   * Applies a quantity change to the local list straight away, then reconciles with the response.
   *
   * The server is the authority — it is what reserves stock at checkout — so its answer replaces
   * the optimistic value rather than merely being hoped for. A quantity of zero or less deletes the
   * line, which is why the decrement goes to the API instead of being clamped locally.
   */
  const changeQuantity = async (line, nextQuantity) => {
    if (busyLine) return;
    setBusyLine(line.id);
    setCheckoutError(null);
    try {
      const updated = await setCartItemQuantity(line.id, nextQuantity);
      setData((current) => (current ?? [])
        .map((entry) => (entry.id === line.id
          ? (updated ? { ...entry, quantity: updated.quantity } : null)
          : entry))
        .filter(Boolean));
    } catch (caught) {
      setCheckoutError(caught.message);
    } finally {
      setBusyLine(null);
    }
  };

  const removeLine = async (line) => {
    if (busyLine) return;
    setBusyLine(line.id);
    setCheckoutError(null);
    try {
      await removeCartItem(line.id);
      setData((current) => (current ?? []).filter((entry) => entry.id !== line.id));
    } catch (caught) {
      setCheckoutError(caught.message);
    } finally {
      setBusyLine(null);
    }
  };

  const proceedToCheckout = async () => {
    if (placing || items.length === 0) return;
    if (!hasAddresses) {
      setCheckoutError('Add a delivery address before checking out');
      return;
    }
    setPlacing(true);
    setCheckoutError(null);
    try {
      // Both values are the caller's own choices. The server reads the cart and the totals itself,
      // and refuses an address that is not theirs.
      await checkoutCart(shippingAddressId, paymentMethod);
      navigate('/orders');
    } catch (caught) {
      setCheckoutError(caught.message);
      setPlacing(false);
    }
  };

  const subtotal = items.reduce((sum, line) => sum + line.price * line.quantity, 0);

  // No shipping line is shown because the server does not charge one: an order's totalAmount is the
  // sum of its lines. The old flat R50 was invented in the mock and made the summary disagree with
  // what the buyer is actually charged.
  const total = subtotal;

  const inCart = new Set(items.map((line) => line.productId).filter(Boolean));
  const suggestions = (catalogue ?? [])
    .filter((product) => !inCart.has(product.id) && product.active !== false)
    .slice(0, 6);

  // The ids are the backend's enum names verbatim. Jackson binds PaymentMethod case-sensitively, so
// a lowercase value here would be rejected with a 400 rather than quietly defaulting.
const paymentOptions = [
    { id: 'CARD', label: 'Credit / debit card' },
    { id: 'EFT', label: 'EFT' },
    { id: 'WALLET', label: 'Digital wallet' },
  ];

  return (
    <Layout showNav={false}>
      <div className="app-container py-6 pb-10">
        <div className="flex items-center gap-4 mb-2">
          <BackButton />
          <div>
            <h1 className="text-2xl font-bold text-text-primary tracking-tight">Your cart</h1>
            <p className="text-sm text-text-secondary">
              {loading ? 'Loading…' : `${items.length} ${items.length === 1 ? 'item' : 'items'}`}
            </p>
          </div>
        </div>

        {error && (
          <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-2xl" role="alert">
            <p className="text-sm text-red-800">{error.message}</p>
          </div>
        )}

        <div className="lg:grid lg:grid-cols-[minmax(0,1fr)_340px] lg:items-start lg:gap-8">
          <div>
            <div className="mt-6 space-y-3.5">
              {items.map((item) => (
                <div key={item.id} className="flex gap-4 p-4 bg-white border border-border rounded-2xl">
                  <div className="w-20 h-20 bg-lavender rounded-xl flex-shrink-0 overflow-hidden">
                    {item.image && (
                      <img src={item.image} alt="" className="w-full h-full object-cover" />
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="font-semibold text-text-primary text-[15px] leading-snug">
                      {item.name}
                    </h3>
                    {item.condition && (
                      <p className="text-sm text-text-secondary mt-0.5">Condition: {item.condition}</p>
                    )}
                    <div className="flex items-center gap-2 mt-1.5">
                      {item.originalPrice && (
                        <span className="text-xs text-text-muted line-through">
                          {formatPrice(item.originalPrice)}
                        </span>
                      )}
                      <span className="font-bold text-text-primary">{formatPrice(item.price)}</span>
                    </div>
                    <div className="flex items-center justify-between mt-3">
                      <div className="flex items-center gap-2.5">
                        <button
                          onClick={() => changeQuantity(item, item.quantity - 1)}
                          disabled={busyLine === item.id}
                          className="w-8 h-8 rounded-lg border border-border flex items-center justify-center hover:bg-lavender hover:border-lavender-dark active:scale-90 transition-all duration-200 disabled:opacity-50"
                          aria-label={`Decrease quantity of ${item.name}`}
                        >
                          <Minus size={14} />
                        </button>
                        <span className="font-semibold w-5 text-center">{item.quantity}</span>
                        <button
                          onClick={() => changeQuantity(item, item.quantity + 1)}
                          disabled={busyLine === item.id}
                          className="w-8 h-8 rounded-lg border border-border flex items-center justify-center hover:bg-lavender hover:border-lavender-dark active:scale-90 transition-all duration-200 disabled:opacity-50"
                          aria-label={`Increase quantity of ${item.name}`}
                        >
                          <Plus size={14} />
                        </button>
                      </div>
                      <button
                        onClick={() => removeLine(item)}
                        disabled={busyLine === item.id}
                        className="flex items-center gap-1.5 text-xs font-medium text-text-muted hover:text-red-600 transition-colors disabled:opacity-50"
                        aria-label={`Remove ${item.name} from your cart`}
                      >
                        <Trash2 size={14} />
                        Remove
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {!loading && items.length === 0 && (
              <div className="py-16 text-center">
                <p className="text-text-secondary">Your cart is empty</p>
                <p className="text-sm text-text-muted mt-1">Browse the marketplace to add items.</p>
              </div>
            )}

            {suggestions.length > 0 && (
              <div className="mt-8">
                <h3 className="font-semibold text-text-primary mb-3">You might also like</h3>
                <div className="flex gap-3.5 overflow-x-auto pb-4 scrollbar-hide">
                  {suggestions.map((product) => (
                    <div key={product.id} className="flex-shrink-0 w-32">
                      <div className="w-32 h-32 bg-lavender rounded-xl overflow-hidden">
                        {product.imageUrl && (
                          <img src={product.imageUrl} alt="" className="w-full h-full object-cover" />
                        )}
                      </div>
                      <p className="text-sm font-medium text-text-primary mt-2 leading-snug line-clamp-2">
                        {product.name}
                      </p>
                      <p className="text-sm font-semibold text-text-primary mt-0.5">
                        {formatPrice(product.price)}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="mt-8">
              <div className="flex items-center gap-2 mb-1">
                <MapPin size={16} className="text-primary" />
                <h3 className="font-semibold text-text-primary">Delivery address</h3>
              </div>
              <p className="text-xs text-text-muted mb-3">
                Where the seller should send this order. Manage your addresses in Settings.
              </p>
              {addressesLoading ? (
                <div className="h-16 animate-pulse rounded-2xl bg-lavender" />
              ) : !hasAddresses ? (
                <div className="p-4 border border-border rounded-2xl bg-lavender/40">
                  <p className="text-sm text-text-secondary">
                    You have no saved addresses. Add one in Settings before checking out.
                  </p>
                  <Button
                    variant="secondary"
                    className="mt-3"
                    onClick={() => navigate('/settings')}
                  >
                    Add a delivery address
                  </Button>
                </div>
              ) : (
                <div className="space-y-2.5" role="radiogroup" aria-label="Delivery address">
                  {savedAddresses.map((address) => (
                    <label
                      key={address.id}
                      className={`flex items-start gap-3 p-4 border rounded-2xl cursor-pointer transition-all duration-200 ${
                        shippingAddressId === address.id
                          ? 'border-primary bg-primary-muted shadow-sm shadow-primary/10'
                          : 'border-border hover:border-lavender-dark hover:bg-lavender/30'
                      }`}
                    >
                      <input
                        type="radio"
                        name="shipping-address"
                        checked={shippingAddressId === address.id}
                        onChange={() => setChosenAddressId(address.id)}
                        className="w-4 h-4 mt-0.5 text-primary focus:ring-primary accent-primary"
                      />
                      <span className="text-sm text-text-primary leading-snug">
                        {address.singleLine || address.line1}
                        {address.defaultAddress && (
                          <span className="ml-2 text-xs font-medium text-primary">Default</span>
                        )}
                      </span>
                    </label>
                  ))}
                </div>
              )}
            </div>

            <div className="mt-8">
              <h3 className="font-semibold text-text-primary mb-1">Payment method</h3>
              <p className="text-xs text-text-muted mb-3">
                A payment is filed against the order when you continue, for the full total shown.
              </p>
              <div className="space-y-2.5">
                {paymentOptions.map((option) => (
                  <label
                    key={option.id}
                    className={`flex items-center gap-3 p-4 border rounded-2xl cursor-pointer transition-all duration-200 ${
                      paymentMethod === option.id
                        ? 'border-primary bg-primary-muted shadow-sm shadow-primary/10'
                        : 'border-border hover:border-lavender-dark hover:bg-lavender/30'
                    }`}
                  >
                    <input
                      type="radio"
                      name="payment"
                      value={option.id}
                      checked={paymentMethod === option.id}
                      onChange={(e) => setPaymentMethod(e.target.value)}
                      className="w-4 h-4 text-primary focus:ring-primary accent-primary"
                    />
                    <span className="text-text-primary font-medium">{option.label}</span>
                  </label>
                ))}
              </div>
            </div>
          </div>

          <div className="lg:sticky lg:top-20 mt-8 lg:mt-0">
            <div className="p-5 bg-lavender/60 rounded-2xl">
              <h3 className="font-semibold text-text-primary mb-4">Order summary</h3>
              <div className="space-y-2.5">
                <div className="flex justify-between">
                  <span className="text-text-secondary">Subtotal</span>
                  <span className="text-text-primary font-medium">{formatPrice(subtotal)}</span>
                </div>
                <div className="border-t border-border-lavender pt-3 mt-1">
                  <div className="flex justify-between">
                    <span className="font-bold text-text-primary">Total</span>
                    <span className="font-bold text-text-primary">{formatPrice(total)}</span>
                  </div>
                </div>
              </div>

              {checkoutError && (
                <p className="mt-3 text-sm text-red-700" role="alert">{checkoutError}</p>
              )}

              <Button
                size="lg"
                className="mt-5"
                onClick={proceedToCheckout}
                disabled={placing || items.length === 0 || addressesLoading || !hasAddresses}
              >
                <Lock size={16} />
                {placing ? 'Placing order…' : `Proceed to buy · ${formatPrice(total)}`}
              </Button>

              {shippingAddressId && (
                <p className="mt-3 text-xs text-text-muted">
                  Delivering to{' '}
                  {savedAddresses.find((a) => a.id === shippingAddressId)?.singleLine
                    ?? 'your selected address'}
                  .
                </p>
              )}
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default Cart;