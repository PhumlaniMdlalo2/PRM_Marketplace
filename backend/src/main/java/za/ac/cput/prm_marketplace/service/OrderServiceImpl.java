package za.ac.cput.prm_marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.domain.NotificationType;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.domain.OrderStatus;
import za.ac.cput.prm_marketplace.domain.FulfillmentMethod;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.exception.ForbiddenException;
import za.ac.cput.prm_marketplace.repository.AddressRepository;
import za.ac.cput.prm_marketplace.repository.CartItemRepository;
import za.ac.cput.prm_marketplace.repository.OrderItemRepository;
import za.ac.cput.prm_marketplace.repository.OrderRepository;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderServiceImpl implements IOrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private static final Set<OrderStatus> CANCELLABLE = EnumSet.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED);

    /** These roles may act on an order only when a line item belongs to their seller profile. */
    private static final Set<Role> MAY_ADVANCE_ORDER = EnumSet.of(Role.STUDENT, Role.VENDOR, Role.ADMIN);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final OrderItemRepository orderItemRepository;
    private final IPaymentService paymentService;
    private final INotificationService notificationService;
    private final IEmailService emailService;
    private final String frontendUrl;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            ProductRepository productRepository,
                            CartItemRepository cartItemRepository,
                            AddressRepository addressRepository,
                            OrderItemRepository orderItemRepository,
                            IPaymentService paymentService,
                            INotificationService notificationService) {
        this(orderRepository, userRepository, productRepository, cartItemRepository, addressRepository,
                orderItemRepository, paymentService, notificationService, null, "http://localhost:5173");
    }

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            ProductRepository productRepository,
                            CartItemRepository cartItemRepository,
                            AddressRepository addressRepository,
                            OrderItemRepository orderItemRepository,
                            IPaymentService paymentService,
                            INotificationService notificationService,
                            IEmailService emailService,
                            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl == null ? "http://localhost:5173" : frontendUrl.replaceAll("/+$", "");
    }

    @Override
    public Order read(UUID id, UUID requesterId) {
        if (id == null || requesterId == null) {
            return null;
        }
        return orderRepository.findByIdAndBuyerId(id, requesterId).orElse(null);
    }

    @Override
    @Transactional
    public Order update(Order order, UUID requesterId) {
        if (order == null || order.getId() == null || requesterId == null) {
            return null;
        }
        Order existing = orderRepository.findByIdAndBuyerId(order.getId(), requesterId).orElse(null);
        if (existing == null) {
            return null;
        }
        if (existing.getStatus() != OrderStatus.PENDING) {
            // Once an order has progressed the buyer's edits would contradict what the seller
            // has already acted on.
            return null;
        }

        // Only the delivery address is buyer-editable. Status, total, buyer and timestamps stay
        // as they are, and the line items are fixed once the order exists.
        Address requested = order.getShippingAddress();
        if (existing.getFulfillmentMethod() == FulfillmentMethod.DELIVERY && requested == null
                || existing.getFulfillmentMethod() == FulfillmentMethod.MEETUP && requested != null) {
            return null;
        }
        if (requested == null) {
            existing.setShippingAddress(null);
        } else {
            // Ownership is checked, but the *stored* row is what gets attached. Attaching the
            // deserialized copy instead would let the caller keep the same address id and quietly
            // rewrite its street and city through an order update, bypassing address validation
            // and leaving the caller's address book disagreeing with their delivered orders.
            Address owned = findOwnedBy(requesterId, requested.getId());
            if (owned == null) {
                // Naming somebody else's address, or one that does not exist, must not quietly
                // redirect the delivery.
                return null;
            }
            existing.setShippingAddress(owned);
        }
        return orderRepository.save(existing);
    }

    /** The persisted address belonging to the caller, or null when the id is not theirs. */
    private Address findOwnedBy(UUID requesterId, UUID addressId) {
        if (addressId == null) {
            return null;
        }
        return addressRepository.findById(addressId)
                .filter(stored -> stored.getUser() != null)
                .filter(stored -> requesterId.equals(stored.getUser().getId()))
                .orElse(null);
    }

    @Override
    @Transactional
    public boolean delete(UUID id, UUID requesterId) {
        if (id == null || requesterId == null) {
            return false;
        }
        Order existing = orderRepository.findByIdAndBuyerId(id, requesterId).orElse(null);
        if (existing == null) {
            return false;
        }
        // Cancelling is the reversible path: it returns the reserved stock. A hard delete of an
        // order that has already reserved stock would leak that stock permanently.
        return cancelInternal(existing);
    }

    @Override
    public List<Order> getAll(UUID requesterId) {
        if (requesterId == null) {
            return List.of();
        }
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(requesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getSellerOrders(UUID requesterId, Role role) {
        if (requesterId == null || (role != Role.STUDENT && role != Role.VENDOR)) {
            return List.of();
        }
        return orderItemRepository.findOrdersByVendorUserId(requesterId);
    }

    @Override
    public List<Order> getByBuyer(UUID buyerId) {
        if (buyerId == null) {
            return List.of();
        }
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    @Override
    public List<Order> getByBuyerAndStatus(UUID buyerId, OrderStatus status) {
        if (buyerId == null || status == null) {
            return List.of();
        }
        return orderRepository.findByBuyerIdAndStatus(buyerId, status);
    }

    public Order checkout(UUID buyerId, UUID shippingAddressId, PaymentMethod paymentMethod) {
        return checkout(buyerId, shippingAddressId, paymentMethod, FulfillmentMethod.MEETUP);
    }

    @Override
    @Transactional
    public Order checkout(UUID buyerId, UUID shippingAddressId, PaymentMethod paymentMethod,
                          FulfillmentMethod fulfillmentMethod) {
        if (buyerId == null) {
            return null;
        }
        FulfillmentMethod selectedFulfillment = fulfillmentMethod == null
                ? FulfillmentMethod.MEETUP
                : fulfillmentMethod;
        PaymentMethod selectedMethod = paymentMethod == null ? PaymentMethod.EFT : paymentMethod;
        if (selectedMethod != PaymentMethod.EFT
                && selectedMethod != PaymentMethod.CASH_ON_PICKUP
                && selectedMethod != PaymentMethod.SANDBOX) {
            return null;
        }
        if (selectedMethod == PaymentMethod.CASH_ON_PICKUP
                && selectedFulfillment != FulfillmentMethod.MEETUP) {
            return null;
        }
        User buyer = userRepository.findById(buyerId).orElse(null);
        if (buyer == null) {
            return null;
        }

        // The cart comes from the database, so a caller cannot order a product that is not in it
        // and cannot pick a quantity the server never agreed to.
        List<CartItem> cartItems = cartItemRepository.findByUser_Id(buyerId);
        if (cartItems.isEmpty()) {
            return null;
        }

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product != null && (product.getVendor() == null || !product.getVendor().isVerified())) {
                throw new ForbiddenException(
                        "Admin approval is required before this seller can sell marketplace items");
            }
        }

        Address shippingAddress = null;
        if (selectedFulfillment == FulfillmentMethod.DELIVERY) {
            if (shippingAddressId == null) {
                return null;
            }
            shippingAddress = addressRepository.findById(shippingAddressId).orElse(null);
            if (shippingAddress == null
                    || shippingAddress.getUser() == null
                    || !buyerId.equals(shippingAddress.getUser().getId())) {
                // Somebody else's address, or one that does not exist. Either way the caller does
                // not get to have it shipped to them.
                return null;
            }
        } else if (shippingAddressId != null) {
            return null;
        }

        Order order = new Order.Builder()
                .setBuyer(buyer)
                .setStatus(OrderStatus.PENDING)
                .setShippingAddress(shippingAddress)
                .setFulfillmentMethod(selectedFulfillment)
                .setEstimatedDeliveryDate(selectedFulfillment == FulfillmentMethod.DELIVERY
                        ? estimateDeliveryDate(LocalDate.now(), 5)
                        : null)
                .build();

        List<CartItem> purchased = new ArrayList<>();

        // Price and reserve in one pass, before anything is written. Reserving first and pricing
        // from the reserved row is safe because decrementStock refuses to go below zero.
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product == null || cartItem.getQuantity() <= 0) {
                continue;
            }
            if (productRepository.decrementStock(product.getId(), cartItem.getQuantity()) == 0) {
                // Either the product was deactivated or somebody else took the last units first.
                // The transaction rolls back, releasing the stock reserved by earlier lines.
                return null;
            }
            Product reserved = productRepository.findById(product.getId()).orElse(null);
            if (reserved == null) {
                return null;
            }
            order.addItem(new OrderItem.Builder()
                    .setOrder(order)
                    .setProduct(reserved)
                    .setQuantity(cartItem.getQuantity())
                    .setPriceAtPurchase(reserved.getPrice())
                    .build());
            purchased.add(cartItem);
        }

        if (order.getItems().isEmpty()) {
            return null;
        }
        order.setTotalAmount(totalOf(order));

        Order saved = orderRepository.save(order);

        // File the payment before emptying the cart, and let a failure here abort the whole
        // checkout. The order existing with no record of any attempt to pay for it is the state
        // this replaces: the order is what the buyer is billed against, so the two are written
        // together or not at all.
        //
        // The amount is left null on purpose. IPaymentService resolves a null amount to the order
        // total, so there is no path by which the caller states what they are paying.
        List<String> paymentReferences = new ArrayList<>();
        if (selectedMethod == PaymentMethod.SANDBOX) {
            Payment attempt = new Payment.Builder()
                    .setOrderId(saved.getId())
                    .setMethod(selectedMethod)
                    .build();
            Payment created = paymentService.create(attempt, buyerId);
            if (created == null) {
                return null;
            }
            if (created.getTransactionReference() != null) {
                paymentReferences.add(created.getTransactionReference());
            }
        } else {
            Map<UUID, BigDecimal> sellerAmounts = new LinkedHashMap<>();
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() == null || item.getProduct().getVendor() == null
                        || item.getProduct().getVendor().getUser() == null
                        || item.getProduct().getVendor().getUser().getId() == null) {
                    return null;
                }
                UUID sellerId = item.getProduct().getVendor().getUser().getId();
                sellerAmounts.merge(sellerId, item.getLineTotal(), BigDecimal::add);
            }
            if (sellerAmounts.isEmpty()) {
                return null;
            }
            for (UUID sellerId : sellerAmounts.keySet()) {
                Payment created = paymentService.createForSeller(saved.getId(), selectedMethod, buyerId, sellerId);
                if (created == null) {
                    return null;
                }
                if (created.getTransactionReference() != null) {
                    paymentReferences.add(created.getTransactionReference());
                }
            }
        }

        // Emptying the cart is scoped to the lines that actually made it into the order.
        cartItemRepository.deleteAll(purchased);
        notifySellersOf(saved);
        sendConfirmationAfterCommit(saved, paymentReferences);
        return saved;
    }

    private static LocalDate estimateDeliveryDate(LocalDate from, int businessDays) {
        LocalDate date = from;
        int added = 0;
        while (added < businessDays) {
            date = date.plusDays(1);
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return date;
    }

    private void sendConfirmationAfterCommit(Order order, List<String> paymentReferences) {
        if (emailService == null || order.getBuyer() == null || order.getBuyer().getEmail() == null) {
            return;
        }
        String recipient = order.getBuyer().getEmail();
        String orderReference = order.getId().toString();
        BigDecimal total = order.getTotalAmount();
        String method = order.getFulfillmentMethod().name();
        String address = order.getShippingAddress() == null ? null : order.getShippingAddress().getSingleLine();
        LocalDate estimatedDeliveryDate = order.getEstimatedDeliveryDate();
        List<String> references = List.copyOf(paymentReferences);
        String trackingUrl = frontendUrl + "/orders/" + orderReference;
        afterCommit(() -> sendEmailSafely(() -> emailService.sendOrderConfirmation(recipient,
                orderReference, total, method, address, estimatedDeliveryDate, references, trackingUrl),
                recipient, orderReference));
    }

    private void sendStatusEmailAfterCommit(Order order) {
        if (emailService == null || order.getBuyer() == null || order.getBuyer().getEmail() == null) {
            return;
        }
        String recipient = order.getBuyer().getEmail();
        String orderReference = order.getId().toString();
        OrderStatus status = order.getStatus();
        String method = order.getFulfillmentMethod().name();
        LocalDate estimatedDeliveryDate = order.getEstimatedDeliveryDate();
        String trackingUrl = frontendUrl + "/orders/" + orderReference;
        afterCommit(() -> sendEmailSafely(() -> emailService.sendOrderStatusUpdate(recipient,
                orderReference, status, method, estimatedDeliveryDate, trackingUrl),
                recipient, orderReference));
    }

    private void sendEmailSafely(Runnable send, String recipient, String orderReference) {
        try {
            send.run();
        } catch (RuntimeException e) {
            log.warn("Could not send order email to {} for order {}", recipient, orderReference, e);
        }
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    /**
     * Tells each seller whose listing was bought that an order exists.
     *
     * <p>Without this a seller has no way to learn that somebody bought from them: the order list is
     * the buyer's, the messages endpoint needs the seller to already know to write first, and nothing
     * else in the product surfaces it. A notification per seller rather than per line, because an
     * order of three of a seller's items is one thing that happened to them.
     *
     * <p>Deliberately skipped when the seller is the buyer. Buying your own listing is possible and is
     * not an error, but a notification telling you about your own order is noise on every refresh.
     */
    private void notifySellersOf(Order order) {
        UUID buyerId = order.getBuyer() == null ? null : order.getBuyer().getId();
        Set<UUID> sellers = new LinkedHashSet<>();
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getProduct() == null || item.getProduct().getVendor() == null
                    || item.getProduct().getVendor().getUser() == null) {
                continue;
            }
            UUID sellerId = item.getProduct().getVendor().getUser().getId();
            if (sellerId != null && !sellerId.equals(buyerId)) {
                sellers.add(sellerId);
            }
        }

        String amount = "R" + (order.getTotalAmount() == null ? "0.00" : order.getTotalAmount().toPlainString());
        for (UUID sellerId : sellers) {
            notify(sellerId, "New order received",
                    "One of your listings was bought. Order " + order.getId() + " for " + amount
                            + " is waiting for you to confirm.");
        }
    }

    /**
     * Tells the buyer their order moved. Admin and the seller's vendor account are the only roles
     * that can move it, so the buyer is otherwise the last to know, watching a status change happen
     * to them.
     */
    private void notifyBuyerOfStatusChange(Order order) {
        User buyer = order.getBuyer();
        if (buyer == null || buyer.getId() == null) {
            return;
        }
        String amount = "R" + (order.getTotalAmount() == null ? "0.00" : order.getTotalAmount().toPlainString());
        String title = order.getStatus() == OrderStatus.CANCELLED
                ? "Order cancelled"
                : "Order " + order.getStatus().name().toLowerCase();
        notify(buyer.getId(), title, "Your order " + order.getId() + " for " + amount
                + " is now " + order.getStatus().name().toLowerCase() + ".");
        sendStatusEmailAfterCommit(order);
    }

    /**
     * A notification that fails must never undo an order that was already written, so this cannot
     * propagate. The same bargain {@code PaymentServiceImpl} makes about payment notifications.
     */
    private void notify(UUID userId, String title, String message) {
        try {
            notificationService.send(userId, NotificationType.ORDER, title, message);
        } catch (RuntimeException e) {
            log.warn("Could not send order notification to {} titled \"{}\"", userId, title, e);
        }
    }

    @Override
    @Transactional
    public Order updateStatus(UUID id, OrderStatus status, UUID requesterId, Role role) {
        if (id == null || status == null || requesterId == null || !MAY_ADVANCE_ORDER.contains(role)) {
            return null;
        }
        Order existing = orderRepository.findById(id).orElse(null);
        if (existing == null || existing.getStatus() == status) {
            return null;
        }
        if (!mayActOn(existing, requesterId, role)) {
            return null;
        }
        if (role != Role.ADMIN && status != OrderStatus.CANCELLED
                && !paymentService.hasCompletedPayment(existing.getId())) {
            return null;
        }
        if (!isLegalTransition(existing.getStatus(), status)) {
            return null;
        }
        existing.setStatus(status);
        Order saved = orderRepository.save(existing);
        notifyBuyerOfStatusChange(saved);
        return saved;
    }

    /**
     * Admins supervise every order. A vendor only speaks for the products they sell, so they are
     * admitted only when one of the order's line items is theirs.
     */
    private boolean mayActOn(Order order, UUID requesterId, Role role) {
        if (role == Role.ADMIN) {
            return true;
        }
        return orderItemRepository.existsByOrderIdAndVendorUserId(order.getId(), requesterId);
    }

    /**
     * The happy path runs forwards one step at a time, and separately allows cancellation and
     * refunding while an order is still in flight.
     *
     * <p>Being explicit about the permitted edges is the point: without this, any status could be
     * set to any other, so a cancelled order could be marked delivered and a delivered one could
     * be reopened. Terminal states have no outgoing edge, which is what makes that impossible.
     */
    private static boolean isLegalTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case PENDING -> to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED;
            case CONFIRMED -> to == OrderStatus.SHIPPED
                    || to == OrderStatus.CANCELLED
                    || to == OrderStatus.REFUNDED;
            case SHIPPED -> to == OrderStatus.DELIVERED || to == OrderStatus.REFUNDED;
            case DELIVERED, CANCELLED, REFUNDED -> false;
        };
    }

    @Override
    @Transactional
    public boolean cancel(UUID id, UUID requesterId) {
        if (id == null || requesterId == null) {
            return false;
        }
        Order existing = orderRepository.findByIdAndBuyerId(id, requesterId).orElse(null);
        if (existing == null) {
            return false;
        }
        return cancelInternal(existing);
    }

    /**
     * Marks a cancellable order cancelled and hands its stock back. Restocking happens before the
     * status change is written, and both live in the caller's transaction, so a failure part way
     * through leaves neither applied.
     */
    private boolean cancelInternal(Order existing) {
        if (!CANCELLABLE.contains(existing.getStatus())) {
            return false;
        }
        List<OrderItem> items = existing.getItems();
        if (items != null) {
            for (OrderItem item : items) {
                if (item.getProduct() != null) {
                    productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());
                }
            }
        }
        existing.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(existing);
        notifyBuyerOfStatusChange(saved);
        return true;
    }

    /**
     * Sums the order's own lines, using the prices captured at purchase. Reading them back off the
     * order rather than the cart means the total cannot drift from what the line items say.
     */
private BigDecimal totalOf(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getPriceAtPurchase() == null) {
                continue;
            }
            total = total.add(item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }

    @Override
    public BigDecimal calculateTotal(List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            if (cartItem == null || cartItem.getProduct() == null) {
                continue;
            }
            BigDecimal price = cartItem.getProduct().getPrice();
            if (price == null) {
                continue;
            }
            total = total.add(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        return total;
    }
}