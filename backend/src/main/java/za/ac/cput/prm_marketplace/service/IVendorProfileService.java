package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.VendorProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IVendorProfileService {

    VendorProfile create(VendorProfile vendorProfile);

    VendorProfile read(UUID id);

    VendorProfile update(VendorProfile vendorProfile);

    boolean delete(UUID id);

    List<VendorProfile> getAll();

    Optional<VendorProfile> findByUserId(UUID userId);
}