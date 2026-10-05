package za.ac.cput.prm_marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.NotificationType;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.factory.PaymentFactory;
import za.ac.cput.prm_marketplace.repository.OrderRepository;
import za.ac.cput.prm_marketplace.repository.PaymentRepository;

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
    private final INotificationService notificationService;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderRepository orderRepository,
                              INotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Payment create(Payment payment, UUID requesterId) {
        if (payment == null || requesterId == null) {
            return null;
        }

        // The order has to belong to the caller. Resolving it through the owner-scoped finder is
        // what stops a payment being filed against somebody else's order.
        Order order = orderRepository.findByIdAndBuyerId(payment.getOrderId(), requesterId).orElse(null);
        if (order == null) {
            return null;
        }

        BigDecimal amount = resolveAmount(payment.getAmount(), order);
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

    /**
     * An omitted amount means "pay the whole order". A supplied amount is accepted up to the order
     * total, so a client cannot inflate a payment beyond what it owes.
     *
     * <p>This is a ceiling rather than an exact match on purpose. One order can end up carrying
     * several payments, and nothing in the model records how much has already been paid, so
     * demanding the exact total would make partial payments impossible to express. Tightening this
     * to an exact match belongs with the order/settlement work that tracks amounts paid.
     */
    private static BigDecimal resolveAmount(BigDecimal requested, Order order) {
        BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();

        if (requested == null) {
            return total.compareTo(BigDecimal.ZERO) > 0 ? total : null;
        }

        if (requested.compareTo(BigDecimal.ZERO) <= 0 || requested.compareTo(total) > 0) {
            return null;
        }
        return requested.setScale(2, RoundingMode.HALF_UP);
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
    @Transactional
public Payment updateStatus(UUID id, PaymentStatus status, UUID requesterId, Role requesterRole) {
        if (id == null || status == null || requesterId == null) {
            return null;
        }

        // Authority is checked before the payment is even loaded. A caller with no business settling
        // this payment gets the same answer whether or not the id exists, and a null role is not
        // faculty rather than being quietly treated as one.
        if (requiresFaculty(status) && requesterRole != Role.FACULTY) {
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
     * about money that has moved. REFUNDED was already faculty-only.
     *
     * <p>The consequence is that with no payment gateway wired up, payments sit at PENDING and orders
     * do not advance on their own. That is the honest state for a marketplace with no processor; the
     * alternative is a checkout that looks paid and is not. A gateway callback that calls this same
     * method with faculty authority replaces the manual step without any other change here.
     */
    private static boolean requiresFaculty(PaymentStatus status) {
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