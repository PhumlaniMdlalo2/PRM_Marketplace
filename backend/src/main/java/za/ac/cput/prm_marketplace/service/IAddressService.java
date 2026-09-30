package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Address;

import java.util.List;
import java.util.UUID;

public interface IAddressService {

    Address create(Address address);

    Address read(UUID id);

    Address update(Address address);

    boolean delete(UUID id);

    List<Address> getAll();

    List<Address> getByUser(UUID userId);

    Address getDefaultForUser(UUID userId);

    Address setDefault(UUID id);

    boolean deleteForUser(UUID id, UUID userId);
}