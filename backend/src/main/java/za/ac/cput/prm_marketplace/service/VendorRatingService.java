package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.ReviewRepository;
import za.ac.cput.prm_marketplace.repository.VendorProfileRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Keeps a vendor's stored rating in step with the reviews actually left against their listings.
 *
 * <p>{@code vendor_profiles.rating_avg} existed from the first migration and was never written by
 * anything, so every seller showed no rating to buyers no matter how many reviews they had. The
 * number is a cache of the reviews table, not a source of truth: it is recalculated from scratch on
 * every change, which is why it cannot drift.
 *
 * <p>Recalculating the whole average rather than adjusting it incrementally is deliberate. An
 * incremental update has to be correct to begin with — one missed call leaves a wrong number on the
 * page forever, with nothing to notice it. A full recalculation is idempotent, so running it twice,
 * or running it after a failed write, converges on the same answer.
 *
 * <p>Nothing here is allowed to fail a review. A rating that could not be recalculated is a stale
 * number, whereas rejecting the review is a user-visible error over a cosmetic field, so a missing
 * product or profile is ignored rather than raised.
 */
@Service
public class VendorRatingService {

    /** Two decimal places, matching {@code vendor_profiles.rating_avg decimal(38,2)}. */
    private static final int AVERAGE_SCALE = 2;

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final VendorProfileRepository vendorProfileRepository;

    public VendorRatingService(ProductRepository productRepository,
                               ReviewRepository reviewRepository,
                               VendorProfileRepository vendorProfileRepository) {
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.vendorProfileRepository = vendorProfileRepository;
    }

    /**
     * Recalculates the average and count for the vendor who sells the given product.
     *
     * <p>Called after every write that changes what a vendor's reviews say: a review being filed,
     * edited or removed. Takes a product id rather than a vendor id because that is what the review
     * side of the code has to hand.
     */
    @Transactional
    public void refreshForProduct(UUID productId) {
        if (productId == null) {
            return;
        }
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || product.getVendor() == null) {
            return;
        }
        VendorProfile vendor = product.getVendor();
        if (vendor.getId() == null) {
            return;
        }
        refresh(vendor.getId());
    }

    /** Recalculates one vendor's stored rating from its reviews. */
    @Transactional
    public void refresh(UUID vendorId) {
        if (vendorId == null) {
            return;
        }
        VendorProfile vendor = vendorProfileRepository.findById(vendorId).orElse(null);
        if (vendor == null) {
            return;
        }

        Double average = reviewRepository.averageRatingForVendor(vendorId);
        long count = reviewRepository.reviewCountForVendor(vendorId);

        // No reviews means no average. Storing zero instead would put "rated 0.00" on the listing of
        // a seller nobody has reviewed yet, which reads as a damning score rather than an absence of
        // one — so the field goes back to null and the count says zero.
        BigDecimal ratingAvg = average == null || count == 0
                ? null
                : BigDecimal.valueOf(average).setScale(AVERAGE_SCALE, RoundingMode.HALF_UP);

        int ratingCount = (int) Math.min(count, Integer.MAX_VALUE);

        // Rebuilt through the builder rather than mutated, so the entity keeps its immutability and a
        // concurrent edit to the business name is not silently overwritten by a rating recalculation.
        vendorProfileRepository.save(new VendorProfile.Builder()
                .copy(vendor)
                .setRatingAvg(ratingAvg)
                .setRatingCount(ratingCount)
                .build());
    }
}