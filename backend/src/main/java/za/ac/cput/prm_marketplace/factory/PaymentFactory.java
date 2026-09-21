package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentFactory {

    public static Payment createPayment(UUID orderId, UUID userId,
                                        BigDecimal amount, PaymentMethod method) {

        if (orderId == null) {
            return null;
        }

        if (userId == null) {
            return null;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        if (method == null) {
            return null;
        }

        return new Payment.Builder()
                .setOrderId(orderId)
                .setUserId(userId)
                .setAmount(amount.setScale(2, RoundingMode.HALF_UP))
                .setMethod(method)
                .setStatus(PaymentStatus.PENDING)
                .setTransactionReference(generateReference())
                .setCreatedAt(LocalDateTime.now())
                .build();
    }

    private static String generateReference() {
        return "PAY-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase();
    }
}

