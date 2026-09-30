package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.dto.AuthResponse;
import za.ac.cput.prm_marketplace.dto.LoginRequest;
import za.ac.cput.prm_marketplace.dto.RegisterRequest;
import za.ac.cput.prm_marketplace.dto.ResetPasswordRequest;
import za.ac.cput.prm_marketplace.dto.UserResponse;

public interface IAuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse verifyCode(String email, String code);

    void resendCode(String email);

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(String email, String currentPassword, String newPassword);
}