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
 * 500. Retiring is the supported way to take a listing down, through {@link #retire}, so that the
 * intent is always visible in the request rather than hidden in an update body.
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
     * move a listing to another seller or rewrite its history. The {@code active} flag is carried
     * over too and is deliberately not read from the body: {@code Product.active} is a primitive
     * boolean, so a body that simply omits it deserialises as false and every such request would
     * quietly retire the listing. Use {@link #retire} and {@link #reactivate} to change it.
     *
     * <p>Returns null when the product does not exist or is not the caller's.
     */
    Product update(UUID id, Product product, UUID requesterId);

    /**
     * Takes one of the caller's own listings down without removing it.
     *
     * @return false when the listing does not exist or belongs to somebody else
     */
    boolean retire(UUID id, UUID requesterId);

    /** Puts a retired listing back on sale. False when it is not the caller's. */
    boolean reactivate(UUID id, UUID requesterId);

    /**
     * The caller's own listings, newest first, retired ones included: a seller has to be able to see
     * and undo what they took down.
     */
    List<Product> getMine(UUID requesterId);

    /** The catalogue as a buyer sees it, which means live listings only. Public. */
    List<Product> getAll();

    /** A vendor's live listings. Public. */
    List<Product> getByVendor(UUID vendorId);

    /** Live listings in one category. Public. */
    List<Product> getByCategory(String category);

    Page<Product> search(ProductSearchCriteria criteria);
}