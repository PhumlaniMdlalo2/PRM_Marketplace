package za.ac.cput.prm_marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/auth/change-password}.
 *
 * <p>This is a request body rather than two request parameters because the new password is a
 * credential: parameters travel in the request line, which means it lands in every access log,
 * proxy log and browser history entry between the client and the server. The account itself is still
 * not in here - it comes from the token.
 *
 * <p>The length rule matches {@link ResetPasswordRequest}, so a password that could not have been set
 * at registration cannot be set by this route either.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {
}