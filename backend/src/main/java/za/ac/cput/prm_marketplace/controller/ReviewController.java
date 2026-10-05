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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

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
 *
 * <p>The service additionally requires that the caller actually bought the product, and that the
 * comment fits {@code reviews.comment varchar(255) not null}. A submission failing either check is
 * reported as not found rather than as a server error, because both are ordinary client input
 * problems and the previous behaviour of letting them reach the database returned a 500.
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
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Nothing to return: the change was applied and there is no state left to read.")
    })
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
