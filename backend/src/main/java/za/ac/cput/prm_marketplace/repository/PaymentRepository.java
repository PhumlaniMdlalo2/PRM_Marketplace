package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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

    List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByTransactionReference(String transactionReference);

    /** Null when the payment does not exist or belongs to somebody else. */
    Optional<Payment> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
