package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
    @DisplayName("send: swallows transport failures so auth flows are not broken by a missing SMTP server")
    void send_transportFailure_doesNotPropagate() {
        doThrow(new org.springframework.mail.MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        EmailServiceImpl service = new EmailServiceImpl(mailSender, "noreply@prm.local");

        assertThatCode(() -> service.sendVerificationCode("jane@example.com", "123456"))
                .doesNotThrowAnyException();
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
}