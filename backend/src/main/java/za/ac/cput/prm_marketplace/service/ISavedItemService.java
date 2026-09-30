package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.SavedItem;

import java.util.List;
import java.util.UUID;

public interface ISavedItemService {

    SavedItem create(SavedItem savedItem);

    SavedItem read(UUID id);

    SavedItem update(SavedItem savedItem);

    boolean delete(UUID id);

    List<SavedItem> getAll();

    List<SavedItem> getByUser(UUID userId);

    SavedItem toggle(UUID userId, UUID productId);

    boolean removeByUserAndProduct(UUID userId, UUID productId);

    long countByUser(UUID userId);
}