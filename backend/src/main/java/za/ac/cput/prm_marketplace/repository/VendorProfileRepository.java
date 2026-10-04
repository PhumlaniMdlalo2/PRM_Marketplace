package za.ac.cput.prm_marketplace.repository;

import za.ac.cput.prm_marketplace.domain.VendorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VendorProfileRepository extends JpaRepository<VendorProfile, UUID> {

    Optional<VendorProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    /**
     * Loads a profile only when it belongs to the given account.
     *
     * <p>A profile is owned by its user, and every write goes through this rather than
     * {@code findById}, so one seller cannot edit or delete another's business details.
     */
    Optional<VendorProfile> findByIdAndUserId(UUID id, UUID userId);
}