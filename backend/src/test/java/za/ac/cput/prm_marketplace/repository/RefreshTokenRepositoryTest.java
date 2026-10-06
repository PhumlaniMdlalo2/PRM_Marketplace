package za.ac.cput.prm_marketplace.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import za.ac.cput.prm_marketplace.domain.RefreshToken;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the refresh token lookup and the spend against a real database.
 *
 * <p>Both are security properties a mocked repository cannot say anything about. The mock can prove
 * that the service asked for the spend; only the database can prove that a second request presenting
 * the same token a moment later matches zero rows. That is the difference between a token that is
 * single-use and a token that is single-use unless two callers are quick about it. The lookup
 * hiding spent rows is the other half of it: a replayed token has to be indistinguishable from one
 * that never existed.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User jane;

    @BeforeEach
    void setUp() {
        jane = userRepository.save(buildUser("jane"));
    }

    @Test
    @DisplayName("a token is spent by exactly one exchange; the second finds nothing")
    void spendIfUnused_spendsOnceAndThenRefuses() {
        saveToken(jane, "live-token", LocalDateTime.now().plusHours(1), false);

        assertThat(refreshTokenRepository.spendIfUnused("live-token", LocalDateTime.now()))
                .as("the first exchange is the one that should get a session")
                .isEqualTo(1);
        assertThat(refreshTokenRepository.spendIfUnused("live-token", LocalDateTime.now()))
                .as("a replayed copy must not mint a second one")
                .isEqualTo(0);
        assertThat(refreshTokenRepository.findByTokenAndUsedFalse("live-token")).isEmpty();
    }

    @Test
    @DisplayName("a token whose clock has run out cannot be spent, even once")
    void spendIfUnused_leavesAnExpiredTokenUnspent() {
        saveToken(jane, "expired-token", LocalDateTime.now().minusMinutes(1), false);

        assertThat(refreshTokenRepository.spendIfUnused("expired-token", LocalDateTime.now()))
                .isEqualTo(0);
        // It stays readable, so the service is the one that answers with the same sign-in message
        // an unknown token gets — and no caller learns which of the two it presented.
        assertThat(refreshTokenRepository.findByTokenAndUsedFalse("expired-token")).isPresent();
    }

    @Test
    @DisplayName("a token that was never issued spends nothing")
    void spendIfUnused_isZeroForAnUnknownToken() {
        saveToken(jane, "live-token", LocalDateTime.now().plusHours(1), false);

        assertThat(refreshTokenRepository.spendIfUnused("made-up-token", LocalDateTime.now()))
                .isEqualTo(0);
        assertThat(refreshTokenRepository.findByTokenAndUsedFalse("live-token")).isPresent();
    }

    @Test
    @DisplayName("a spent token cannot be found again, so a replay looks like a stranger")
    void findByTokenAndUsedFalse_hidesASpentToken() {
        RefreshToken spent = saveToken(jane, "spent-token", LocalDateTime.now().plusHours(1), true);

        assertThat(refreshTokenRepository.findByTokenAndUsedFalse("spent-token"))
                .as("if this matched, the copied token would keep working")
                .isEmpty();
        assertThat(refreshTokenRepository.findById(spent.getId()))
                .as("the row stays, so the account's sessions can still be inspected and revoked")
                .isPresent();
    }

    @Test
    @DisplayName("changing a password drops that account's sessions and nobody else's")
    void deleteByUserId_removesOnlyThatAccountsTokens() {
        User john = userRepository.save(buildUser("john"));
        saveToken(jane, "janes-first", LocalDateTime.now().plusHours(1), false);
        saveToken(jane, "janes-second", LocalDateTime.now().plusDays(30), false);
        saveToken(john, "johns-token", LocalDateTime.now().plusDays(30), false);

        refreshTokenRepository.deleteByUserId(jane.getId());

        assertThat(refreshTokenRepository.findByTokenAndUsedFalse("janes-first")).isEmpty();
        assertThat(refreshTokenRepository.findByTokenAndUsedFalse("janes-second")).isEmpty();
        assertThat(refreshTokenRepository.findByTokenAndUsedFalse("johns-token")).isPresent();
    }

    private RefreshToken saveToken(User user, String value, LocalDateTime expiresAt, boolean used) {
        return refreshTokenRepository.save(new RefreshToken.Builder()
                .setUser(user)
                .setToken(value)
                .setExpiresAt(expiresAt)
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
