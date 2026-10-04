package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Review;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IReviewService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final IReviewService reviewService;

    @Autowired
    public ReviewController(IReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Writes a review as the caller. The reviewer used to be read from the body, so a review could
     * be attributed to another account.
     */
    @PostMapping
    public ResponseEntity<Review> create(@RequestBody Review review, Authentication authentication) {
        Review created = reviewService.create(review, CurrentCaller.id(authentication));

        if (created == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Review> read(@PathVariable UUID id) {
        Review review = reviewService.read(id);

        if (review == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(review);
    }

    /** @return 404 unless the caller wrote the review */
    @PutMapping
    public ResponseEntity<Review> update(@RequestBody Review review, Authentication authentication) {
        Review updated = reviewService.update(review, CurrentCaller.id(authentication));

        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updated);
    }

    /** @return 404 unless the caller wrote the review */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        boolean deleted = reviewService.delete(id, CurrentCaller.id(authentication));

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    /** Reviews are public, so every review may be listed. */
    @GetMapping
    public ResponseEntity<List<Review>> getAll() {
        return ResponseEntity.ok(reviewService.getAll());
    }

    /** Reviews of one product. */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Review>> getByProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(reviewService.getByProduct(productId));
    }

    /** The reviews written by one account. */
    @GetMapping("/reviewer/{reviewerId}")
    public ResponseEntity<List<Review>> getByReviewer(@PathVariable UUID reviewerId) {
        return ResponseEntity.ok(reviewService.getByReviewer(reviewerId));
    }
}