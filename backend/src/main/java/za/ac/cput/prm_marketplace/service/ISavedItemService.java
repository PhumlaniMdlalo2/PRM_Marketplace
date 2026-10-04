package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.SavedItem;

import java.util.List;
import java.util.UUID;

/**
 * A saved-items list is private to one account. Everything is scoped by the caller's id.
 */
public interface ISavedItemService {

    /** @return the saved item, or null when it does not exist or belongs to someone else */
    SavedItem read(UUID id, UUID requesterId);

    /** @return false when it does not exist or belongs to someone else */
    boolean delete(UUID id, UUID requesterId);

    List<SavedItem> getByUser(UUID requesterId);

    /**
     * Saves the product if it is not already saved and removes it if it is, returning the saved
     * row or null when the caller unsaved it.
     */
    SavedItem toggle(UUID requesterId, UUID productId);

    boolean removeByUserAndProduct(UUID requesterId, UUID productId);

    long countByUser(UUID requesterId);
}