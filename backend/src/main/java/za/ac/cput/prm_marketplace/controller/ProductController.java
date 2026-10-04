package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductCondition;
import za.ac.cput.prm_marketplace.dto.PageResponse;
import za.ac.cput.prm_marketplace.dto.ProductSearchCriteria;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Browsing is public; writing is not.
 *
 * <p>{@code POST}, {@code PUT} and the seller's own listing take the caller, because the previous
 * version published a product carrying a vendor taken from the request body, and let any caller
 * edit or delete any listing by id.
 *
 * <p>There is no delete route. A product that has been ordered is referenced by the order line,
 * so removing the row fails on the foreign key. A seller takes a listing down with
 * {@code POST /{id}/retire} instead, which is a route of its own rather than a field in the update
 * body: {@code Product.active} is a primitive boolean, so an update that omits it deserialises as
 * false and every ordinary price edit would quietly retire the listing.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final IProductService productService;

    @Autowired
    public ProductController(IProductService productService) {
        this.productService = productService;
    }

    /**
     * Lists a product under the caller's own vendor profile. The body's vendor, active flag and
     * creation time are ignored: the entity marks them read-only and the service rebuilds the
     * listing from the stored data.
     */
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product, Authentication authentication) {
        Product created = productService.create(product, CurrentCaller.id(authentication));
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<Product>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) ProductCondition condition,
            @RequestParam(defaultValue = "true") Boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        ProductSearchCriteria criteria = new ProductSearchCriteria(
                keyword, category, city, minPrice, maxPrice,
                condition, activeOnly, page, size, sortBy, direction);

        Page<Product> results = productService.search(criteria);
        return ResponseEntity.ok(PageResponse.of(results));
    }

    /** The caller's own listings. */
    @GetMapping("/mine")
    public ResponseEntity<List<Product>> getMine(Authentication authentication) {
        return ResponseEntity.ok(productService.getMine(CurrentCaller.id(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> read(@PathVariable UUID id) {
        Product product = productService.read(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    /**
     * Updates one of the caller's own listings. Somebody else's is reported as not found.
     *
     * <p>The body's {@code active} flag is ignored: the stored value is kept, so editing a price can
     * never take a listing off sale.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable UUID id,
                                          @RequestBody Product product,
                                          Authentication authentication) {
        Product updated = productService.update(id, product, CurrentCaller.id(authentication));
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /**
     * Takes one of the caller's own listings off sale. Somebody else's is reported as not found.
     *
     * <p>Answers 204 either way the listing is found, whether or not it was already retired, so the
     * client does not have to know the current state to make the call idempotent.
     */
    @PostMapping("/{id}/retire")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Nothing to return: the change was applied and there is no state left to read.")
    })
    public ResponseEntity<Void> retire(@PathVariable UUID id, Authentication authentication) {
        if (!productService.retire(id, CurrentCaller.id(authentication))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /** Puts a retired listing back on sale. Same ownership rule as retire. */
    @PostMapping("/{id}/reactivate")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Nothing to return: the change was applied and there is no state left to read.")
    })
    public ResponseEntity<Void> reactivate(@PathVariable UUID id, Authentication authentication) {
        if (!productService.reactivate(id, CurrentCaller.id(authentication))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        return ResponseEntity.ok(productService.getAll());
    }

    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<List<Product>> getByVendor(@PathVariable UUID vendorId) {
        return ResponseEntity.ok(productService.getByVendor(vendorId));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getByCategory(category));
    }
}
