package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.ReviewRepository;
import za.ac.cput.prm_marketplace.repository.VendorProfileRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@code vendor_profiles.rating_avg} existed from the first migration and nothing ever wrote it, so
 * every seller showed buyers no rating at all no matter how many reviews they had. These tests pin
 * the recalculation that fixes that, and pin the cases that would quietly put a wrong number on a
 * listing.
 */
@ExtendWith(MockitoExtension.class)
class VendorRatingServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private VendorProfileRepository vendorProfileRepository;

    private VendorRatingService service;

    private UUID productId;
    private UUID vendorId;

    @BeforeEach
    void setUp() {
        service = new VendorRatingService(productRepository, reviewRepository, vendorProfileRepository);
        productId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
    }

    private VendorProfile vendor(boolean verified, java.math.BigDecimal ratingAvg, int ratingCount) {
        return new VendorProfile.Builder()
                .setId(vendorId)
                .setUser(new User.Builder()
                        .setId(UUID.randomUUID())
                        .setName("Seller")
                        .setEmail("seller@example.ac.za")
                        .setPasswordHash("hash")
                        .build())
                .setBusinessName("Seller")
                .setVerified(verified)
                .setRatingAvg(ratingAvg)
                .setRatingCount(ratingCount)
                .build();
    }

    private Product productOwnedBy(VendorProfile owner) {
        return Product.builder()
                .id(productId)
                .vendor(owner)
                .name("A listing")
                .price(new java.math.BigDecimal("10.00"))
                .build();
    }

    private VendorProfile captureSaved() {
        ArgumentCaptor<VendorProfile> captor = ArgumentCaptor.forClass(VendorProfile.class);
        verify(vendorProfileRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("stores the average of every review left against the vendor's listings")
    void refresh_writesTheAverageAndCount() {
        VendorProfile stored = vendor(false, null, 0);
        when(vendorProfileRepository.findById(vendorId)).thenReturn(Optional.of(stored));
        when(reviewRepository.averageRatingForVendor(vendorId)).thenReturn(4.5d);
        when(reviewRepository.reviewCountForVendor(vendorId)).thenReturn(6L);

        service.refresh(vendorId);

        VendorProfile saved = captureSaved();
        assertThat(saved.getRatingAvg()).isEqualByComparingTo("4.50");
        assertThat(saved.getRatingCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("clears the average when the last review goes, rather than leaving a stale score")
    void refresh_noReviewsClearsTheAverage() {
        // This is the case that matters most. Adjusting an average incrementally would leave a seller
        // rated 5.00 forever after their only review was deleted; "no reviews" has to read as no
        // rating, not as a perfect or a zero.
        VendorProfile stored = vendor(true, new java.math.BigDecimal("5.00"), 1);
        when(vendorProfileRepository.findById(vendorId)).thenReturn(Optional.of(stored));
        when(reviewRepository.averageRatingForVendor(vendorId)).thenReturn(null);
        when(reviewRepository.reviewCountForVendor(vendorId)).thenReturn(0L);

        service.refresh(vendorId);

        VendorProfile saved = captureSaved();
        assertThat(saved.getRatingAvg()).isNull();
        assertThat(saved.getRatingCount()).isZero();
    }

    @Test
    @DisplayName("a null average with a non-zero count is treated as no rating, not as zero")
    void refresh_nullAverageIsNotZero() {
        // AVG over an empty set is null, but the two are worth separating: a stored 0.00 would render
        // on a listing as a damning score for a seller nobody has reviewed.
        VendorProfile stored = vendor(false, null, 0);
        when(vendorProfileRepository.findById(vendorId)).thenReturn(Optional.of(stored));
        when(reviewRepository.averageRatingForVendor(vendorId)).thenReturn(null);
        when(reviewRepository.reviewCountForVendor(vendorId)).thenReturn(3L);

        service.refresh(vendorId);

        assertThat(captureSaved().getRatingAvg()).isNull();
        assertThat(captureSaved().getRatingCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("rounds to the two decimals the column holds")
    void refresh_roundsToTheColumnScale() {
        VendorProfile stored = vendor(false, null, 0);
        when(vendorProfileRepository.findById(vendorId)).thenReturn(Optional.of(stored));
        // 4.333... cannot go into decimal(38,2) unrounded, and letting the driver decide would make
        // the stored value depend on the rounding mode in the JDBC driver.
        when(reviewRepository.averageRatingForVendor(vendorId)).thenReturn(4.3333333d);
        when(reviewRepository.reviewCountForVendor(vendorId)).thenReturn(3L);

        service.refresh(vendorId);

        assertThat(captureSaved().getRatingAvg()).isEqualByComparingTo("4.33");
    }

    @Test
    @DisplayName("carries verification and the business details through untouched")
    void refresh_leavesTheRestOfTheProfileAlone() {
        VendorProfile stored = vendor(true, null, 0);
        when(vendorProfileRepository.findById(vendorId)).thenReturn(Optional.of(stored));
        when(reviewRepository.averageRatingForVendor(vendorId)).thenReturn(4.0d);
        when(reviewRepository.reviewCountForVendor(vendorId)).thenReturn(2L);

        service.refresh(vendorId);

        VendorProfile saved = captureSaved();
        // Recalculating a rating must not un-verify a seller, and must not rewrite their name.
        assertThat(saved.isVerified()).isTrue();
        assertThat(saved.getBusinessName()).isEqualTo("Seller");
        assertThat(saved.getUser().getId()).isEqualTo(stored.getUser().getId());
    }

    @Test
    @DisplayName("resolves the vendor from the product, which is what the review side holds")
    void refreshForProduct_walksProductToVendor() {
        VendorProfile owner = vendor(false, null, 0);
        when(productRepository.findById(productId)).thenReturn(Optional.of(productOwnedBy(owner)));
        when(vendorProfileRepository.findById(vendorId)).thenReturn(Optional.of(owner));
        when(reviewRepository.averageRatingForVendor(vendorId)).thenReturn(3.0d);
        when(reviewRepository.reviewCountForVendor(vendorId)).thenReturn(1L);

        service.refreshForProduct(productId);

        // The same vendor, reached through the listing the review was left on.
        verify(reviewRepository).averageRatingForVendor(vendorId);
        assertThat(captureSaved().getRatingAvg()).isEqualByComparingTo("3.00");
    }

    @Test
    @DisplayName("does nothing when the product has gone")
    void refreshForProduct_ignoresAMissingProduct() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        service.refreshForProduct(productId);

        verifyNoInteractions(reviewRepository);
        verify(vendorProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("does nothing when the profile has gone")
    void refresh_ignoresAMissingProfile() {
        when(vendorProfileRepository.findById(vendorId)).thenReturn(Optional.empty());

        // Stops before touching the review aggregate: there is no row left to write a rating onto.
        service.refresh(vendorId);

        verifyNoInteractions(reviewRepository);
        verify(vendorProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("ignores a null id rather than failing")
    void refresh_ignoresNulls() {
        service.refreshForProduct(null);
        service.refresh(null);

        verifyNoInteractions(reviewRepository);
        verify(vendorProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("is idempotent, so a repeat or a retried call converges")
    void refresh_isIdempotent() {
        VendorProfile stored = vendor(false, null, 0);
        when(vendorProfileRepository.findById(vendorId)).thenReturn(Optional.of(stored));
        when(reviewRepository.averageRatingForVendor(vendorId)).thenReturn(4.0d);
        when(reviewRepository.reviewCountForVendor(vendorId)).thenReturn(2L);

        service.refresh(vendorId);
        service.refresh(vendorId);

        // Both runs read the same reviews and write the same numbers. This is why the average is
        // recalculated rather than adjusted: an incremental update has to be correct to begin with,
        // and nothing would notice if it silently were not.
        verify(vendorProfileRepository, org.mockito.Mockito.times(2)).save(any());
    }
}