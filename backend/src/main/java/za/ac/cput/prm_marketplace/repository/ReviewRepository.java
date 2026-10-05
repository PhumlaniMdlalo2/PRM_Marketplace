package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.ac.cput.prm_marketplace.domain.Review;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByProductId(UUID productId);

    List<Review> findByReviewerId(UUID reviewerId);

    /**
     * The mean rating across every review of every product this vendor sells.
     *
     * <p>Vendor-wide rather than per product, because the number the listing page shows belongs to the
     * seller: a buyer deciding whether to trust a vendor is reading the vendor's whole record.
     *
     * <p>Written as a theta-style join over two roots because {@code Review} holds a bare
     * {@code productId} UUID rather than a mapped {@code Product} association, so there is no path to
     * walk with a dot. Null when the vendor has no reviews at all, which the caller turns into a null
     * average rather than a zero — "no ratings" and "rated zero" are not the same claim.
     */
    @Query("select avg(r.rating) from Review r, Product p "
            + "where p.id = r.productId and p.vendor.id = :vendorId")
    Double averageRatingForVendor(@Param("vendorId") UUID vendorId);

    /** How many reviews {@link #averageRatingForVendor} is drawn from. */
    @Query("select count(r) from Review r, Product p "
            + "where p.id = r.productId and p.vendor.id = :vendorId")
    long reviewCountForVendor(@Param("vendorId") UUID vendorId);
}