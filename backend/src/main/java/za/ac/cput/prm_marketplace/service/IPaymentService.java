package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;
import za.ac.cput.prm_marketplace.domain.Role;

import java.util.List;
import java.util.UUID;

/**
 * Payments are scoped to the account that owns them. Every method that can reach a stored payment
 * takes the caller, because a payment id is guessable in the sense that it does not belong to the
 * person holding it: reading by id alone would expose another account's payment history.
 *
 * <p>Two operations that used to exist were removed rather than restricted:
 * <ul>
 *   <li>listing every payment in the system, which returned all customers' payments to anyone;</li>
 *   <li>deleting a payment, which is a financial record and belongs in the status lifecycle.</li>
 * </ul>
 */
public interface IPaymentService {

    /**
     * Records a payment attempt against one of the caller's own orders.
     *
     * <p>The payer is the caller, the amount defaults to the order total and may not exceed it,
     * and the status, reference and timestamps are set by the server. Returns null when the order
     * is missing, is not the caller's, or the amount is unusable.
     */
    Payment create(Payment payment, UUID requesterId);

    /** The caller's own payment, or null if it does not exist or is somebody else's. */
    Payment read(UUID id, UUID requesterId);

    /**
     * Updates the payment method on a PENDING payment of the caller's own.
     *
     * <p>The payment to change is named by {@code id} from the path rather than from the body,
     * which is ignored. The amount is deliberately not editable here: the order total is
     * authoritative, and a client that could lower the amount of a payment it has not made yet
     * could pay a fraction of the order. Returns null when the payment is missing, not owned by
     * the caller, or no longer PENDING.
     */
    Payment update(UUID id, Payment payment, UUID requesterId);

    /** The caller's own payments, most recent first. */
    List<Payment> getByUserId(UUID requesterId);

    /**
     * The payments attached to an order, but only when that order belongs to the caller.
     * Returns an empty list otherwise, which is how a caller cannot probe for order existence.
     */
    List<Payment> getByOrderId(UUID orderId, UUID requesterId);

    /**
     * Moves one of the caller's own payments through the lifecycle
     * (PENDING -> COMPLETED/FAILED, FAILED -> PENDING, COMPLETED -> REFUNDED) and notifies the payer.
     *
     * <p>Only FACULTY may settle a payment: everything out of PENDING (COMPLETED, FAILED, REFUNDED)
     * requires that role, even though the payment is the caller's own. The payer cannot mark their
     * own attempt as paid or as failed. Re-attempting a payment, which is the PENDING move, stays
     * open to everyone because it asserts nothing about money that has moved.
     *
     * <p>Consequence worth being explicit about: with no payment gateway wired up, payments stay at
     * PENDING and orders do not advance on their own. Faculty settle them by hand until a gateway
     * callback replaces them.
     *
     * <p>Returns null when the payment is missing, is not the caller's, the transition is not
     * allowed, or the status requires a role the caller lacks.
     */
    Payment updateStatus(UUID id, PaymentStatus status, UUID requesterId, Role requesterRole);
}