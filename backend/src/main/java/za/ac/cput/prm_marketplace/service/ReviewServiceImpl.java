package za.ac.cput.prm_marketplace.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Review;
import za.ac.cput.prm_marketplace.repository.ReviewRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewServiceImpl implements IReviewService {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    private final ReviewRepository reviewRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Transactional
    public Review create(Review review, UUID reviewerId) {
        if (review == null || reviewerId == null) {
            return null;
        }
        if (!isValidRating(review.getRating())) {
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
            return reviewRepository.save(created);
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
        return reviewRepository.save(updated);
    }

    @Override
    @Transactional
    public boolean delete(UUID id, UUID requesterId) {
        Review existing = reviewRepository.findById(id).orElse(null);
        if (existing == null || !isReviewer(existing, requesterId)) {
            return false;
        }
        reviewRepository.deleteById(id);
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

    private boolean isReviewer(Review review, UUID userId) {
        return review.getReviewerId() != null && review.getReviewerId().equals(userId);
    }
}