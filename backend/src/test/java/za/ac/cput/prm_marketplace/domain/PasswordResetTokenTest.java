package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokenTest {

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .build();
    }

    private PasswordResetToken buildToken(String token, LocalDateTime expiresAt) {
        return new PasswordResetToken.Builder()
                .setId(UUID.randomUUID())
                .setUser(buildUser())
                .setToken(token)
                .setExpiresAt(expiresAt)
                .build();
    }

    @Test
    void builderSetsAllFields() {
        UUID id = UUID.randomUUID();
        User user = buildUser();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        PasswordResetToken token = new PasswordResetToken.Builder()
                .setId(id)
                .setUser(user)
                .setToken("a1b2c3")
                .setExpiresAt(expiresAt)
                .setUsed(true)
                .build();

        assertThat(token.getId()).isEqualTo(id);
        assertThat(token.getUser()).isEqualTo(user);
        assertThat(token.getToken()).isEqualTo("a1b2c3");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void copyPreservesEveryField() {
        PasswordResetToken original = buildToken("a1b2c3", LocalDateTime.now().plusHours(1));

        PasswordResetToken copy = new PasswordResetToken.Builder().copy(original).build();

        assertThat(copy.getId()).isEqualTo(original.getId());
        assertThat(copy.getUser()).isEqualTo(original.getUser());
        assertThat(copy.getToken()).isEqualTo(original.getToken());
        assertThat(copy.getExpiresAt()).isEqualTo(original.getExpiresAt());
        assertThat(copy.isUsed()).isEqualTo(original.isUsed());
    }

    @Test
    void unusedTokenInTheFutureIsValid() {
        PasswordResetToken token = buildToken("a1b2c3", LocalDateTime.now().plusHours(1));

        assertThat(token.isExpired()).isFalse();
        assertThat(token.isValid()).isTrue();
    }

    @Test
    void tokenPastItsExpiryIsExpiredAndInvalid() {
        PasswordResetToken token = buildToken("a1b2c3", LocalDateTime.now().minusSeconds(1));

        assertThat(token.isExpired()).isTrue();
        assertThat(token.isValid()).isFalse();
    }

    @Test
    void markUsedMakesTheTokenInvalidWithoutTouchingTheExpiry() {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        PasswordResetToken token = buildToken("a1b2c3", expiresAt);

        token.markUsed();

        assertThat(token.isUsed()).isTrue();
        assertThat(token.isValid()).isFalse();
        assertThat(token.isExpired()).isFalse();
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void toStringDoesNotLeakTheTokenItself() {
        PasswordResetToken token = buildToken("super-secret-token", LocalDateTime.now().plusHours(1));

        // Password hashes and reset tokens are credentials; a stray log of the entity would leak them.
        assertThat(token.toString()).doesNotContain("super-secret-token");
    }

    @Test
    void equalityIsByIdAndDoesNotApplyBeforeOneExists() {
        UUID id = UUID.randomUUID();
        PasswordResetToken first = new PasswordResetToken.Builder().setId(id).setToken("aaa").build();
        PasswordResetToken second = new PasswordResetToken.Builder().setId(id).setToken("bbb").build();

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);

        PasswordResetToken transientOne = new PasswordResetToken.Builder().setToken("aaa").build();
        PasswordResetToken otherTransient = new PasswordResetToken.Builder().setToken("zzz").build();

        assertThat(transientOne).isNotEqualTo(otherTransient);
        assertThat(transientOne).isEqualTo(transientOne);
    }
}