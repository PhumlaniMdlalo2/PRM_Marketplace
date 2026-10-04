package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.dto.AuthResponse;
import za.ac.cput.prm_marketplace.dto.LoginRequest;
import za.ac.cput.prm_marketplace.dto.RegisterRequest;
import za.ac.cput.prm_marketplace.dto.ResetPasswordRequest;
import za.ac.cput.prm_marketplace.dto.UserResponse;

import java.util.UUID;

public interface IAuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse verifyCode(String email, String code);

    void resendCode(String email);

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);

    /**
     * Changes the authenticated caller's own password.
     *
     * <p>The account is identified by {@code requesterId}, taken from the token, not by an email in
     * the request. Naming the target account let any authenticated caller aim the operation at
     * somebody else's login, and it turned a wrong guess into an account-enumeration oracle:
* "Account not found" and "Current password is incorrect" are different answers.
 *
 * <p>A wrong current password and an unknown account produce the same failure, so this cannot be
 * used to discover which addresses are registered.
 */
    void changePassword(UUID requesterId, String currentPassword, String newPassword);
}