package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Owner-scoped lookups. Every read the API performs goes through one of these rather than
 * {@code findById}, because a payment belongs to exactly one account and a bare id read would
 * let any authenticated caller read anyone's payment history.
 */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderId(UUID orderId);

    /**
     * The payments against one order that still represent money owed or money taken.
     *
     * <p>PENDING counts as well as COMPLETED: a pending payment is a claim on the order's total, and
     * leaving it out would let a buyer file unlimited attempts at the full amount. FAILED and REFUNDED
     * are excluded, which is what lets a buyer retry after a failed charge or a refund without the
     * old row still counting against the ceiling.
     */
    @Query("select p from Payment p "
            + "where p.orderId = :orderId and p.status in :statuses")
    List<Payment> findByOrderIdAndStatusIn(@Param("orderId") UUID orderId,
                                           @Param("statuses") List<PaymentStatus> statuses);

    List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Payment> findByStatus(PaymentStatus status);

    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);

    boolean existsByOrderId(UUID orderId);

    long countByOrderIdAndStatusNot(UUID orderId, PaymentStatus status);

    List<Payment> findBySellerUserIdOrderByCreatedAtDesc(UUID sellerUserId);

    Optional<Payment> findByTransactionReference(String transactionReference);

    /** Null when the payment does not exist or belongs to somebody else. */
    Optional<Payment> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
