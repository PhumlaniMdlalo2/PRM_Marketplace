package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.ProductImage;
import za.ac.cput.prm_marketplace.service.IProductImageService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product-images")
public class ProductImageController {

    private final IProductImageService productImageService;

    @Autowired
    public ProductImageController(IProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    @PostMapping
    public ResponseEntity<ProductImage> create(@RequestBody ProductImage image) {
        ProductImage created = productImageService.create(image);
        if (created == null) {
            return ResponseEntity.badRequest().build();
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

    @PutMapping("/{id}")
    public ResponseEntity<ProductImage> update(@PathVariable UUID id, @RequestBody ProductImage image) {
        ProductImage existing = productImageService.read(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        ProductImage toUpdate = new ProductImage.Builder().copy(image).setId(id).build();
        return ResponseEntity.ok(productImageService.update(toUpdate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!productImageService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ProductImage>> getAll() {
        return ResponseEntity.ok(productImageService.getAll());
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

    @DeleteMapping("/product/{productId}")
    public ResponseEntity<Void> deleteByProduct(@PathVariable UUID productId) {
        if (!productImageService.deleteByProduct(productId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}