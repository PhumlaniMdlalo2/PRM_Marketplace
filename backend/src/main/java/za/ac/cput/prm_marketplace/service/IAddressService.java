package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Address;

import java.util.List;
import java.util.UUID;

/**
 * Addresses belong to exactly one account, so every method is scoped by the caller's id.
 * There is no list-everything operation: an address book is private, and an endpoint that
 * returned every address in the system would leak where people live.
 */
public interface IAddressService {

    /** Creates an address owned by the caller. The owner is taken from the token, not the body. */
    Address create(Address address, UUID requesterId);

    /** @return the address, or null when it does not exist or belongs to someone else */
    Address read(UUID id, UUID requesterId);

    /** @return the updated address, or null when it does not exist or belongs to someone else */
    Address update(Address address, UUID requesterId);

    /** @return false when it does not exist or belongs to someone else */
    boolean delete(UUID id, UUID requesterId);

    List<Address> getByUser(UUID requesterId);

    Address getDefaultForUser(UUID requesterId);

    /** Makes one of the caller's addresses the default, clearing the flag on their other rows. */
    Address setDefault(UUID id, UUID requesterId);
}