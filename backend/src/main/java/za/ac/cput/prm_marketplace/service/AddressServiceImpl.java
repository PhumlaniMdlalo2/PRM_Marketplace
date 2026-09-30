package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.repository.AddressRepository;

import java.util.List;
import java.util.UUID;

@Service
public class AddressServiceImpl implements IAddressService {

    private final AddressRepository addressRepository;

    public AddressServiceImpl(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    public Address create(Address address) {
        if (address == null) {
            return null;
        }
        return addressRepository.save(address);
    }

    @Override
    public Address read(UUID id) {
        if (id == null) {
            return null;
        }
        return addressRepository.findById(id).orElse(null);
    }

    @Override
    public Address update(Address address) {
        if (address == null || address.getId() == null || !addressRepository.existsById(address.getId())) {
            return null;
        }
        return addressRepository.save(address);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !addressRepository.existsById(id)) {
            return false;
        }
        addressRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Address> getAll() {
        return addressRepository.findAll();
    }

    @Override
    public List<Address> getByUser(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return addressRepository.findByUserId(userId);
    }

    @Override
    public Address getDefaultForUser(UUID userId) {
        if (userId == null) {
            return null;
        }
        return addressRepository.findByUserIdAndDefaultAddressTrue(userId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public Address setDefault(UUID id) {
        Address target = read(id);
        if (target == null || target.getUser() == null || target.getUser().getId() == null) {
            return null;
        }

        UUID userId = target.getUser().getId();
        for (Address address : addressRepository.findByUserId(userId)) {
            boolean shouldBeDefault = address.getId().equals(id);
            if (address.isDefaultAddress() != shouldBeDefault) {
                address.setDefaultAddress(shouldBeDefault);
                addressRepository.save(address);
            }
        }
        return target;
    }

    @Override
    public boolean deleteForUser(UUID id, UUID userId) {
        Address address = read(id);
        if (address == null || address.getUser() == null || !address.getUser().getId().equals(userId)) {
            return false;
        }
        addressRepository.deleteById(id);
        return true;
    }
}