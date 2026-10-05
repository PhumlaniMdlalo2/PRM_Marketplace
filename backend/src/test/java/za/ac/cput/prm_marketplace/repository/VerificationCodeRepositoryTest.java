package za.ac.cput.prm_marketplace.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VerificationCode;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the verification code lookups against a real database.
 *
 * <p>The same {@code used = false} predicate as the password reset token, with the same reason for
 * proving it here: a six digit code that stays valid after it has been spent is an account takeover
 * waiting to happen, and a mocked repository cannot show whether the filter is applied to the column.
 *
 * <p>{@code deleteByUserIdAndUsedFalse} is the one that matters most. Clearing only the unused codes
 * on resend is what stops the delete from erasing the record that a code was already spent, while
 * still making sure an old code cannot be tried alongside a new one.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VerificationCodeRepositoryTest {

    @Autowired
    private VerificationCodeRepository codeRepository;

    @Autowired
    private UserRepository userRepository;

    private User jane;
    private User john;

    @BeforeEach
    void setUp() {
        jane = userRepository.save(buildUser("jane"));
        john = userRepository.save(buildUser("john"));
    }

    @Test
    @DisplayName("an unused code verifies the account it was issued for")
    void findByUserIdAndCodeAndUsedFalse_returnsTheLiveCode() {
        VerificationCode code = saveCode(jane, "123456", false);

        assertThat(codeRepository.findByUserIdAndCodeAndUsedFalse(jane.getId(), "123456")).contains(code);
    }

    @Test
    @DisplayName("a spent code no longer verifies, even for the right account")
    void findByUserIdAndCodeAndUsedFalse_hidesASpentCode() {
        saveCode(jane, "123456", true);

        assertThat(codeRepository.findByUserIdAndCodeAndUsedFalse(jane.getId(), "123456"))
                .as("replaying a spent code would verify the account a second time")
                .isEmpty();
    }

    @Test
    @DisplayName("one account's code does not verify another account")
    void findByUserIdAndCodeAndUsedFalse_doesNotCrossAccounts() {
        saveCode(jane, "123456", false);

        assertThat(codeRepository.findByUserIdAndCodeAndUsedFalse(john.getId(), "123456"))
                .as("the code is only meaningful together with the account it was sent to")
                .isEmpty();
    }

    @Test
    @DisplayName("a code issued for another purpose does not match a different one")
    void findByUserIdAndCodeAndUsedFalse_doesNotMatchADifferentCode() {
        saveCode(jane, "123456", false);

        assertThat(codeRepository.findByUserIdAndCodeAndUsedFalse(jane.getId(), "654321")).isEmpty();
    }

    @Test
    @DisplayName("the outstanding codes are the ones a user could still type")
    void findByUserIdAndUsedFalse_listsOnlyWhatIsStillUsable() {
        saveCode(jane, "111111", true);
        VerificationCode current = saveCode(jane, "222222", false);
        saveCode(john, "333333", false);

        assertThat(codeRepository.findByUserIdAndUsedFalse(jane.getId())).containsExactly(current);
    }

    @Test
    @DisplayName("resending clears the codes still waiting to be typed")
    void deleteByUserIdAndUsedFalse_removesTheOutstandingCodes() {
        saveCode(jane, "111111", false);
        VerificationCode current = saveCode(jane, "222222", false);

        codeRepository.deleteByUserIdAndUsedFalse(jane.getId());

        assertThat(codeRepository.findByUserIdAndUsedFalse(jane.getId()))
                .as("an old code must stop working the moment a new one is sent")
                .isEmpty();
        assertThat(codeRepository.findByUserIdAndCodeAndUsedFalse(jane.getId(), "222222"))
                .as("and the newly issued code goes with it, because it was pending too")
                .isNotPresent();
        assertThat(codeRepository.findById(current.getId()))
                .as("the row is deleted, not merely flagged")
                .isEmpty();
    }

    @Test
    @DisplayName("clearing one account's codes leaves the record of codes already spent")
    void deleteByUserIdAndUsedFalse_keepsSpentCodesAndOtherAccounts() {
        VerificationCode spent = saveCode(jane, "111111", true);
        saveCode(jane, "222222", false);
        VerificationCode joHNsPending = saveCode(john, "333333", false);

        codeRepository.deleteByUserIdAndUsedFalse(jane.getId());

        assertThat(codeRepository.findById(spent.getId()))
                .as("this is the audit trail: the code was issued and it was spent")
                .isPresent();
        assertThat(codeRepository.findByUserIdAndUsedFalse(john.getId()))
                .containsExactly(joHNsPending);
    }

    private VerificationCode saveCode(User user, String code, boolean used) {
        return codeRepository.save(new VerificationCode.Builder()
                .setUser(user)
                .setCode(code)
                .setExpiresAt(LocalDateTime.now().plusMinutes(15))
                .setUsed(used)
                .build());
    }

    private User buildUser(String name) {
        return new User.Builder()
                .setName(name)
                .setEmail(name + "-" + UUID.randomUUID() + "@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .build();
    }
}