package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import za.ac.cput.prm_marketplace.domain.Review;
import za.ac.cput.prm_marketplace.repository.ReviewRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    private ReviewServiceImpl service;

    private UUID reviewerId;
    private UUID intruderId;
    private UUID productId;
    private UUID reviewId;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(reviewRepository);
        reviewerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        productId = UUID.randomUUID();
        reviewId = UUID.randomUUID();
    }

    @Test
    @DisplayName("create attributes the review to the caller, not the reviewerId in the body")
    void create_takesReviewerFromTheRequester() {
        when(reviewRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Review hostile = new Review.Builder()
                .setId(UUID.randomUUID())
                .setProductId(productId)
                .setReviewerId(intruderId)
                .setRating(5)
                .setComment("Excellent")
                .build();

        Review created = service.create(hostile, reviewerId);

        assertThat(created).isNotNull();
        assertThat(created.getReviewerId()).isEqualTo(reviewerId);
        assertThat(created.getProductId()).isEqualTo(productId);
        // A client-supplied id must not turn the insert into an overwrite.
        assertThat(created.getId()).isNull();
    }

    @Test
    @DisplayName("create rejects a rating outside one to five")
    void create_rejectsAnOutOfRangeRating() {
        assertThat(service.create(buildReview(0), reviewerId)).isNull();
        assertThat(service.create(buildReview(6), reviewerId)).isNull();
        assertThat(service.create(buildReview(-1), reviewerId)).isNull();

        verifyNoInteractions(reviewRepository);
    }

    @Test
    @DisplayName("create returns null for a null payload or reviewer")
    void create_rejectsNulls() {
        assertThat(service.create(null, reviewerId)).isNull();
        assertThat(service.create(buildReview(4), null)).isNull();

        verifyNoInteractions(reviewRepository);
    }

    @Test
    @DisplayName("create reports a second review of the same product as null rather than a 500")
    void create_duplicateReviewIsRejected() {
        // uk_review_product_reviewer is the reason this returns null instead of blowing up.
        when(reviewRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("uk_review_product_reviewer"));

        assertThat(service.create(buildReview(4), reviewerId)).isNull();
    }

    @Test
    @DisplayName("read returns the stored review or null when it is missing")
    void read_returnsStoredReview() {
        Review stored = buildReview(4, reviewId);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(stored));

        assertThat(service.read(reviewId)).isSameAs(stored);
        assertThat(service.read(null)).isNull();
    }

    @Test
    @DisplayName("update saves the caller's review but keeps the stored product and reviewer")
    void update_preservesStoredAttribution() {
        Review stored = new Review.Builder()
                .setId(reviewId)
                .setProductId(productId)
                .setReviewerId(reviewerId)
                .setRating(3)
                .setComment("Average")
                .build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(stored));
        when(reviewRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Review body = new Review.Builder()
                .setId(reviewId)
                .setProductId(UUID.randomUUID())
                .setReviewerId(intruderId)
                .setRating(5)
                .setComment("Actually excellent")
                .build();

        Review updated = service.update(body, reviewerId);

        assertThat(updated).isNotNull();
        assertThat(updated.getRating()).isEqualTo(5);
        assertThat(updated.getComment()).isEqualTo("Actually excellent");
        assertThat(updated.getReviewerId()).isEqualTo(reviewerId);
        assertThat(updated.getProductId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("update refuses to touch another account's review")
    void update_foreignReviewIsRefused() {
        Review stored = new Review.Builder()
                .setId(reviewId)
                .setProductId(productId)
                .setReviewerId(intruderId)
                .setRating(3)
                .setComment("Average")
                .build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(stored));

        assertThat(service.update(stored, reviewerId)).isNull();
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("update rejects an out-of-range rating without saving")
    void update_rejectsAnOutOfRangeRating() {
        Review body = new Review.Builder()
                .setId(reviewId)
                .setProductId(productId)
                .setReviewerId(reviewerId)
                .setRating(99)
                .setComment("Perfect")
                .build();

        // The rating is validated before the stored row is even loaded.
        assertThat(service.update(body, reviewerId)).isNull();
        verifyNoInteractions(reviewRepository);
    }

    @Test
    @DisplayName("update returns null for a missing review or a body without an id")
    void update_invalidInputReturnsNull() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThat(service.update(buildReview(4, reviewId), reviewerId)).isNull();
        assertThat(service.update(buildReview(4), reviewerId)).isNull();
        assertThat(service.update(null, reviewerId)).isNull();

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete removes the caller's own review")
    void delete_ownedReviewIsRemoved() {
        Review stored = new Review.Builder()
                .setId(reviewId)
                .setProductId(productId)
                .setReviewerId(reviewerId)
                .setRating(3)
                .setComment("Average")
                .build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(stored));

        assertThat(service.delete(reviewId, reviewerId)).isTrue();
        verify(reviewRepository).deleteById(reviewId);
    }

    @Test
    @DisplayName("delete refuses to remove another account's review")
    void delete_foreignReviewIsRefused() {
        Review stored = new Review.Builder()
                .setId(reviewId)
                .setProductId(productId)
                .setReviewerId(intruderId)
                .setRating(3)
                .setComment("Average")
                .build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(stored));

        assertThat(service.delete(reviewId, reviewerId)).isFalse();
        verify(reviewRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete returns false for a missing review")
    void delete_missingReturnsFalse() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThat(service.delete(reviewId, reviewerId)).isFalse();
        verify(reviewRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("getAll returns every review")
    void getAll_returnsEveryReview() {
        Review stored = buildReview(4, reviewId);
        when(reviewRepository.findAll()).thenReturn(List.of(stored));

        assertThat(service.getAll()).containsExactly(stored);
    }

    @Test
    @DisplayName("product and author listings scope to their argument and reject null")
    void listings_areScoped() {
        Review stored = buildReview(4, reviewId);
        when(reviewRepository.findByProductId(productId)).thenReturn(List.of(stored));
        when(reviewRepository.findByReviewerId(reviewerId)).thenReturn(List.of(stored));

        assertThat(service.getByProduct(productId)).containsExactly(stored);
        assertThat(service.getByReviewer(reviewerId)).containsExactly(stored);
        assertThat(service.getByProduct(null)).isEmpty();
        assertThat(service.getByReviewer(null)).isEmpty();
    }

    private Review buildReview(int rating) {
        return new Review.Builder()
                .setProductId(productId)
                .setReviewerId(intruderId)
                .setRating(rating)
                .setComment("Good value")
                .build();
    }

    /** The entity deliberately exposes no id setter, so the id is seeded via the builder. */
    private Review buildReview(int rating, UUID id) {
        return new Review.Builder()
                .setId(id)
                .setProductId(productId)
                .setReviewerId(intruderId)
                .setRating(rating)
                .setComment("Good value")
                .build();
    }
}