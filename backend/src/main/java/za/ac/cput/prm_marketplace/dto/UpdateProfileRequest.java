package za.ac.cput.prm_marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The only fields a user may change about their own account.
 *
 * <p>This record exists so that a profile edit is described by its allowlist rather than by what
 * happens to deserialise. The previous {@code PUT /users} accepted the {@code User} entity itself
 * and took the target account from {@code user.getId()} inside the body, so any authenticated
 * caller could rewrite any account — including its {@code role}, which is how the registration
 * allowlist added in the last change was bypassed, and its {@code password}, which was written
 * straight to the column without hashing. Naming the permitted fields means there is no
 * {@code role}, no {@code password} and no {@code id} for a caller to set, so none of those can be
 * escalated through this endpoint.
 *
 * <p>Email is deliberately absent. Changing it invalidates the verified flag that the emailed code
 * establishes, so it needs its own confirm-the-new-address flow rather than being folded in here.
 * Passwords have their own endpoint that re-hashes and revokes tokens.
 *
 * <p>P semantics: {@code name} is required and {@code phone} / {@code avatarUrl} are replaced
 * outright, so sending a blank value clears the field. Send the whole form.
 */
public record UpdateProfileRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        @Size(max = 32, message = "Phone number must be at most 32 characters")
        String phone,

        @Size(max = 2048, message = "Avatar URL must be at most 2048 characters")
        @Pattern(
                regexp = "^$|https?://.+",
                message = "Avatar URL must start with http:// or https://")
        String avatarUrl,

        @Size(max = 160, message = "Campus must be at most 160 characters")
        String campus
) {
}