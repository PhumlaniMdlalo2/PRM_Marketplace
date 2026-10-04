package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.ProductImage;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IProductImageService;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * The photographs attached to a listing.
 *
 * <p>Creating takes the product from the path rather than the body, and every write takes the
 * caller. The previous controller accepted the product inside the request body and did not know
 * who was asking, so any authenticated caller could add an image to any product, re-point an
 * existing image at a different product, or delete every image on a listing.
 *
 * <p>{@code GET /api/product-images} is gone: it returned every image row in the system at once,
 * which is every seller's photography in one unordered list.
 */
@RestController
@RequestMapping("/api/product-images")
public class ProductImageController {

    private final IProductImageService productImageService;

    @Autowired
    public ProductImageController(IProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    /**
     * Attaches an image to one of the caller's own products. The body's product is ignored, and
     * flagging an image primary demotes whichever image held it before.
     */
    @PostMapping("/product/{productId}")
    public ResponseEntity<ProductImage> create(@PathVariable UUID productId,
                                               @RequestBody ProductImage image,
                                               Authentication authentication) {
        ProductImage created = productImageService.create(productId, image,
                CurrentCaller.id(authentication));
        if (created == null) {
            return ResponseEntity.notFound().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductImage> read(@PathVariable UUID id) {
        ProductImage image = productImageService.read(id);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(image);
    }

    /** Updates one of the caller's own images. An image on somebody else's product is not found. */
    @PutMapping("/{id}")
    public ResponseEntity<ProductImage> update(@PathVariable UUID id,
                                               @RequestBody ProductImage image,
                                               Authentication authentication) {
        ProductImage updated = productImageService.update(id, image, CurrentCaller.id(authentication));
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Nothing to return: the change was applied and there is no state left to read.")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        if (!productImageService.delete(id, CurrentCaller.id(authentication))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductImage>> getByProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(productImageService.getByProduct(productId));
    }

    @GetMapping("/product/{productId}/primary")
    public ResponseEntity<ProductImage> getPrimary(@PathVariable UUID productId) {
        ProductImage image = productImageService.getPrimary(productId);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(image);
    }

    /** Clears the gallery on one of the caller's own products. */
    @DeleteMapping("/product/{productId}")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Nothing to return: the change was applied and there is no state left to read.")
    })
    public ResponseEntity<Void> deleteByProduct(@PathVariable UUID productId,
                                                Authentication authentication) {
        if (!productImageService.deleteByProduct(productId, CurrentCaller.id(authentication))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
