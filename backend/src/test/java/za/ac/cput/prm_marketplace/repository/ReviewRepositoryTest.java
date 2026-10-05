package za.ac.cput.prm_marketplace.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductCondition;
import za.ac.cput.prm_marketplace.domain.Review;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Runs the vendor rating aggregate against a real database.
 *
 * <p>It is a theta-style join over two roots rather than an association walk, because {@code Review}
 * holds a bare {@code productId} with no mapped {@code Product} on it. A query written that way
 * compiles happily and only fails when it is executed, which is why the mocked
 * {@code VendorRatingServiceTest} cannot vouch for it: the mock returns whatever the test told it to
 * return, and the join is never evaluated by a database.
 *
 * <p>The aggregate also decides what a seller with no reviews is shown as, which is why the null case
 * is pinned here rather than left to the service.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VendorProfileRepository vendorProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private VendorProfile vendor;
    private VendorProfile otherVendor;
    private Product textbook;
    private Product labCoat;
    private User reviewer;

    @BeforeEach
    void setUp() {
        reviewer = userRepository.save(buildUser("reviewer", Role.STUDENT));
        vendor = saveVendor("Block B Books");
        otherVendor = saveVendor("Residence C Spares");
        textbook = saveProduct("Calculus textbook", vendor);
        labCoat = saveProduct("Lab coat", vendor);
    }

    @Test
    @DisplayName("the average spans every product a vendor sells, not one listing")
    void averageRatingForVendor_drawsFromAllOfTheVendorsListings() {
        saveReview(textbook, 5);
        saveReview(labCoat, 3);

        assertThat(reviewRepository.averageRatingForVendor(vendor.getId()))
                .isCloseTo(4.0, within(0.001));
        assertThat(reviewRepository.reviewCountForVendor(vendor.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("another vendor's reviews are not averaged into this one")
    void averageRatingForVendor_excludesReviewsOfOtherVendorsListings() {
        saveReview(textbook, 5);
        saveReview(saveProduct("Ruler", otherVendor), 1);

        assertThat(reviewRepository.averageRatingForVendor(vendor.getId()))
                .isCloseTo(5.0, within(0.001));
        assertThat(reviewRepository.reviewCountForVendor(vendor.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("a review pointing at a listing that no longer exists is left out of the join")
    void averageRatingForVendor_ignoresReviewsWithNoMatchingProduct() {
        saveReview(textbook, 4);
        // The listing was deleted while the review row survived, which is possible because Review
        // stores a plain UUID and the database has no foreign key to stop it.
        saveReviewWithProductId(UUID.randomUUID(), 1);

        assertThat(reviewRepository.averageRatingForVendor(vendor.getId()))
                .isCloseTo(4.0, within(0.001));
        assertThat(reviewRepository.reviewCountForVendor(vendor.getId()))
                .as("counting the orphan would inflate the number beside the average")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("a vendor with no reviews has no average rather than an average of zero")
    void averageRatingForVendor_isNullForAVendorWithNoReviews() {
        saveReview(textbook, 4);

        assertThat(reviewRepository.averageRatingForVendor(otherVendor.getId()))
                .as("no ratings and rated zero are different claims, and only one of them is true")
                .isNull();
        assertThat(reviewRepository.reviewCountForVendor(otherVendor.getId())).isZero();
    }

    @Test
    @DisplayName("a single review is its own average")
    void averageRatingForVendor_withOneReview_isThatRating() {
        saveReview(textbook, 2);

        assertThat(reviewRepository.averageRatingForVendor(vendor.getId()))
                .isCloseTo(2.0, within(0.001));
        assertThat(reviewRepository.reviewCountForVendor(vendor.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("reviews are listed for one listing and for one reviewer")
    void findByProductIdAndReviewerId_scopeToTheirOwnRows() {
        saveReview(textbook, 5);
        saveReview(labCoat, 3);

        assertThat(reviewRepository.findByProductId(textbook.getId()))
                .extracting(Review::getRating)
                .containsExactly(5);
        assertThat(reviewRepository.findByReviewerId(reviewer.getId()))
                .extracting(Review::getProductId)
                .containsExactlyInAnyOrder(textbook.getId(), labCoat.getId());
    }

    @Test
    @DisplayName("another reviewer's reviews are not returned")
    void findByReviewerId_excludesOtherReviewers() {
        User someoneElse = userRepository.save(buildUser("someone-else", Role.STUDENT));
        saveReview(textbook, 5);
        reviewRepository.save(new Review.Builder()
                .setProductId(labCoat.getId())
                .setReviewerId(someoneElse.getId())
                .setRating(1)
                .setComment("Mine.")
                .build());

        assertThat(reviewRepository.findByReviewerId(reviewer.getId()))
                .extracting(Review::getRating)
                .containsExactly(5);
    }

    private void saveReview(Product product, int rating) {
        saveReviewWithProductId(product.getId(), rating);
    }

    private void saveReviewWithProductId(UUID productId, int rating) {
        reviewRepository.save(new Review.Builder()
                .setProductId(productId)
                .setReviewerId(reviewer.getId())
                .setRating(rating)
                .setComment("Arrived as described.")
                .build());
    }

    private VendorProfile saveVendor(String businessName) {
        return vendorProfileRepository.save(new VendorProfile.Builder()
                .setUser(userRepository.save(buildUser(businessName, Role.VENDOR)))
                .setBusinessName(businessName)
                .setRegistrationNo("REG-" + UUID.randomUUID())
                .build());
    }

    private Product saveProduct(String name, VendorProfile profile) {
        return productRepository.save(new Product.Builder()
                .vendor(profile)
                .name(name)
                .description("Second edition.")
                .price(new BigDecimal("250"))
                .stockQuantity(3)
                .category("Books")
                .condition(ProductCondition.GOOD)
                .build());
    }

    private User buildUser(String prefix, Role role) {
        return new User.Builder()
                .setName(prefix)
                .setEmail(prefix.toLowerCase() + "-" + UUID.randomUUID() + "@example.com")
                .setPasswordHash("hash")
                .setRole(role)
                .setVerified(true)
                .build();
    }
}