package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IEmailService {

    void sendVerificationCode(String to, String code);

    void sendPasswordResetToken(String to, String token);

    void sendOrderConfirmation(String to, String orderReference, BigDecimal total,
                               String fulfillmentMethod, String deliveryAddress,
                               LocalDate estimatedDeliveryDate, List<String> paymentReferences,
                               String trackingUrl);

    void sendOrderStatusUpdate(String to, String orderReference, OrderStatus status,
                               String fulfillmentMethod, LocalDate estimatedDeliveryDate,
                               String trackingUrl);
}