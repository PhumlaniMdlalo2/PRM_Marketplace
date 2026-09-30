package za.ac.cput.prm_marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${spring.mail.username:noreply@prm-marketplace.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendVerificationCode(String to, String code) {
        send(to, "Verify your PRM Marketplace account",
                "Your verification code is " + code + ". It expires in 15 minutes.");
    }

    @Override
    public void sendPasswordResetToken(String to, String token) {
        send(to, "Reset your PRM Marketplace password",
                "Use this token to reset your password: " + token + ". It expires in 60 minutes.");
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (RuntimeException ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }
}