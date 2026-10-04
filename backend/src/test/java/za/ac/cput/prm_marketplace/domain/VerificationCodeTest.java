package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationCodeTest {

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .build();
    }

    private VerificationCode buildCode(String code, LocalDateTime expiresAt) {
        return new VerificationCode.Builder()
                .setId(UUID.randomUUID())
                .setUser(buildUser())
                .setCode(code)
                .setExpiresAt(expiresAt)
                .build();
    }

    @Test
    void builderSetsAllFields() {
        UUID id = UUID.randomUUID();
        User user = buildUser();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        VerificationCode code = new VerificationCode.Builder()
                .setId(id)
                .setUser(user)
                .setCode("123456")
                .setExpiresAt(expiresAt)
                .setUsed(true)
                .build();

        assertThat(code.getId()).isEqualTo(id);
        assertThat(code.getUser()).isEqualTo(user);
        assertThat(code.getCode()).isEqualTo("123456");
        assertThat(code.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(code.isUsed()).isTrue();
    }

    @Test
    void copyPreservesEveryField() {
        VerificationCode original = buildCode("654321", LocalDateTime.now().plusMinutes(15));

        VerificationCode copy = new VerificationCode.Builder().copy(original).build();

        assertThat(copy.getId()).isEqualTo(original.getId());
        assertThat(copy.getUser()).isEqualTo(original.getUser());
        assertThat(copy.getCode()).isEqualTo(original.getCode());
        assertThat(copy.getExpiresAt()).isEqualTo(original.getExpiresAt());
        assertThat(copy.isUsed()).isEqualTo(original.isUsed());
    }

    @Test
    void unusedCodeInTheFutureIsValid() {
        VerificationCode code = buildCode("123456", LocalDateTime.now().plusMinutes(15));

        assertThat(code.isExpired()).isFalse();
        assertThat(code.isValid()).isTrue();
    }

    @Test
    void codePastItsExpiryIsExpiredAndInvalid() {
        VerificationCode code = buildCode("123456", LocalDateTime.now().minusMinutes(1));

        assertThat(code.isExpired()).isTrue();
        assertThat(code.isValid()).isFalse();
    }

    @Test
    void markUsedMakesTheCodeInvalidWithoutTouchingTheExpiry() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        VerificationCode code = buildCode("123456", expiresAt);

        code.markUsed();

        assertThat(code.isUsed()).isTrue();
        assertThat(code.isValid()).isFalse();
        assertThat(code.isExpired()).isFalse();
        assertThat(code.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void aCodeWithNoExpiryIsNotTreatedAsExpired() {
        VerificationCode code = buildCode("123456", null);

        assertThat(code.isExpired()).isFalse();
        assertThat(code.isValid()).isTrue();
    }

    @Test
    void equalityIsByIdAndDoesNotApplyBeforeOneExists() {
        UUID id = UUID.randomUUID();
        VerificationCode first = new VerificationCode.Builder().setId(id).setCode("111111").build();
        VerificationCode second = new VerificationCode.Builder().setId(id).setCode("222222").build();

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);

        VerificationCode transientOne = new VerificationCode.Builder().setCode("111111").build();
        VerificationCode otherTransient = new VerificationCode.Builder().setCode("999999").build();

        assertThat(transientOne).isNotEqualTo(otherTransient);
        assertThat(transientOne).isEqualTo(transientOne);
    }
}