package za.ac.cput.prm_marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.NotificationType;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;
import za.ac.cput.prm_marketplace.domain.PaymentSimulationOutcome;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.dto.PaymentInstruction;
import za.ac.cput.prm_marketplace.dto.SellerPayoutDetails;
import za.ac.cput.prm_marketplace.exception.ConflictException;
import za.ac.cput.prm_marketplace.factory.PaymentFactory;
import za.ac.cput.prm_marketplace.repository.OrderItemRepository;
import za.ac.cput.prm_marketplace.repository.OrderRepository;
import za.ac.cput.prm_marketplace.repository.PaymentRepository;
import za.ac.cput.prm_marketplace.repository.VendorProfileRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements IPaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final INotificationService notificationService;
    @Value("${app.payments.simulation.enabled:false}")
    private boolean simulationEnabled;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderRepository orderRepository,
                              OrderItemRepository orderItemRepository,
                              VendorProfileRepository vendorProfileRepository,
                              INotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.vendorProfileRepository = vendorProfileRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Payment create(Payment payment, UUID requesterId) {
        if (payment == null || requesterId == null) {
            return null;
        }
        if (payment.getMethod() == PaymentMethod.SANDBOX && !simulationEnabled) {
            return null;
        }

        // The order has to belong to the caller. Resolving it through the owner-scoped finder is
        // what stops a payment being filed against somebody else's order.
        Order order = orderRepository.findByIdAndBuyerId(payment.getOrderId(), requesterId).orElse(null);
        if (order == null) {
            return null;
        }

        // The ceiling is per order, not per payment. Without this, an order of R450 accepts any number of
        // payments of R450 and admin settling all of them takes R2250 for goods worth R450. What is
        // left of the total is what the next attempt may cover, so a retry settles a shortfall rather
        // than the whole total again.
        BigDecimal amount = resolveAmount(payment.getAmount(), remainingFor(order));
        if (amount == null) {
            return null;
        }

        // The factory validates the remaining input and sets status = PENDING, a unique
        // transaction reference and createdAt. requesterId is passed as the payer, never the one
        // the client supplied.
        Payment validated = PaymentFactory.createPayment(
                order.getId(), requesterId, amount, payment.getMethod());

        if (validated == null) {
            return null;
        }
        return paymentRepository.save(validated);
    }

    @Override
    @Transactional
    public Payment createForSeller(UUID orderId, PaymentMethod method, UUID buyerId, UUID sellerUserId) {
        if (orderId == null || buyerId == null || sellerUserId == null
                || (method != PaymentMethod.EFT && method != PaymentMethod.CASH_ON_PICKUP)) {
            return null;
        }
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId).orElse(null);
        if (order == null) {
            return null;
        }
        VendorProfile profile = vendorProfileRepository.findByUserId(sellerUserId).orElse(null);
        if (profile == null || !profile.isVerified()) {
            return null;
        }
        if (method == PaymentMethod.EFT && !hasPayoutDetails(profile)) {
            throw new ConflictException(
                    "A seller has not saved complete EFT details. Choose cash on pickup or contact the seller.");
        }

        BigDecimal sellerTotal = orderItemRepository.findByOrderId(orderId).stream()
                .filter(item -> sellerUserId.equals(sellerId(item)))
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amount = resolveAmount(sellerTotal, remainingFor(order));
        if (amount == null || amount.compareTo(sellerTotal) != 0) {
            return null;
        }
        Payment validated = PaymentFactory.createPayment(orderId, buyerId, amount, method);
        if (validated == null) {
            return null;
        }
        Payment assigned = new Payment.Builder()
                .copy(validated)
                .setSellerUserId(sellerUserId)
                .build();
        return paymentRepository.save(assigned);
    }

    private static UUID sellerId(OrderItem item) {
        return item.getProduct() == null || item.getProduct().getVendor() == null
                || item.getProduct().getVendor().getUser() == null
                ? null
                : item.getProduct().getVendor().getUser().getId();
    }

    private static boolean hasPayoutDetails(VendorProfile profile) {
        return hasText(profile.getPayoutAccountHolder())
                && hasText(profile.getPayoutBankName())
                && hasText(profile.getPayoutAccountNumber())
                && hasText(profile.getPayoutBranchCode())
                && hasText(profile.getPayoutAccountType());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Turns the requested amount into one that may actually be filed against the order.
     *
     * <p>An omitted amount means "settle whatever is left", which is why it resolves to the remaining
     * balance rather than to the order total: on an order that is already part paid, paying the total
     * again is exactly what this is here to prevent.
     *
     * <p>Returns null for everything the caller may not file: an order that is already covered, a
     * non-positive amount, and an amount larger than what is outstanding.
     */
    private static BigDecimal resolveAmount(BigDecimal requested, BigDecimal remaining) {
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        if (requested == null) {
            return remaining;
        }

        if (requested.compareTo(BigDecimal.ZERO) <= 0 || requested.compareTo(remaining) > 0) {
            return null;
        }
        return requested.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * What the order still owes: its total less the payments already committed to it.
     *
     * <p>PENDING counts alongside COMPLETED, because an attempt the buyer has filed and admin has
     * not yet ruled on is still a claim on the money. A buyer who abandons an attempt therefore needs
     * admin to settle it as FAILED before another can be filed, which is the same manual step the
     * payment status already requires with no gateway attached.
     *
     * <p>Read in the caller's transaction and not enforced by a single statement, so two payments
     * filed at the same instant can both pass this check. That needs a concurrent double submission
     * from one account, and the honest fix is a locking read or a running total on the order rather
     * than a subtler version of the same arithmetic here.
     */
    private BigDecimal remainingFor(Order order) {
        BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        BigDecimal committed = BigDecimal.ZERO;
        for (Payment existing : paymentRepository.findByOrderIdAndStatusIn(order.getId(),
                List.of(PaymentStatus.PENDING, PaymentStatus.COMPLETED))) {
            if (existing.getAmount() != null) {
                committed = committed.add(existing.getAmount());
            }
        }
        BigDecimal remaining = total.subtract(committed);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    @Override
    public Payment read(UUID id, UUID requesterId) {
        if (id == null || requesterId == null) {
            return null;
        }
        return paymentRepository.findByIdAndUserId(id, requesterId).orElse(null);
    }

    @Override
    @Transactional
    public Payment update(UUID id, Payment payment, UUID requesterId) {
        if (id == null || payment == null || requesterId == null) {
            return null;
        }

        Payment existing = paymentRepository.findByIdAndUserId(id, requesterId).orElse(null);
        if (existing == null || existing.getStatus() != PaymentStatus.PENDING) {
            return null;
        }

        // Only the method moves. The amount, payer, status and reference are carried over from the
        // stored row, so a request body cannot rewrite any of them.
        Payment updated = new Payment.Builder()
                .copy(existing)
                .setMethod(payment.getMethod() != null ? payment.getMethod() : existing.getMethod())
                .build();

        return paymentRepository.save(updated);
    }

    @Override
    public List<Payment> getByUserId(UUID requesterId) {
        if (requesterId == null) {
            return Collections.emptyList();
        }
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(requesterId);
    }

    @Override
    public List<Payment> getByOrderId(UUID orderId, UUID requesterId) {
        if (orderId == null || requesterId == null) {
            return Collections.emptyList();
        }
        // Confirm the order belongs to the caller before returning anything attached to it,
        // otherwise this is a readable payment history for any order id.
        if (!orderRepository.existsByIdAndBuyerId(orderId, requesterId)) {
            return Collections.emptyList();
        }
        return paymentRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentInstruction> getPaymentInstructions(UUID orderId, UUID requesterId) {
        if (orderId == null || requesterId == null
                || !orderRepository.existsByIdAndBuyerId(orderId, requesterId)) {
            return List.of();
        }
        return paymentRepository.findByOrderId(orderId).stream()
                .map(payment -> toInstruction(payment, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentInstruction> getSellerPayments(UUID sellerUserId, Role role) {
        if (sellerUserId == null || !isSellerRole(role)) {
            return List.of();
        }
        return paymentRepository.findBySellerUserIdOrderByCreatedAtDesc(sellerUserId).stream()
                .map(payment -> toInstruction(payment, false))
                .toList();
    }

    private PaymentInstruction toInstruction(Payment payment, boolean revealPayoutDetails) {
        VendorProfile profile = payment.getSellerUserId() == null ? null
                : vendorProfileRepository.findByUserId(payment.getSellerUserId()).orElse(null);
        SellerPayoutDetails payout = revealPayoutDetails && payment.getMethod() == PaymentMethod.EFT
                && profile != null
                ? new SellerPayoutDetails(profile.getPayoutAccountHolder(), profile.getPayoutBankName(),
                        profile.getPayoutAccountNumber(), profile.getPayoutBranchCode(),
                        profile.getPayoutAccountType())
                : null;
        return new PaymentInstruction(payment.getId(), payment.getOrderId(), payment.getSellerUserId(),
                profile == null ? "Marketplace seller" : profile.getBusinessName(),
                payment.getAmount(), payment.getMethod(), payment.getStatus(),
                payment.getTransactionReference(), payout);
    }

    @Override
    public boolean isSimulationEnabled() {
        return simulationEnabled;
    }

    @Override
    public boolean hasCompletedPayment(UUID orderId) {
        return orderId != null
                && paymentRepository.existsByOrderId(orderId)
                && paymentRepository.countByOrderIdAndStatusNot(orderId, PaymentStatus.COMPLETED) == 0;
    }

    @Override
    @Transactional
    public Payment simulate(UUID id, PaymentSimulationOutcome outcome, UUID requesterId) {
        if (!simulationEnabled || id == null || outcome == null || requesterId == null) {
            return null;
        }

        Payment existing = paymentRepository.findByIdAndUserId(id, requesterId).orElse(null);
        if (existing == null
                || existing.getMethod() != PaymentMethod.SANDBOX
                || existing.getStatus() != PaymentStatus.PENDING) {
            return null;
        }

        PaymentStatus status = outcome == PaymentSimulationOutcome.SUCCESS
                ? PaymentStatus.COMPLETED
                : PaymentStatus.FAILED;
        Payment updated = new Payment.Builder()
                .copy(existing)
                .setStatus(status)
                .setPaidAt(status == PaymentStatus.COMPLETED ? LocalDateTime.now() : null)
                .build();
        Payment saved = paymentRepository.save(updated);
        notifyUser(saved);
        return saved;
    }

    @Override
    @Transactional
    public Payment confirmReceipt(UUID id, UUID sellerUserId, Role role) {
        if (id == null || sellerUserId == null || !isSellerRole(role)) {
            return null;
        }
        Payment existing = paymentRepository.findById(id).orElse(null);
        if (existing == null || !sellerUserId.equals(existing.getSellerUserId())
                || existing.getStatus() != PaymentStatus.PENDING
                || (existing.getMethod() != PaymentMethod.EFT
                && existing.getMethod() != PaymentMethod.CASH_ON_PICKUP)
                || orderIsCancelled(existing.getOrderId())) {
            return null;
        }
        Payment saved = paymentRepository.save(new Payment.Builder()
                .copy(existing)
                .setStatus(PaymentStatus.COMPLETED)
                .setPaidAt(LocalDateTime.now())
                .build());
        notifyUser(saved);
        return saved;
    }

    private boolean orderIsCancelled(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        return order == null || order.getStatus() == za.ac.cput.prm_marketplace.domain.OrderStatus.CANCELLED
                || order.getStatus() == za.ac.cput.prm_marketplace.domain.OrderStatus.REFUNDED;
    }

    private static boolean isSellerRole(Role role) {
        return role == Role.STUDENT || role == Role.VENDOR;
    }

    @Override
    @Transactional
public Payment updateStatus(UUID id, PaymentStatus status, UUID requesterId, Role requesterRole) {
        if (id == null || status == null || requesterId == null) {
            return null;
        }

        // Authority is checked before the payment is even loaded. A caller with no business settling
        // this payment gets the same answer whether or not the id exists, and a null role is not
        // admin rather than being quietly treated as one.
        if (requiresAdmin(status) && requesterRole != Role.ADMIN) {
            return null;
        }

        Payment existing = paymentRepository.findByIdAndUserId(id, requesterId).orElse(null);
        if (existing == null || !isValidTransition(existing.getStatus(), status)) {
            return null;
        }

        LocalDateTime paidAt = status == PaymentStatus.COMPLETED
                ? LocalDateTime.now()
                : existing.getPaidAt();

        Payment updated = new Payment.Builder()
                .copy(existing)
                .setStatus(status)
                .setPaidAt(paidAt)
                .build();

        Payment saved = paymentRepository.save(updated);
        notifyUser(saved);
        return saved;
    }

    /**
     * Whether settling the payment is the business's decision rather than the payer's.
     *
     * <p>Settling a payment is the one transition the payer must not make for themselves. The
     * ownership check already stops a buyer settling somebody else's payment, which left the case
     * that actually mattered: a buyer PATCHing their own PENDING payment straight to COMPLETED and
     * then acting on an order they never paid for. Nothing in the request distinguishes "the gateway
     * confirmed this" from "I am asserting that it did", so the endpoint refuses to take the word of
     * whoever the money is attached to.
     *
     * <p>FAILED is gated alongside COMPLETED deliberately: a buyer who can mark their own attempt as
     * failed can erase a completed charge they regret, so both directions out of PENDING are closed
     * together. PENDING is left open to everyone, since retrying your own attempt asserts nothing
     * about money that has moved. REFUNDED was already admin-only.
     *
     * <p>The consequence is that with no payment gateway wired up, payments sit at PENDING and orders
     * do not advance on their own. That is the honest state for a marketplace with no processor; the
     * alternative is a checkout that looks paid and is not. A gateway callback that calls this same
     * method with admin authority replaces the manual step without any other change here.
     */
    private static boolean requiresAdmin(PaymentStatus status) {
        return status != PaymentStatus.PENDING;
    }

    private static boolean isValidTransition(PaymentStatus from, PaymentStatus to) {
        switch (from) {
            case PENDING:
                return to == PaymentStatus.COMPLETED || to == PaymentStatus.FAILED;
            case FAILED:
                return to == PaymentStatus.PENDING;
            case COMPLETED:
                return to == PaymentStatus.REFUNDED;
            default:
                return false;
        }
    }

    private void notifyUser(Payment payment) {
        String title;
        String message;
        String amount = "R" + payment.getAmount().toPlainString();

        switch (payment.getStatus()) {
            case COMPLETED:
                title = "Payment successful";
                message = "Your payment of " + amount + " for order " + payment.getOrderId()
                        + " was successful. Reference: " + payment.getTransactionReference() + ".";
                break;
            case FAILED:
                title = "Payment failed";
                message = "Your payment of " + amount + " for order " + payment.getOrderId()
                        + " could not be processed. Please try again.";
                break;
            case REFUNDED:
                title = "Payment refunded";
                message = "Your payment of " + amount + " for order " + payment.getOrderId()
                        + " has been refunded. Reference: " + payment.getTransactionReference() + ".";
                break;
            default:
                return; // nothing to tell the user when a payment goes back to PENDING
        }

        // A failed notification must never undo or hide a payment that was already saved.
        try {
            notificationService.send(payment.getUserId(), NotificationType.PAYMENT, title, message);
        } catch (RuntimeException e) {
            log.warn("Could not send payment notification for payment {}", payment.getId(), e);
        }
    }
}