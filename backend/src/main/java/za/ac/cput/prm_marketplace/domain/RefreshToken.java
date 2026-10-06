package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A long-lived credential that can be traded for a new access token.
 *
 * <p>The token is an opaque random string rather than a second JWT. A refresh token has to be
 * readable at a glance in the database when something goes wrong — which sessions exist, which have
 * been spent — and it has to be spendable exactly once. Both are a property of this row: the value
 * lives here, and
 * {@link za.ac.cput.prm_marketplace.repository.RefreshTokenRepository#spendIfUnused} is what stops a
 * copy of a stolen token from minting access tokens forever. A second signed JWT would be neither
 * inspectable nor single-use without a store behind it anyway, which is the store this is.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    protected RefreshToken() {
    }

    private RefreshToken(Builder builder) {
        this.id = builder.id;
        this.user = builder.user;
        this.token = builder.token;
        this.expiresAt = builder.expiresAt;
        this.used = builder.used;
        this.createdAt = builder.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Whether this token may still be exchanged for a new session.
     *
     * <p>Spent and expired are the two reasons a refresh fails, and neither is worth telling apart
     * to the caller: both mean the same thing — sign in again. Spent rows are checked again by the
     * database at exchange time, which is the check that actually holds under concurrency.
     */
    public boolean isValid() {
        return !used && !isExpired();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshToken that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id +
                ", userId=" + (user == null ? null : user.getId()) +
                ", expiresAt=" + expiresAt +
                ", used=" + used +
                ", createdAt=" + createdAt +
                '}';
    }

    public static class Builder {
        private UUID id;
        private User user;
        private String token;
        private LocalDateTime expiresAt;
        private boolean used;
        private LocalDateTime createdAt;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setUser(User user) {
            this.user = user;
            return this;
        }

        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        public Builder setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder setUsed(boolean used) {
            this.used = used;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RefreshToken build() {
            return new RefreshToken(this);
        }
    }
}
