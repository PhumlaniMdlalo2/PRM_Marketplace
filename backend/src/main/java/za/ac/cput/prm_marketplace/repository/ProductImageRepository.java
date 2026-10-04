package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.ProductImage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdOrderBySortOrderAsc(UUID productId);

    Optional<ProductImage> findByProductIdAndPrimaryTrue(UUID productId);

    void deleteByProductId(UUID productId);

    /**
     * Ownership of an image is ownership of its product: product to vendor to user. An image stores
     * no owner id of its own, so these are how a route checks that the caller is the seller whose
     * listing the image belongs to.
     */
    Optional<ProductImage> findByIdAndProductVendorUserId(UUID id, UUID userId);

    List<ProductImage> findByProductIdAndProductVendorUserIdOrderBySortOrderAsc(UUID productId, UUID userId);

    boolean existsByProductIdAndProductVendorUserId(UUID productId, UUID userId);

    /** Every image on a product, so a new primary can displace the old one. */
    List<ProductImage> findByProductId(UUID productId);
}