package za.ac.cput.prm_marketplace.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import za.ac.cput.prm_marketplace.domain.Review;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewFactoryTest {

    @Test
    @DisplayName("builds a review with the supplied rating and comment")
    void createReview_builds() {
        UUID productId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();

        Review review = ReviewFactory.createReview(productId, reviewerId, 4, "Book was in great shape");

        assertThat(review).isNotNull();
        assertThat(review.getProductId()).isEqualTo(productId);
        assertThat(review.getReviewerId()).isEqualTo(reviewerId);
        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getComment()).isEqualTo("Book was in great shape");
    }

    @Test
    @DisplayName("requires a product and a reviewer")
    void createReview_requiresBothIds() {
        assertThat(ReviewFactory.createReview(null, UUID.randomUUID(), 4, "Nice")).isNull();
        assertThat(ReviewFactory.createReview(UUID.randomUUID(), null, 4, "Nice")).isNull();
    }

    @ParameterizedTest(name = "rating {0} is accepted")
    @ValueSource(ints = {1, 2, 3, 4, 5})
    @DisplayName("accepts every rating on the 1-5 scale")
    void createReview_acceptsValidRange(int rating) {
        assertThat(ReviewFactory.createReview(UUID.randomUUID(), UUID.randomUUID(), rating, "Fine"))
                .isNotNull();
    }

    @ParameterizedTest(name = "rating {0} is rejected")
    @ValueSource(ints = {0, -1, 6, 100})
    @DisplayName("rejects ratings outside the 1-5 scale")
    void createReview_rejectsOutOfRange(int rating) {
        assertThat(ReviewFactory.createReview(UUID.randomUUID(), UUID.randomUUID(), rating, "Fine"))
                .isNull();
    }

    @Test
    @DisplayName("requires a non-blank comment")
    void createReview_requiresComment() {
        UUID productId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();

        assertThat(ReviewFactory.createReview(productId, reviewerId, 4, null)).isNull();
        assertThat(ReviewFactory.createReview(productId, reviewerId, 4, "")).isNull();
        assertThat(ReviewFactory.createReview(productId, reviewerId, 4, "    ")).isNull();
    }
}
