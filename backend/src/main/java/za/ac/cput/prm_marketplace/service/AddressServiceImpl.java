package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.AddressRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
public class AddressServiceImpl implements IAddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressServiceImpl(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Address create(Address address, UUID requesterId) {
        if (address == null || requesterId == null) {
            return null;
        }
        User owner = userRepository.findById(requesterId).orElse(null);
        if (owner == null) {
            return null;
        }
        boolean makeDefault = address.isDefaultAddress();
        if (makeDefault) {
            clearExistingDefault(requesterId);
        }
        // The owner comes from the token. A userId in the body is ignored, so an address cannot
        // be filed under someone else's account. The id is cleared so an id in the body cannot
        // turn an insert into an overwrite of an existing row.
        Address owned = new Address.Builder()
                .copy(address)
                .setId(null)
                .setUser(owner)
                .setDefaultAddress(makeDefault)
                .build();
        return addressRepository.save(owned);
    }

    @Override
    public Address read(UUID id, UUID requesterId) {
        Address address = find(id);
        return isOwnedBy(address, requesterId) ? address : null;
    }

    @Override
    @Transactional
    public Address update(Address address, UUID requesterId) {
        if (address == null || address.getId() == null) {
            return null;
        }
        Address existing = read(address.getId(), requesterId);
        if (existing == null) {
            return null;
        }
        // Rebuild from the stored row using only the fields a user is allowed to change, so a
        // body cannot reassign the owner or forge the creation timestamp.
        boolean makeDefault = address.isDefaultAddress();
        if (makeDefault && !existing.isDefaultAddress()) {
            clearExistingDefault(requesterId);
        }
        Address updated = new Address.Builder()
                .copy(existing)
                .setLine1(address.getLine1())
                .setLine2(address.getLine2())
                .setSuburb(address.getSuburb())
                .setCity(address.getCity())
                .setProvince(address.getProvince())
                .setPostalCode(address.getPostalCode())
                .setCountry(address.getCountry())
                .setDefaultAddress(makeDefault)
                .build();
        return addressRepository.save(updated);
    }

    @Override
    @Transactional
    public boolean delete(UUID id, UUID requesterId) {
        if (read(id, requesterId) == null) {
            return false;
        }
        addressRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Address> getByUser(UUID requesterId) {
        if (requesterId == null) {
            return List.of();
        }
        return addressRepository.findByUserId(requesterId);
    }

    @Override
    public Address getDefaultForUser(UUID requesterId) {
        if (requesterId == null) {
            return null;
        }
        return addressRepository.findByUserIdAndDefaultAddressTrue(requesterId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public Address setDefault(UUID id, UUID requesterId) {
        Address target = read(id, requesterId);
        if (target == null) {
            return null;
        }
        if (!target.isDefaultAddress()) {
            clearExistingDefault(requesterId);
            target.setDefaultAddress(true);
            addressRepository.save(target);
        }
        return target;
    }

    private void clearExistingDefault(UUID requesterId) {
        for (Address address : addressRepository.findByUserId(requesterId)) {
            if (address.isDefaultAddress()) {
                address.setDefaultAddress(false);
                addressRepository.save(address);
            }
        }
    }

    private Address find(UUID id) {
        if (id == null) {
            return null;
        }
        return addressRepository.findById(id).orElse(null);
    }

    private boolean isOwnedBy(Address address, UUID requesterId) {
        return address != null
                && address.getUser() != null
                && address.getUser().getId() != null
                && address.getUser().getId().equals(requesterId);
    }
}