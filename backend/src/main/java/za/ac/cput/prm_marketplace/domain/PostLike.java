package za.ac.cput.prm_marketplace.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import za.ac.cput.prm_marketplace.dto.AuthorSummary;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_likes", uniqueConstraints = @UniqueConstraint(name = "uk_post_like_user", columnNames = {"post_id", "user_id"}))
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BulletinPost post;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private User user;

    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    protected PostLike() {
    }

    private PostLike(Builder builder) {
        this.id = builder.id;
        this.post = builder.post;
        this.user = builder.user;
        this.createdAt = builder.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public BulletinPost getPost() {
        return post;
    }

    /** Serialised through {@link AuthorSummary}, for the same reason a post author is. */
    @Schema(implementation = AuthorSummary.class)

    @JsonSerialize(using = AuthorSummary.Serializer.class)
    public User getUser() {
        return user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostLike)) return false;
        PostLike postLike = (PostLike) o;
        return id != null && id.equals(postLike.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "PostLike{" +
                "id=" + id +
                ", post=" + (post == null ? null : post.getId()) +
                ", user=" + (user == null ? null : user.getId()) +
                ", createdAt=" + createdAt +
                '}';
    }

    public static class Builder {
        private UUID id;
        private BulletinPost post;
        private User user;
        private LocalDateTime createdAt;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setPost(BulletinPost post) {
            this.post = post;
            return this;
        }

        public Builder setUser(User user) {
            this.user = user;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(PostLike postLike) {
            this.id = postLike.id;
            this.post = postLike.post;
            this.user = postLike.user;
            this.createdAt = postLike.createdAt;
            return this;
        }

        public PostLike build() {
            return new PostLike(this);
        }
    }
}
