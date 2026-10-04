package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.VendorProfile;

import java.util.List;
import java.util.UUID;

/**
 * A vendor's business profile.
 *
 * <p>The profile itself is public: buyers browse the seller directory, so {@link #read(UUID)} and
 * {@link #getAll()} stay open. Everything that changes a profile takes the caller, because a
 * profile belongs to exactly one account and the previous signature trusted an id in the body.
 *
 * <p>{@code delete} is gone rather than guarded. A profile is referenced by every product the
 * seller has listed, through a non-nullable foreign key, so deleting one cannot succeed once the
 * seller has any stock: the database rejects it and the endpoint answers 500. Retiring a seller
 * needs a flag on the profile, which this entity does not have yet.
 */
public interface IVendorProfileService {

    /**
     * Creates a profile for the caller.
     *
     * <p>The owner is the caller, {@code verified} starts false however the body asks, and the
     * caller must hold the VENDOR role and not already have a profile. Returns null otherwise.
     */
    VendorProfile create(VendorProfile profile, UUID requesterId, Role requesterRole);

    /** A profile by id. Public: this is the seller directory. */
    VendorProfile read(UUID id);

    /**
     * Updates the caller's own profile. Only the business name and registration number move; the
     * owner, verified flag, rating and creation time are carried over from the stored row.
     * Returns null when there is no such profile for this caller.
     */
    VendorProfile update(UUID id, VendorProfile profile, UUID requesterId);

    /** The public seller directory. */
    List<VendorProfile> getAll();

    /** The caller's own profile, or null if they have not created one. */
    VendorProfile findMine(UUID requesterId);
}