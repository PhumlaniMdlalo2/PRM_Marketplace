package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Review;

import java.util.List;
import java.util.UUID;

/**
 * Reviews are public, but the reviewer is taken from the token and only the reviewer may edit or
 * remove their own. The reviewer id is stored rather than a nested user object, so it is never
 * taken from request data.
 */
public interface IReviewService {

    /** Creates a review written by the caller. */
    Review create(Review review, UUID reviewerId);

    Review read(UUID id);

    /** @return the updated review, or null when it does not exist or the caller is not the reviewer */
    Review update(Review review, UUID requesterId);

    /** @return false when it does not exist or the caller is not the reviewer */
    boolean delete(UUID id, UUID requesterId);

    List<Review> getAll();

    List<Review> getByProduct(UUID productId);

    /** The reviews written by one account. */
    List<Review> getByReviewer(UUID reviewerId);
}