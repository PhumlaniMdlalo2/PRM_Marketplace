package za.ac.cput.prm_marketplace.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import za.ac.cput.prm_marketplace.domain.PasswordResetToken;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the password reset token lookups against a real database.
 *
 * <p>Both queries filter on {@code used = false}, and that predicate is the entire security property
 * here: it is what stops a reset link from working twice. A mocked repository test asserts that the
 * service asked for an unused token, which says nothing about whether the column is actually
 * filtered — and a derived {@code UsedFalse} that quietly stopped matching would let a spent link
 * reset a password a second time.
 *
 * <p>Expiry is not applied here: {@link za.ac.cput.prm_marketplace.service.AuthServiceImpl} compares
 * {@code expiresAt} itself, so the database returns spent-but-unexpired rows and the service rejects
 * them. That is why the used flag is the part worth proving here.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

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
    @DisplayName("an unused token is found by the value in the emailed link")
    void findByTokenAndUsedFalse_returnsTheLiveToken() {
        PasswordResetToken token = saveToken(jane, "live-token", false);

        assertThat(tokenRepository.findByTokenAndUsedFalse("live-token")).contains(token);
    }

    @Test
    @DisplayName("a spent token cannot be found again, which is what makes the link single use")
    void findByTokenAndUsedFalse_hidesASpentToken() {
        PasswordResetToken token = saveToken(jane, "spent-token", true);

        assertThat(tokenRepository.findByTokenAndUsedFalse("spent-token"))
                .as("if this matched, the link would keep working after the password changed")
                .isEmpty();
        assertThat(tokenRepository.findById(token.getId()))
                .as("the row stays, so a second request can see the token was already spent")
                .isPresent();
    }

    @Test
    @DisplayName("a token that was never issued is not found")
    void findByTokenAndUsedFalse_isEmptyForAnUnknownToken() {
        saveToken(jane, "live-token", false);

        assertThat(tokenRepository.findByTokenAndUsedFalse("made-up-token")).isEmpty();
    }

    @Test
    @DisplayName("only the outstanding token is offered for a user, not their spent ones")
    void findByUserIdAndUsedFalse_returnsTheOutstandingTokenOnly() {
        saveToken(jane, "first-token", true);
        PasswordResetToken current = saveToken(jane, "second-token", false);
        saveToken(john, "johns-token", false);

        assertThat(tokenRepository.findByUserIdAndUsedFalse(jane.getId())).contains(current);
    }

    @Test
    @DisplayName("a user who has spent every token has nothing outstanding")
    void findByUserIdAndUsedFalse_isEmptyOnceEveryTokenIsSpent() {
        saveToken(jane, "first-token", true);

        assertThat(tokenRepository.findByUserIdAndUsedFalse(jane.getId())).isEmpty();
    }

    @Test
    @DisplayName("one user's spent token is not found under another user's account")
    void findByTokenAndUsedFalse_doesNotCrossAccounts() {
        saveToken(jane, "janes-token", true);

        assertThat(tokenRepository.findByUserIdAndUsedFalse(john.getId()))
                .as("a spent link must not report a different account as having a live token")
                .isEmpty();
    }

    private PasswordResetToken saveToken(User user, String value, boolean used) {
        return tokenRepository.save(new PasswordResetToken.Builder()
                .setUser(user)
                .setToken(value)
                .setExpiresAt(LocalDateTime.now().plusHours(1))
                .setUsed(used)
                .build());
    }

    private User buildUser(String name) {
        return new User.Builder()
                .setName(name)
                .setEmail(name + "-" + UUID.randomUUID() + "@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .setVerified(true)
                .build();
    }
}