package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.ProductImage;

import java.util.List;
import java.util.UUID;

/**
 * The photographs on a product.
 *
 * <p>Reads are public, because they are part of the listing. Writes belong to the seller whose
 * product the image hangs on, and every write method takes the caller for that reason: the previous
 * version took the product from the request body, so any caller could attach an image to any
 * product, move an existing image onto somebody else's listing, or wipe every image off a
 * competitor's product.
 *
 * <p>{@code getAll} is gone. It returned every image row in the system, which is every seller's
 * photography in one unordered list, and the same information is available per product.
 */
public interface IProductImageService {

    /**
     * Attaches an image to one of the caller's own products.
     *
     * <p>The product is named by the path rather than the body. Returns null when the product is
     * not the caller's or the image has no URL.
     */
    ProductImage create(UUID productId, ProductImage image, UUID requesterId);

    /** An image by id. Public. */
    ProductImage read(UUID id);

    /**
     * Updates one of the caller's own images. The product it belongs to is immutable: only the
     * URL, sort order and primary flag move.
     *
     * <p>Setting the primary flag clears it on the product's previous primary image, so a product
     * can only ever have one. Returns null when the image does not exist or is not the caller's.
     */
    ProductImage update(UUID id, ProductImage image, UUID requesterId);

    /** Removes one of the caller's own images. */
    boolean delete(UUID id, UUID requesterId);

    /** Every image on a product, in display order. Public. */
    List<ProductImage> getByProduct(UUID productId);

    /** The product's primary image. Public. */
    ProductImage getPrimary(UUID productId);

    /** Removes every image on one of the caller's own products. */
    boolean deleteByProduct(UUID productId, UUID requesterId);
}