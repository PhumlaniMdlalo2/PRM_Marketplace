package za.ac.cput.prm_marketplace.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_product_reviewer",
                columnNames = {"product_id", "reviewer_id"}))
public class Review {

    /**
     * Stays writable on purpose. The update endpoint takes the id in the body, so Jackson has to be
     * able to read it. The service looks the row up by that id and refuses it unless the caller is
     * the reviewer, so a forged id cannot reach another account's review.
     */
@Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "reviewer_id", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID reviewerId;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private String comment;

    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    protected Review() {

    }

    private Review(Builder builder) {
        this.id = builder.id;
        this.productId = builder.productId;
        this.reviewerId = builder.reviewerId;
        this.rating = builder.rating;
        this.comment = builder.comment;
        this.createdAt = builder.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Review{" +
                "id=" + id +
                ", productId=" + productId +
                ", reviewerId=" + reviewerId +
                ", rating=" + rating +
                ", comment='" + comment + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    public static class Builder {

        private UUID id;
        private UUID productId;
        private UUID reviewerId;
        private int rating;
private String comment;
        private LocalDateTime createdAt;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setProductId(UUID productId) {
            this.productId = productId;
            return this;
        }

        public Builder setReviewerId(UUID reviewerId) {
            this.reviewerId = reviewerId;
            return this;
        }

        public Builder setRating(int rating) {
            this.rating = rating;
            return this;
        }

        public Builder setComment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(Review review) {
            this.id = review.id;
            this.productId = review.productId;
            this.reviewerId = review.reviewerId;
            this.rating = review.rating;
            this.comment = review.comment;
            this.createdAt = review.createdAt;
            return this;
        }

        public Review build() {
            return new Review(this);
        }
    }
}
