package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.dto.ProductSearchCriteria;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

/**
 * Product listings.
 *
 * <p>Browsing is public: anyone may list, read, search, and filter by vendor or category, and
 * those stay unauthenticated. Everything that changes a listing takes the caller, because a listing
 * belongs to the vendor whose profile it sits under and the previous signatures trusted an id from
 * the request.
 *
 * <p>{@code delete} is gone rather than guarded, and this is the same reason as for vendor
 * profiles: a product is referenced by the order lines that bought it, through a non-nullable
 * foreign key. Deleting a product that has been sold fails on that key and the endpoint answers
 * 500. The {@code active} flag is the supported way to retire a listing, and the vendor sets it
 * through {@link #update}.
 */
public interface IProductService {

    /**
     * Lists a new product under the caller's own vendor profile.
     *
     * <p>The vendor is the caller's, never the body's, and the listing starts active. Returns null
     * if the caller has no vendor profile or the details fail validation.
     */
    Product create(Product product, UUID requesterId);

    /** A product by id. Public. */
    Product read(UUID id);

    /**
     * Updates one of the caller's own listings.
     *
     * <p>The vendor and creation time are carried over from the stored row, so a request cannot
     * move a listing to another seller or rewrite its history. Returns null when the product does
     * not exist or is not the caller's.
     */
    Product update(UUID id, Product product, UUID requesterId);

    /** The caller's own listings, newest first. */
    List<Product> getMine(UUID requesterId);

    /** The whole catalogue. Public. */
    List<Product> getAll();

    List<Product> getByVendor(UUID vendorId);

    List<Product> getByCategory(String category);

    Page<Product> search(ProductSearchCriteria criteria);
}