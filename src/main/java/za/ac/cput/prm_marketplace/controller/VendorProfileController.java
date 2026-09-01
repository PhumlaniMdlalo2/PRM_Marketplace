package za.ac.cput.prm_marketplace.controller;

import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.service.IVendorProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vendor-profiles")
public class VendorProfileController {

    private final IVendorProfileService vendorProfileService;

    public VendorProfileController(IVendorProfileService vendorProfileService) {
        this.vendorProfileService = vendorProfileService;
    }

    @PostMapping
    public ResponseEntity<VendorProfile> create(@RequestBody VendorProfile vendorProfile) {
        VendorProfile created = vendorProfileService.create(vendorProfile);
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorProfile> read(@PathVariable UUID id) {
        VendorProfile vendorProfile = vendorProfileService.read(id);
        if (vendorProfile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(vendorProfile);
    }

    @PutMapping
    public ResponseEntity<VendorProfile> update(@RequestBody VendorProfile vendorProfile) {
        VendorProfile updated = vendorProfileService.update(vendorProfile);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        boolean deleted = vendorProfileService.delete(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<VendorProfile>> getAll() {
        return ResponseEntity.ok(vendorProfileService.getAll());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<VendorProfile> findByUserId(@PathVariable UUID userId) {
        return vendorProfileService.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}