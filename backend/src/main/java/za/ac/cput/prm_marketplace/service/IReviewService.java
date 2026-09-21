package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Review;

import java.util.List;
import java.util.UUID;

public interface IReviewService {

    Review create(Review review);

    Review read(UUID id);

    Review update(Review review);

    boolean delete(UUID id);

    List<Review> getAll();
}