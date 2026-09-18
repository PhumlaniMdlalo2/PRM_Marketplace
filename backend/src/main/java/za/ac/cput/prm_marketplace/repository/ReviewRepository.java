package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.cput.prm_marketplace.domain.Review;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByProductId(UUID productId);

    List<Review> findByReviewerId(UUID reviewerId);
}