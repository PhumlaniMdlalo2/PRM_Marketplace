package za.ac.cput.prm_marketplace.dto;

import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentInstruction(
        UUID paymentId,
        UUID orderId,
        UUID sellerUserId,
        String sellerName,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        String transactionReference,
        SellerPayoutDetails payoutDetails
) {
}
