package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.RefreshToken;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Looks a presented token up among the ones that have not been spent.
     *
     * <p>Spent rows are deliberately not returned: a token that was already exchanged and a token
     * that never existed must both come back empty so the two cannot be told apart by whoever is
     * holding the copy that lost the race.
     */
    Optional<RefreshToken> findByTokenAndUsedFalse(String token);

    /**
     * Spends a refresh token in a single statement.
     *
     * <p>The {@code used = false} predicate is the whole point, and it is the same argument as
     * {@link ProductRepository#decrementStock}: reading the row first and writing it back afterwards
     * lets two requests carrying the same token both observe it unused and both exchange it, which
     * is what a stolen copy replayed alongside the original would be doing. Letting the database
     * evaluate the condition and flip the flag under one statement makes the second caller match
     * zero rows instead. The expiry is in the same predicate for the same reason — a token whose
     * clock ran out a moment ago must not be spendable by anybody, not even once.
     *
     * @return 1 when the token was spent by this call, 0 when it was already spent, expired, or
     *         never existed
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken t
               set t.used = true
             where t.token = :token
               and t.used = false
               and t.expiresAt > :now
            """)
    int spendIfUnused(@Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * Drops every live session for an account. Called when the password changes, because the point
     * of changing a password is usually that it should stop working where it was working.
     */
    void deleteByUserId(UUID userId);
}
