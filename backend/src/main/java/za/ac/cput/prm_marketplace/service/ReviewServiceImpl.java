package za.ac.cput.prm_marketplace.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Review;
import za.ac.cput.prm_marketplace.repository.OrderItemRepository;
import za.ac.cput.prm_marketplace.repository.ReviewRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewServiceImpl implements IReviewService {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    /**
     * Matches {@code reviews.comment varchar(255) not null}.
     *
     * <p>The column is NOT NULL and length-limited, and the comment used to be passed straight
     * through. A review with no comment, or one longer than the column, therefore reached the
     * database and came back as a constraint violation surfaced as a 500 — the caller sent bad
     * input and got told the server was broken. Validating here turns both into the same null this
     * service already returns for an unusable rating.
     */
    private static final int MAX_COMMENT_LENGTH = 255;

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final VendorRatingService vendorRatingService;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             OrderItemRepository orderItemRepository,
                             VendorRatingService vendorRatingService) {
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.vendorRatingService = vendorRatingService;
    }

    @Override
    @Transactional
    public Review create(Review review, UUID reviewerId) {
        if (review == null || reviewerId == null) {
            return null;
        }
        if (review.getProductId() == null) {
            return null;
        }
        if (!isValidRating(review.getRating())) {
            return null;
        }
        if (!isCommentAcceptable(review.getComment())) {
            return null;
        }

        // A review only means something if the reviewer bought the thing. Without this check any
        // signed-up account can rate any listing, which makes the average worthless as a trust
        // signal and leaves the vendor's rating column aggregating noise.
        if (!orderItemRepository.existsByProductIdAndBuyerId(review.getProductId(), reviewerId)) {
            return null;
        }

        // The reviewer comes from the token, so a review cannot be attributed to another account.
        Review created = new Review.Builder()
                .setProductId(review.getProductId())
                .setReviewerId(reviewerId)
                .setRating(review.getRating())
                .setComment(review.getComment())
                .build();
        try {
            Review saved = reviewRepository.save(created);
            // The vendor's displayed rating is a cache of this table, so it has to be brought back in
            // step the moment a review lands. Without this the average stayed null forever.
            vendorRatingService.refreshForProduct(review.getProductId());
            return saved;
        } catch (DataIntegrityViolationException alreadyReviewed) {
            // The unique index on (product_id, reviewer_id) rejects a second review of the same
            // product by the same person. Report that rather than letting a constraint error
            // surface as a 500.
            return null;
        }
    }

    @Override
    public Review read(UUID id) {
        if (id == null) {
            return null;
        }
        return reviewRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Review update(Review review, UUID requesterId) {
        if (review == null || review.getId() == null || !isValidRating(review.getRating())) {
            return null;
        }
        if (!isCommentAcceptable(review.getComment())) {
            return null;
        }
        Review existing = reviewRepository.findById(review.getId()).orElse(null);
        if (existing == null || !isReviewer(existing, requesterId)) {
            return null;
        }
        // Only the rating and comment may change. The product and reviewer are fixed at creation.
        Review updated = new Review.Builder()
                .copy(existing)
                .setRating(review.getRating())
                .setComment(review.getComment())
                .build();
        Review saved = reviewRepository.save(updated);
        // Changing a rating changes the average it feeds.
        vendorRatingService.refreshForProduct(saved.getProductId());
        return saved;
    }

    @Override
    @Transactional
    public boolean delete(UUID id, UUID requesterId) {
        Review existing = reviewRepository.findById(id).orElse(null);
        if (existing == null || !isReviewer(existing, requesterId)) {
            return false;
        }
        reviewRepository.deleteById(id);
        // Recalculated rather than adjusted: with the last review gone the average has to go back to
        // null instead of staying at whatever the one review said.
        vendorRatingService.refreshForProduct(existing.getProductId());
        return true;
    }

    @Override
    public List<Review> getAll() {
        return reviewRepository.findAll();
    }

    @Override
    public List<Review> getByProduct(UUID productId) {
        if (productId == null) {
            return List.of();
        }
        return reviewRepository.findByProductId(productId);
    }

    @Override
    public List<Review> getByReviewer(UUID reviewerId) {
        if (reviewerId == null) {
            return List.of();
        }
        return reviewRepository.findByReviewerId(reviewerId);
    }

    private boolean isValidRating(int rating) {
        return rating >= MIN_RATING && rating <= MAX_RATING;
    }

    /** The comment is required by the schema, and must fit the column. */
    private boolean isCommentAcceptable(String comment) {
        return comment != null && !comment.isBlank() && comment.length() <= MAX_COMMENT_LENGTH;
    }

    private boolean isReviewer(Review review, UUID userId) {
        return review.getReviewerId() != null && review.getReviewerId().equals(userId);
    }
}