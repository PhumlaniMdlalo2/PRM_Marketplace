package za.ac.cput.prm_marketplace.dto;

/**
 * What a caller gets back when it signs in, registers, or renews a session.
 *
 * <p>{@code refreshToken} is the long-lived credential the caller trades for a new {@code token}
 * once the short one lapses. It sits next to the access token because the two are issued together
 * and are useless apart: the access token expires within a day, and the refresh token is only good
 * for minting more of them.
 */
public record AuthResponse(
        String token,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
