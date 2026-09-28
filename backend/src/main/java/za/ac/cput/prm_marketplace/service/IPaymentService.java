package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;

import java.util.List;
import java.util.UUID;

public interface IPaymentService {

    Payment create(Payment payment);

    Payment read(UUID id);

    /**
     * Only a PENDING payment can be edited (method / amount).
     * Use {@link #updateStatus(UUID, PaymentStatus)} to move a payment through its lifecycle.
     */
    Payment update(Payment payment);

    boolean delete(UUID id);

    List<Payment> getAll();

    List<Payment> getByOrderId(UUID orderId);

    List<Payment> getByUserId(UUID userId);

    /**
     * Moves a payment to a new status if the transition is allowed
     * (PENDING -> COMPLETED/FAILED, FAILED -> PENDING, COMPLETED -> REFUNDED)
     * and notifies the user. Returns null if the payment does not exist
     * or the transition is not allowed.
     */
    Payment updateStatus(UUID id, PaymentStatus status);
}
