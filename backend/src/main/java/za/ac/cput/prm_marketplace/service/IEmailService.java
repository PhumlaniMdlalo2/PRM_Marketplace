package za.ac.cput.prm_marketplace.service;

public interface IEmailService {

    void sendVerificationCode(String to, String code);

    void sendPasswordResetToken(String to, String token);
}