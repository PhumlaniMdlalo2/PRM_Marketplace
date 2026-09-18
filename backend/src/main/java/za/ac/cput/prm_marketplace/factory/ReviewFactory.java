package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.Review;

import java.util.UUID;

public class ReviewFactory {

    public static Review createReview(UUID productId, UUID reviewerId,
                                      int rating, String comment) {

        if (productId == null) {
            return null;
        }

        if (reviewerId == null) {
            return null;
        }

        if (rating < 1 || rating > 5) {
            return null;
        }

        if (comment == null || comment.isBlank()) {
            return null;
        }

        return new Review.Builder()
                .setProductId(productId)
                .setReviewerId(reviewerId)
                .setRating(rating)
                .setComment(comment)
                .build();
    }
}