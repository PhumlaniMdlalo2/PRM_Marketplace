package za.ac.cput.prm_marketplace.repository;

import za.ac.cput.prm_marketplace.domain.VendorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VendorProfileRepository extends JpaRepository<VendorProfile, UUID> {

    Optional<VendorProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}