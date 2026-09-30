package za.ac.cput.prm_marketplace.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "a-test-signing-secret-that-is-long-enough-for-hs256!!";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3_600_000L);
    }

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setRole(Role.VENDOR)
                .build();
    }

    @Test
    @DisplayName("generateToken: round-trips the subject and user id claims")
    void generateToken_roundTripsClaims() {
        User user = buildUser();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("jane@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("generateToken: puts the role and display name in the payload")
    void generateToken_includesRoleAndNameInPayload() {
        User user = buildUser();

        String token = jwtService.generateToken(user);

        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                java.nio.charset.StandardCharsets.UTF_8);
        assertThat(payload).contains("VENDOR");
        assertThat(payload).contains("Jane Doe");
    }

    @Test
    @DisplayName("generateToken: tolerates a user without an id")
    void generateToken_userWithoutId_doesNotThrow() {
        User user = new User.Builder()
                .setName("No Id")
                .setEmail("noid@example.com")
                .setRole(Role.STUDENT)
                .build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUserId(token)).isNull();
        assertThat(jwtService.extractUsername(token)).isEqualTo("noid@example.com");
    }

    @Test
    @DisplayName("isValid: accepts a freshly issued token for the matching user")
    void isValid_freshToken_returnsTrue() {
        String token = jwtService.generateToken(buildUser());

        assertThat(jwtService.isValid(token, "jane@example.com")).isTrue();
    }

    @Test
    @DisplayName("isValid: rejects a token presented for a different user")
    void isValid_wrongUsername_returnsFalse() {
        String token = jwtService.generateToken(buildUser());

        assertThat(jwtService.isValid(token, "someone-else@example.com")).isFalse();
    }

    @Test
    @DisplayName("isValid: rejects a token signed with a different secret")
    void isValid_foreignSignature_returnsFalse() {
        String foreignToken = new JwtService("another-secret-that-is-also-long-enough-for-hs256", 3_600_000L)
                .generateToken(buildUser());

        assertThat(jwtService.isValid(foreignToken, "jane@example.com")).isFalse();
    }

    @Test
    @DisplayName("isValid: rejects a structurally invalid token without throwing")
    void isValid_garbageToken_returnsFalse() {
        assertThat(jwtService.isValid("not-a-jwt", "jane@example.com")).isFalse();
    }

    @Test
    @DisplayName("isValid: rejects an expired token")
    void isValid_expiredToken_returnsFalse() throws InterruptedException {
        JwtService shortLived = new JwtService(SECRET, 1L);
        String token = shortLived.generateToken(buildUser());
        Thread.sleep(20);

        assertThat(shortLived.isValid(token, "jane@example.com")).isFalse();
    }

    @Test
    @DisplayName("getExpirationMillis: returns the configured lifetime")
    void getExpirationMillis_returnsConfiguredValue() {
        assertThat(jwtService.getExpirationMillis()).isEqualTo(3_600_000L);
    }
}