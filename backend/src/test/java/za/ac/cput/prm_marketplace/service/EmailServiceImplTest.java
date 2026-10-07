package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import za.ac.cput.prm_marketplace.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    @DisplayName("sendVerificationCode: sends a mail carrying the code")
    void sendVerificationCode_sendsCode() {
        EmailServiceImpl service = new EmailServiceImpl(mailSender, "noreply@prm.local");

        service.sendVerificationCode("jane@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("jane@example.com");
        assertThat(sent.getFrom()).isEqualTo("noreply@prm.local");
        assertThat(sent.getSubject()).contains("Verify");
        assertThat(sent.getText()).contains("123456");
    }

    @Test
    @DisplayName("sendPasswordResetToken: sends a mail carrying the token")
    void sendPasswordResetToken_sendsToken() {
        EmailServiceImpl service = new EmailServiceImpl(mailSender, "noreply@prm.local");

        service.sendPasswordResetToken("jane@example.com", "token-abc");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains("token-abc");
    }

    @Test
    @DisplayName("sendVerificationCode: propagates transport failures so callers know delivery failed")
    void sendVerificationCode_transportFailure_propagates() {
        doThrow(new org.springframework.mail.MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        EmailServiceImpl service = new EmailServiceImpl(mailSender, "noreply@prm.local");

        assertThatThrownBy(() -> service.sendVerificationCode("jane@example.com", "123456"))
                .isInstanceOf(org.springframework.mail.MailSendException.class);
    }

    @Test
    @DisplayName("sendPasswordResetToken: keeps transport failures from revealing account state")
    void sendPasswordResetToken_transportFailure_doesNotPropagate() {
        doThrow(new org.springframework.mail.MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        EmailServiceImpl service = new EmailServiceImpl(mailSender, "noreply@prm.local");

        service.sendPasswordResetToken("jane@example.com", "token-abc");
    }

    @Test
    @DisplayName("send: uses the configured from address")
    void send_usesConfiguredFromAddress() {
        EmailServiceImpl service = new EmailServiceImpl(mailSender, "no-reply@student.marketplace.ac.za");

        service.sendVerificationCode("jane@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isEqualTo("no-reply@student.marketplace.ac.za");
    }

    @Test
    @DisplayName("order confirmation includes reference, delivery choice, address and tracking")
    void sendOrderConfirmation_includesOrderDetails() {
        EmailServiceImpl service = new EmailServiceImpl(mailSender, "noreply@prm.local");

        service.sendOrderConfirmation("jane@example.com", "order-123", new BigDecimal("475.00"),
                "DELIVERY", "12 Main Road, Cape Town", LocalDate.of(2026, 10, 14),
                List.of("PAY-SELLER-123"), "https://market.example/orders/order-123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).contains("order-123");
        assertThat(captor.getValue().getText())
                .contains("order-123", "R475.00", "Delivery", "12 Main Road",
                        "PAY-SELLER-123", "2026-10-14", "https://market.example/orders/order-123");
    }

    @Test
    @DisplayName("meetup confirmation explains the seller arrangement")
    void sendOrderConfirmation_meetupMentionsArrangement() {
        EmailServiceImpl service = new EmailServiceImpl(mailSender, "noreply@prm.local");

        service.sendOrderConfirmation("jane@example.com", "order-456", new BigDecimal("20.00"),
                "MEETUP", null, null, List.of("PAY-SELLER-456"),
                "https://market.example/orders/order-456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText())
                .contains("Meetup", "Arrange a meetup time", "PAY-SELLER-456");
    }

    @Test
    @DisplayName("status updates include the marketplace stage and tracking link")
    void sendOrderStatusUpdate_includesTrackingDetails() {
        EmailServiceImpl service = new EmailServiceImpl(mailSender, "noreply@prm.local");

        service.sendOrderStatusUpdate("jane@example.com", "order-789", OrderStatus.SHIPPED,
                "DELIVERY", LocalDate.of(2026, 10, 14),
                "https://market.example/orders/order-789");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText())
                .contains("on its way", "order-789", "2026-10-14",
                        "https://market.example/orders/order-789");
    }
}