package za.ac.cput.prm_marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import za.ac.cput.prm_marketplace.domain.NotificationType;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;
import za.ac.cput.prm_marketplace.factory.PaymentFactory;
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
    private final INotificationService notificationService;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              INotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
    }

    @Override
    public Payment create(Payment payment) {
        if (payment == null) {
            return null;
        }

        // The factory validates the input and sets status = PENDING,
        // a transaction reference and createdAt.
        Payment validated = PaymentFactory.createPayment(
                payment.getOrderId(), payment.getUserId(),
                payment.getAmount(), payment.getMethod());

        if (validated == null) {
            return null;
        }
        return paymentRepository.save(validated);
    }

    @Override
    public Payment read(UUID id) {
        if (id == null) {
            return null;
        }
        return paymentRepository.findById(id).orElse(null);
    }

    @Override
    public Payment update(Payment payment) {
        if (payment == null || payment.getId() == null) {
            return null;
        }

        Payment existing = paymentRepository.findById(payment.getId()).orElse(null);

        if (existing == null || existing.getStatus() != PaymentStatus.PENDING) {
            return null;
        }

        BigDecimal amount = existing.getAmount();
        if (payment.getAmount() != null && payment.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            amount = payment.getAmount().setScale(2, RoundingMode.HALF_UP);
        }

        PaymentMethod method = payment.getMethod() != null
                ? payment.getMethod()
                : existing.getMethod();

        Payment updated = new Payment.Builder()
                .copy(existing)
                .setAmount(amount)
                .setMethod(method)
                .build();

        return paymentRepository.save(updated);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !paymentRepository.existsById(id)) {
            return false;
        }

        paymentRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Payment> getAll() {
        return paymentRepository.findAll();
    }

    @Override
    public List<Payment> getByOrderId(UUID orderId) {
        if (orderId == null) {
            return Collections.emptyList();
        }
        return paymentRepository.findByOrderId(orderId);
    }

    @Override
    public List<Payment> getByUserId(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return paymentRepository.findByUserId(userId);
    }

    @Override
    public Payment updateStatus(UUID id, PaymentStatus status) {
        if (id == null || status == null) {
            return null;
        }

        Payment existing = paymentRepository.findById(id).orElse(null);

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

