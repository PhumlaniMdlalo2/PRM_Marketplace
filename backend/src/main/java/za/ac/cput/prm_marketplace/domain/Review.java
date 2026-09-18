package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private UUID reviewerId;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private String comment;

    protected Review() {

    }

    private Review(Builder builder) {
        this.id = builder.id;
        this.productId = builder.productId;
        this.reviewerId = builder.reviewerId;
        this.rating = builder.rating;
        this.comment = builder.comment;
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

    @Override
    public String toString() {
        return "Review{" +
                "id=" + id +
                ", productId=" + productId +
                ", reviewerId=" + reviewerId +
                ", rating=" + rating +
                ", comment='" + comment + '\'' +
                '}';
    }

    public static class Builder {

        private UUID id;
        private UUID productId;
        private UUID reviewerId;
        private int rating;
        private String comment;

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

        public Builder copy(Review review) {
            this.id = review.id;
            this.productId = review.productId;
            this.reviewerId = review.reviewerId;
            this.rating = review.rating;
            this.comment = review.comment;
            return this;
        }

        public Review build() {
            return new Review(this);
        }
    }
}