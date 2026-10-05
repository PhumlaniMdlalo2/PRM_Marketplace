package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import za.ac.cput.prm_marketplace.domain.Review;
import za.ac.cput.prm_marketplace.repository.OrderItemRepository;
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

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private VendorRatingService vendorRatingService;

    private ReviewServiceImpl service;

    private UUID reviewerId;
    private UUID intruderId;
    private UUID productId;
    private UUID reviewId;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(reviewRepository, orderItemRepository, vendorRatingService);
        reviewerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        productId = UUID.randomUUID();
        reviewId = UUID.randomUUID();
    }

    @Test
    @DisplayName("create attributes the review to the caller, not the reviewerId in the body")
    void create_takesReviewerFromTheRequester() {
        givenBuyerHasPurchased();
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
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    @DisplayName("create refuses a review from somebody who never bought the product")
    void create_rejectsAUserWhoHasNotPurchased() {
        // The point of the purchase check: a rating means nothing from a non-buyer, and without this
        // any signed-up account could rate any listing.
        when(orderItemRepository.existsByProductIdAndBuyerId(productId, reviewerId)).thenReturn(false);

        assertThat(service.create(buildReview(5), reviewerId)).isNull();

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("create refuses a review naming no product")
    void create_rejectsAMissingProduct() {
        Review body = new Review.Builder()
                .setReviewerId(intruderId)
                .setRating(4)
                .setComment("Good value")
                .build();

        assertThat(service.create(body, reviewerId)).isNull();

        // Nothing is queried, so a null product cannot reach the new foreign key.
        verifyNoInteractions(orderItemRepository);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("create refuses a missing or oversized comment instead of hitting the column limit")
    void create_rejectsAnUnusableComment() {
        // reviews.comment is NOT NULL varchar(255). Passing these straight through produced a
        // constraint violation surfaced to the caller as a server error.
        when(orderItemRepository.existsByProductIdAndBuyerId(productId, reviewerId)).thenReturn(true);
        when(reviewRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        assertThat(service.create(buildReviewWithComment(4, null), reviewerId)).isNull();
        assertThat(service.create(buildReviewWithComment(4, "   "), reviewerId)).isNull();
        assertThat(service.create(buildReviewWithComment(4, "x".repeat(256)), reviewerId)).isNull();

        // Exactly 255 still fits the column, so only that one is saved.
        Review atTheLimit = service.create(buildReviewWithComment(4, "x".repeat(255)), reviewerId);
        assertThat(atTheLimit).isNotNull();
        assertThat(atTheLimit.getComment()).hasSize(255);

        verify(reviewRepository).save(any());
    }

    @Test
    @DisplayName("create reports a second review of the same product as null rather than a 500")
    void create_duplicateReviewIsRejected() {
        givenBuyerHasPurchased();
        // uk_review_product_reviewer is the reason this returns null instead of blowing up.
        when(reviewRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("uk_review_product_reviewer"));

        assertThat(service.create(buildReview(4), reviewerId)).isNull();
    }

    @Test
    @DisplayName("a filed review refreshes the seller's stored rating")
    void create_refreshesTheVendorRating() {
        givenBuyerHasPurchased();
        when(reviewRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        service.create(buildReview(4), reviewerId);

        // The listing page shows a cached average, so a review that does not update it leaves the
        // vendor showing no rating at all.
        verify(vendorRatingService).refreshForProduct(productId);
    }

    @Test
    @DisplayName("a rejected review does not touch the seller's rating")
    void create_rejectedReviewLeavesTheRatingAlone() {
        // A refused review changes nothing, so recalculating would be wasted work at best.
        assertThat(service.create(buildReview(5), reviewerId)).isNull();

        verifyNoInteractions(vendorRatingService);
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
    @DisplayName("update refuses to blank out the required comment")
    void update_rejectsAnUnusableComment() {
        Review body = new Review.Builder()
                .setId(reviewId)
                .setProductId(productId)
                .setReviewerId(reviewerId)
                .setRating(2)
                .setComment("   ")
                .build();

        assertThat(service.update(body, reviewerId)).isNull();

        // Rejected before the stored row is read, so nothing is written back.
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
    @DisplayName("editing a rating refreshes the seller's stored rating")
    void update_refreshesTheVendorRating() {
        Review stored = new Review.Builder()
                .setId(reviewId)
                .setProductId(productId)
                .setReviewerId(reviewerId)
                .setRating(3)
                .setComment("Average")
                .build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(stored));
        when(reviewRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        service.update(new Review.Builder()
                .setId(reviewId)
                .setProductId(productId)
                .setRating(1)
                .setComment("Changed my mind")
                .build(), reviewerId);

        verify(vendorRatingService).refreshForProduct(productId);
    }

    @Test
    @DisplayName("deleting a review refreshes the seller's stored rating")
    void delete_refreshesTheVendorRating() {
        // Owned by the caller: buildReview attributes to intruderId, which delete would refuse.
        Review stored = new Review.Builder()
                .setId(reviewId)
                .setProductId(productId)
                .setReviewerId(reviewerId)
                .setRating(4)
                .setComment("Good value")
                .build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(stored));

        assertThat(service.delete(reviewId, reviewerId)).isTrue();

        // Recalculated rather than adjusted, so removing the last review takes the average back to
        // null instead of leaving it at whatever that one review said.
        verify(vendorRatingService).refreshForProduct(productId);
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

    private Review buildReviewWithComment(int rating, String comment) {
        return new Review.Builder()
                .setProductId(productId)
                .setReviewerId(intruderId)
                .setRating(rating)
                .setComment(comment)
                .build();
    }

    /** The default happy path: the reviewer is a real buyer of this product. */
    private void givenBuyerHasPurchased() {
        when(orderItemRepository.existsByProductIdAndBuyerId(productId, reviewerId)).thenReturn(true);
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