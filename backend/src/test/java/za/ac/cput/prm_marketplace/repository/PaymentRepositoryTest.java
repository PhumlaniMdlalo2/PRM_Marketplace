package za.ac.cput.prm_marketplace.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the settlement lookup against a real database.
 *
 * <p>{@code findByOrderIdAndStatusIn} is the query that stops an order being paid twice over: the
 * service sums what it returns to work out what the order still owes, and refuses anything above it.
 * The whole property lives in that {@code in} clause. A mocked service test can only prove the
 * service asked for {@code PENDING} and {@code COMPLETED} — it cannot prove that a row which was
 * {@code REFUNDED} in the database is left out, and a query that quietly stopped filtering would let
 * a refunded order look permanently settled, or an order of R780 accept unlimited R780 attempts.
 *
 * <p>So these tests write rows in all four statuses and assert on which ones come back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    private UUID orderId;
    private UUID otherOrderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        otherOrderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("a pending and a completed payment both count as still owing")
    void findByOrderIdAndStatusIn_returnsPaymentsThatStillClaimTheMoney() {
        Payment pending = savePayment(orderId, "500.00", PaymentStatus.PENDING);
        Payment completed = savePayment(orderId, "200.00", PaymentStatus.COMPLETED);

        List<Payment> committed = committedFor(orderId);

        assertThat(committed).containsExactlyInAnyOrder(pending, completed);
        assertThat(committed).as("what this returns is what the service subtracts from the total")
                .extracting(Payment::getAmount)
                .containsExactlyInAnyOrder(new BigDecimal("500.00"), new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("a failed payment is left out, so a declined charge can be retried")
    void findByOrderIdAndStatusIn_excludesFailedPayments() {
        Payment failed = savePayment(orderId, "780.00", PaymentStatus.FAILED);
        savePayment(orderId, "780.00", PaymentStatus.PENDING);

        assertThat(committedFor(orderId))
                .as("if a failed attempt still counted, a buyer could never pay a second time")
                .doesNotContain(failed);
    }

    @Test
    @DisplayName("a refunded payment is left out, so refunded money can be paid again")
    void findByOrderIdAndStatusIn_excludesRefundedPayments() {
        Payment refunded = savePayment(orderId, "780.00", PaymentStatus.REFUNDED);

        assertThat(committedFor(orderId))
                .as("if a refund still counted, a refunded order would look settled forever")
                .doesNotContain(refunded);
    }

    @Test
    @DisplayName("an order whose every payment failed or was refunded owes its whole total again")
    void findByOrderIdAndStatusIn_isEmptyWhenNothingIsOwed() {
        savePayment(orderId, "780.00", PaymentStatus.FAILED);
        savePayment(orderId, "780.00", PaymentStatus.REFUNDED);

        assertThat(committedFor(orderId)).isEmpty();
    }

    @Test
    @DisplayName("another order's payments are not counted against this one")
    void findByOrderIdAndStatusIn_isScopedToOneOrder() {
        savePayment(otherOrderId, "780.00", PaymentStatus.PENDING);

        assertThat(committedFor(orderId))
                .as("a buyer settling one order must not be blocked by money owed on another")
                .isEmpty();
    }

    @Test
    @DisplayName("an order with no payments at all is empty rather than an error")
    void findByOrderIdAndStatusIn_isEmptyForAnUnpaidOrder() {
        assertThat(committedFor(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("asking for a status nothing has does not return the whole table")
    void findByOrderIdAndStatusIn_honoursTheStatusesItIsGiven() {
        savePayment(orderId, "780.00", PaymentStatus.PENDING);
        savePayment(orderId, "780.00", PaymentStatus.COMPLETED);

        assertThat(paymentRepository.findByOrderIdAndStatusIn(orderId, List.of(PaymentStatus.COMPLETED)))
                .extracting(Payment::getStatus)
                .containsExactly(PaymentStatus.COMPLETED);
    }

    private List<Payment> committedFor(UUID id) {
        return paymentRepository.findByOrderIdAndStatusIn(id,
                List.of(PaymentStatus.PENDING, PaymentStatus.COMPLETED));
    }

    private Payment savePayment(UUID order, String amount, PaymentStatus status) {
        return paymentRepository.save(new Payment.Builder()
                .setOrderId(order)
                .setUserId(UUID.randomUUID())
                .setAmount(new BigDecimal(amount))
                .setMethod(PaymentMethod.CARD)
                .setStatus(status)
                .setTransactionReference("PAY-" + UUID.randomUUID())
                .setCreatedAt(LocalDateTime.now())
                .build());
    }
}