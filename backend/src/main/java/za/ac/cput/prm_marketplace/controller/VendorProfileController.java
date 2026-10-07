package za.ac.cput.prm_marketplace.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.dto.SellerPayoutDetails;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IVendorProfileService;

import java.util.List;
import java.util.UUID;

/**
 * Mounted under "/api" to match the rest of the application; the old "/vendor-profiles" mapping sat
 * outside the group the security rules are written against.
 *
 * <p>Reads stay public because this is the seller directory. Writes are scoped to the caller: the
 * previous controller accepted the owner and the verified flag in the body, took the profile id
 * from the body on update, and exposed {@code GET /vendor-profiles/user/{userId}}.
 */
@RestController
@RequestMapping("/api/vendor-profiles")
public class VendorProfileController {

    private final IVendorProfileService vendorProfileService;

    public VendorProfileController(IVendorProfileService vendorProfileService) {
        this.vendorProfileService = vendorProfileService;
    }

    /**
     * Opens a seller profile for the caller. The body's user and verified flag are ignored: the
     * owner is the token and a new profile is never verified.
     */
    @PostMapping
    public ResponseEntity<VendorProfile> create(@RequestBody VendorProfile profile,
                                                Authentication authentication) {
        VendorProfile created = vendorProfileService.create(profile,
                CurrentCaller.id(authentication), CurrentCaller.role(authentication));
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /** One seller, public. */
    @GetMapping("/{id}")
    public ResponseEntity<VendorProfile> read(@PathVariable UUID id) {
        VendorProfile profile = vendorProfileService.read(id);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profile);
    }

    /** Updates the caller's own profile. Another seller's profile is reported as not found. */
    @PutMapping("/{id}")
    public ResponseEntity<VendorProfile> update(@PathVariable UUID id,
                                                @RequestBody VendorProfile profile,
                                                Authentication authentication) {
        VendorProfile updated = vendorProfileService.update(id, profile, CurrentCaller.id(authentication));
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /**
     * Grants or withdraws a seller's verified badge. Admin only.
     *
     * <p>Separate from {@code PUT /{id}} on purpose. That endpoint rebuilds the profile from the
     * stored row and carries the verified flag over, so it cannot be used to grant verification even
     * though it accepts the whole entity in the body — mixing the two would have meant trusting the
     * body's {@code verified} field, which is exactly how a seller would mark themselves trusted.
     *
     * <p>The decision arrives as a parameter rather than a body field so that "no value" cannot be
     * confused with "false": the request always states which way it is going.
     *
     * <p>Anything other than admin is reported as not found, matching report resolution, so this
     * cannot be used to find out whether a given profile exists.
     */
    @PatchMapping("/{id}/verification")
    public ResponseEntity<VendorProfile> verify(@PathVariable UUID id,
                                                @RequestParam boolean verified,
                                                Authentication authentication) {
        VendorProfile updated = vendorProfileService.verify(id, verified,
                CurrentCaller.id(authentication), CurrentCaller.role(authentication));
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /** The public seller directory. */
    @GetMapping
    public ResponseEntity<List<VendorProfile>> getAll() {
        return ResponseEntity.ok(vendorProfileService.getAll());
    }

    /**
     * The caller's own profile. This replaces {@code GET /vendor-profiles/user/{userId}}, which let
     * any caller map a user id to the business behind it.
     */
    @GetMapping("/me")
    public ResponseEntity<VendorProfile> findMine(Authentication authentication) {
        VendorProfile profile = vendorProfileService.findMine(CurrentCaller.id(authentication));
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/me/payout-details")
    public ResponseEntity<SellerPayoutDetails> getMyPayoutDetails(Authentication authentication) {
        Role role = CurrentCaller.role(authentication);
        if (role != Role.VENDOR && role != Role.STUDENT) {
            return ResponseEntity.notFound().build();
        }
        SellerPayoutDetails details = vendorProfileService.getMyPayoutDetails(CurrentCaller.id(authentication));
        if (details == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(details);
    }

    @PutMapping("/me/payout-details")
    public ResponseEntity<SellerPayoutDetails> updateMyPayoutDetails(
            @RequestBody SellerPayoutDetails details,
            Authentication authentication) {
        Role role = CurrentCaller.role(authentication);
        if (role != Role.VENDOR && role != Role.STUDENT) {
            return ResponseEntity.notFound().build();
        }
        SellerPayoutDetails updated = vendorProfileService.updateMyPayoutDetails(
                CurrentCaller.id(authentication), details);
        if (updated == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(updated);
    }
}