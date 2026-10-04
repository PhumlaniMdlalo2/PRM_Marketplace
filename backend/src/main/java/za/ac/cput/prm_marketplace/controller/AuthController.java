package za.ac.cput.prm_marketplace.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.dto.AuthResponse;
import za.ac.cput.prm_marketplace.dto.ChangePasswordRequest;
import za.ac.cput.prm_marketplace.dto.ForgotPasswordRequest;
import za.ac.cput.prm_marketplace.dto.LoginRequest;
import za.ac.cput.prm_marketplace.dto.RegisterRequest;
import za.ac.cput.prm_marketplace.dto.ResetPasswordRequest;
import za.ac.cput.prm_marketplace.dto.UserResponse;
import za.ac.cput.prm_marketplace.dto.VerifyCodeRequest;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IAuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IAuthService authService;

    @Autowired
    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    // OpenApiConfig applies bearerAuth globally, which would tell a generated client to send a
    // token to the endpoints that mint tokens. These must be declared public, matching the
    // permitAll list in SecurityConfig.
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }

    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @SecurityRequirements
    @PostMapping("/verify")
    public ResponseEntity<UserResponse> verify(@Valid @RequestBody VerifyCodeRequest request) {
        return ResponseEntity.ok(authService.verifyCode(request.email(), request.code()));
    }

    @SecurityRequirements
    @PostMapping("/resend-code")
    public ResponseEntity<Map<String, String>> resendCode(@RequestParam String email) {
        authService.resendCode(email);
        return ResponseEntity.ok(Map.of("message", "Verification code sent"));
    }

    @SecurityRequirements
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(Map.of("message", "If the account exists a reset link has been sent"));
    }

    @SecurityRequirements
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    /**
     * Changes the caller's own password.
     *
     * <p>The {@code email} parameter is gone. It used to name the account to modify, so any
     * authenticated caller could aim the request at another user's login, and the differing error
     * messages for an unknown address versus a wrong password turned the endpoint into an account
     * enumeration oracle. The account now comes from the token via {@link CurrentCaller}.
     *
     * <p>The two passwords arrive in a request body, not as request parameters. They were parameters
     * once, which put the new password in the request line and therefore in every access log, proxy
     * log and browser history entry on the path to this server. Neither field has any business in a
     * URL: the request line is for naming resources, not carrying secrets. Validation lives on the
     * DTO, so the minimum length rule is enforced here rather than only inside the service.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(Authentication authentication,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(CurrentCaller.id(authentication),
                request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }
}