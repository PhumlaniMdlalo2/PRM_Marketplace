package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.repository.VendorProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VendorProfileServiceImpl implements IVendorProfileService {

    private final VendorProfileRepository vendorProfileRepository;

    public VendorProfileServiceImpl(VendorProfileRepository vendorProfileRepository) {
        this.vendorProfileRepository = vendorProfileRepository;
    }

    @Override
    public VendorProfile create(VendorProfile vendorProfile) {
        if (vendorProfile == null) {
            return null;
        }
        return vendorProfileRepository.save(vendorProfile);
    }

    @Override
    public VendorProfile read(UUID id) {
        if (id == null) {
            return null;
        }
        return vendorProfileRepository.findById(id).orElse(null);
    }

    @Override
    public VendorProfile update(VendorProfile vendorProfile) {
        if (vendorProfile == null || vendorProfile.getId() == null
                || !vendorProfileRepository.existsById(vendorProfile.getId())) {
            return null;
        }
        return vendorProfileRepository.save(vendorProfile);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !vendorProfileRepository.existsById(id)) {
            return false;
        }
        vendorProfileRepository.deleteById(id);
        return true;
    }

    @Override
    public List<VendorProfile> getAll() {
        return vendorProfileRepository.findAll();
    }

    @Override
    public Optional<VendorProfile> findByUserId(UUID userId) {
        return vendorProfileRepository.findByUserId(userId);
    }
}