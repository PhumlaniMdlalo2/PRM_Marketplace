package za.ac.cput.prm_marketplace.factory;

import org.junit.jupiter.api.Test;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentFactoryTest {

    private final UUID orderId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void createsPaymentWithValidFields() {
        Payment payment = PaymentFactory.createPayment(orderId, userId,
                new BigDecimal("780"), PaymentMethod.CARD);

        assertThat(payment).isNotNull();
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getUserId()).isEqualTo(userId);
        assertThat(payment.getAmount()).isEqualByComparingTo("780.00");
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getTransactionReference()).startsWith("PAY-");
        assertThat(payment.getCreatedAt()).isNotNull();
        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    void generatesDifferentReferencesForDifferentPayments() {
        Payment first = PaymentFactory.createPayment(orderId, userId, BigDecimal.TEN, PaymentMethod.EFT);
        Payment second = PaymentFactory.createPayment(orderId, userId, BigDecimal.TEN, PaymentMethod.EFT);

        assertThat(first.getTransactionReference()).isNotEqualTo(second.getTransactionReference());
    }

    @Test
    void returnsNullWhenOrderIdIsNull() {
        assertThat(PaymentFactory.createPayment(null, userId, BigDecimal.TEN, PaymentMethod.CARD)).isNull();
    }

    @Test
    void returnsNullWhenUserIdIsNull() {
        assertThat(PaymentFactory.createPayment(orderId, null, BigDecimal.TEN, PaymentMethod.CARD)).isNull();
    }

    @Test
    void returnsNullWhenAmountIsNullZeroOrNegative() {
        assertThat(PaymentFactory.createPayment(orderId, userId, null, PaymentMethod.CARD)).isNull();
        assertThat(PaymentFactory.createPayment(orderId, userId, BigDecimal.ZERO, PaymentMethod.CARD)).isNull();
        assertThat(PaymentFactory.createPayment(orderId, userId, new BigDecimal("-5"), PaymentMethod.CARD)).isNull();
    }

    @Test
    void returnsNullWhenMethodIsNull() {
        assertThat(PaymentFactory.createPayment(orderId, userId, BigDecimal.TEN, null)).isNull();
    }
}
