package za.ac.cput.prm_marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import za.ac.cput.prm_marketplace.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailServiceImpl implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${app.mail.from:noreply@prm-marketplace.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendVerificationCode(String to, String code) {
        send(to, "Verify your PRM Marketplace account",
                "Your verification code is " + code + ". It expires in 15 minutes.", true);
    }

    @Override
    public void sendPasswordResetToken(String to, String token) {
        send(to, "Reset your PRM Marketplace password",
                "Use this token to reset your password: " + token + ". It expires in 60 minutes.", false);
    }

    @Override
    public void sendOrderConfirmation(String to, String orderReference, BigDecimal total,
                                      String fulfillmentMethod, String deliveryAddress,
                                      LocalDate estimatedDeliveryDate, List<String> paymentReferences,
                                      String trackingUrl) {
        String fulfillment = "DELIVERY".equals(fulfillmentMethod) ? "Delivery" : "Meetup";
        String estimate = estimatedDeliveryDate == null
                ? "Arrange a meetup time with the seller through marketplace messages."
                : "Estimated delivery by " + estimatedDeliveryDate + ". This is an estimate, not live courier tracking.";
        String address = deliveryAddress == null || deliveryAddress.isBlank()
                ? ""
                : "\nDelivery address: " + deliveryAddress;
        String references = paymentReferences == null || paymentReferences.isEmpty()
                ? "Not available"
                : paymentReferences.stream().collect(Collectors.joining(", "));
        String body = """
                Thank you for your order with PRM Marketplace.

                Order reference: %s
                Total: R%s
                Fulfillment: %s%s
                %s
                Payment reference(s): %s

                Track your order and view updates: %s
                """.formatted(orderReference, total == null ? "0.00" : total.toPlainString(),
                fulfillment, address, estimate, references, trackingUrl);
        send(to, "Order confirmed: " + orderReference, body, false);
    }

    @Override
    public void sendOrderStatusUpdate(String to, String orderReference, OrderStatus status,
                                      String fulfillmentMethod, LocalDate estimatedDeliveryDate,
                                      String trackingUrl) {
        String stage = switch (status) {
            case PENDING -> "Order placed";
            case CONFIRMED -> "Seller confirmed your order";
            case SHIPPED -> "DELIVERY".equals(fulfillmentMethod)
                    ? "Your order is on its way"
                    : "Your order is ready for meetup";
            case DELIVERED -> "DELIVERY".equals(fulfillmentMethod)
                    ? "Your order was delivered"
                    : "Your meetup order was collected";
            case CANCELLED -> "Your order was cancelled";
            case REFUNDED -> "Your order was refunded";
        };
        String estimate = estimatedDeliveryDate == null
                ? ""
                : "\nCurrent estimated delivery date: " + estimatedDeliveryDate + ".";
        String body = """
                %s.

                Order reference: %s%s

                View the latest order progress: %s
                """.formatted(stage, orderReference, estimate, trackingUrl);
        send(to, "Order update: " + orderReference, body, false);
    }

    private void send(String to, String subject, String body, boolean propagateFailure) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (RuntimeException ex) {
            log.error("Failed to send email to {}", to, ex);
            if (propagateFailure) throw ex;
        }
    }
}