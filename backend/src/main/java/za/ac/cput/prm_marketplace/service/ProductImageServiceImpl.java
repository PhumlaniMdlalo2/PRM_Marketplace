package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductImage;
import za.ac.cput.prm_marketplace.repository.ProductImageRepository;
import za.ac.cput.prm_marketplace.repository.ProductRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ProductImageServiceImpl implements IProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;

    public ProductImageServiceImpl(ProductImageRepository productImageRepository,
                                   ProductRepository productRepository) {
        this.productImageRepository = productImageRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public ProductImage create(UUID productId, ProductImage image, UUID requesterId) {
        if (productId == null || image == null || requesterId == null) {
            return null;
        }
        if (image.getImageUrl() == null || image.getImageUrl().isBlank()) {
            return null;
        }

        // The product comes from the path, and is only accepted if the caller owns it. Loading it
        // this way, rather than taking the product off the body, is what keeps an image from being
        // attached to a competitor's listing.
        Product product = productRepository.findByIdAndVendorUserId(productId, requesterId).orElse(null);
        if (product == null) {
            return null;
        }

        ProductImage candidate = new ProductImage.Builder()
                .setId(null)
                .setProduct(product)
                .setImageUrl(image.getImageUrl().trim())
                .setSortOrder(image.getSortOrder())
                .setPrimary(image.isPrimary())
                .build();

        // A new primary demotes the old one. Without this a product could end up with several
        // images all flagged primary, and the gallery would have no defined cover shot.
        if (candidate.isPrimary()) {
            clearPreviousPrimary(productId);
        }

        return productImageRepository.save(candidate);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductImage read(UUID id) {
        if (id == null) {
            return null;
        }
        return productImageRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public ProductImage update(UUID id, ProductImage image, UUID requesterId) {
        if (id == null || image == null || requesterId == null) {
            return null;
        }

        ProductImage existing =
                productImageRepository.findByIdAndProductVendorUserId(id, requesterId).orElse(null);
        if (existing == null) {
            return null;
        }

        // Rebuilt from the stored row, so the owning product is carried over from the database. An
        // update therefore edits the picture on the seller's own product and cannot re-point the
        // row at a different product.
        ProductImage updated = new ProductImage.Builder()
                .copy(existing)
                .setImageUrl(image.getImageUrl() != null && !image.getImageUrl().isBlank()
                        ? image.getImageUrl().trim()
                        : existing.getImageUrl())
                .setSortOrder(image.getSortOrder())
                .setPrimary(image.isPrimary())
                .build();

        if (updated.isPrimary() && !existing.isPrimary()) {
            clearPreviousPrimary(existing.getProduct().getId());
        }

        return productImageRepository.save(updated);
    }

    @Override
    @Transactional
    public boolean delete(UUID id, UUID requesterId) {
        if (id == null || requesterId == null) {
            return false;
        }

        ProductImage existing =
                productImageRepository.findByIdAndProductVendorUserId(id, requesterId).orElse(null);
        if (existing == null) {
            return false;
        }

        productImageRepository.delete(existing);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImage> getByProduct(UUID productId) {
        if (productId == null) {
            return List.of();
        }
        return productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductImage getPrimary(UUID productId) {
        if (productId == null) {
            return null;
        }
        return productImageRepository.findByProductIdAndPrimaryTrue(productId).orElse(null);
    }

    @Override
    @Transactional
    public boolean deleteByProduct(UUID productId, UUID requesterId) {
        if (productId == null || requesterId == null) {
            return false;
        }

        if (!productImageRepository.existsByProductIdAndProductVendorUserId(productId, requesterId)) {
            return false;
        }

        productImageRepository.deleteByProductId(productId);
        return true;
    }

    /**
     * Takes the primary flag off whatever image currently holds it on this product.
     *
     * <p>The list is read inside the caller's transaction, so the demoted row is flushed with the
     * new image rather than in a separate statement.
     */
    private void clearPreviousPrimary(UUID productId) {
        List<ProductImage> current = productImageRepository.findByProductId(productId);
        for (ProductImage candidate : current) {
            if (candidate.isPrimary()) {
                candidate.setPrimary(false);
                productImageRepository.save(candidate);
            }
        }
    }
}