package za.ac.cput.prm_marketplace.repository;

import za.ac.cput.prm_marketplace.domain.VendorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VendorProfileRepository extends JpaRepository<VendorProfile, UUID> {

    // These three are written as explicit queries rather than derived ones because
    // VendorProfile now has a `userId` property — the read-only accessor that hands the frontend the
    // owner's id for the Contact button. Spring Data resolves `findByUserId` against that property
    // before it tries to split the name into a path, and there is no `userId` field on the entity, so
    // every one of them failed at runtime with "Could not resolve attribute 'userId'" and
    // GET /api/products/mine answered 500. Naming the traversal explicitly is what makes it
    // unambiguous again: the account behind the profile is `user.id`.
    @Query("select v from VendorProfile v where v.user.id = :userId")
    Optional<VendorProfile> findByUserId(@Param("userId") UUID userId);

    @Query("select count(v) > 0 from VendorProfile v where v.user.id = :userId")
    boolean existsByUserId(@Param("userId") UUID userId);

    /**
     * Loads a profile only when it belongs to the given account.
     *
     * <p>A profile is owned by its user, and every write goes through this rather than
     * {@code findById}, so one seller cannot edit or delete another's business details.
     */
    @Query("select v from VendorProfile v where v.id = :id and v.user.id = :userId")
    Optional<VendorProfile> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}