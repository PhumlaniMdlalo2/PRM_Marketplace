package za.ac.cput.prm_marketplace.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.ac.cput.prm_marketplace.domain.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_NAME = "name";

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms:86400000}") long expirationMillis) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret is not set. Provide a secret of at least "
                    + MIN_SECRET_BYTES + " bytes via JWT_SECRET.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret must be at least " + MIN_SECRET_BYTES
                    + " bytes so it is valid for HS256.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getId() == null ? null : user.getId().toString())
                .claim(CLAIM_ROLE, user.getRole() == null ? null : user.getRole().name())
                .claim(CLAIM_NAME, user.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMillis, ChronoUnit.MILLIS)))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        String raw = claims(token).get(CLAIM_USER_ID, String.class);
        return raw == null ? null : UUID.fromString(raw);
    }

    public boolean isValid(String token, String username) {
        try {
            Claims claims = claims(token);
            return username.equals(claims.getSubject()) && claims.getExpiration().after(new Date());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}