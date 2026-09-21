package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import za.ac.cput.prm_marketplace.domain.Review;
import za.ac.cput.prm_marketplace.repository.ReviewRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewServiceImpl implements IReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    public Review create(Review review) {
        if (review == null) {
            return null;
        }
        return reviewRepository.save(review);
    }

    @Override
    public Review read(UUID id) {
        if (id == null) {
            return null;
        }
        return reviewRepository.findById(id).orElse(null);
    }

    @Override
    public Review update(Review review) {
        if (review == null || review.getId() == null ||
                !reviewRepository.existsById(review.getId())) {
            return null;
        }
        return reviewRepository.save(review);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !reviewRepository.existsById(id)) {
            return false;
        }

        reviewRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Review> getAll() {
        return reviewRepository.findAll();
    }
}