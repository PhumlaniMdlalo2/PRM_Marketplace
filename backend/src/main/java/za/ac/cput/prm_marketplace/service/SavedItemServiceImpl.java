package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.SavedItem;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.SavedItemRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
public class SavedItemServiceImpl implements ISavedItemService {

    private final SavedItemRepository savedItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public SavedItemServiceImpl(SavedItemRepository savedItemRepository,
                                UserRepository userRepository,
                                ProductRepository productRepository) {
        this.savedItemRepository = savedItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    public SavedItem read(UUID id, UUID requesterId) {
        SavedItem item = find(id);
        return isOwnedBy(item, requesterId) ? item : null;
    }

    @Override
    @Transactional
    public boolean delete(UUID id, UUID requesterId) {
        if (read(id, requesterId) == null) {
            return false;
        }
        savedItemRepository.deleteById(id);
        return true;
    }

    @Override
    public List<SavedItem> getByUser(UUID requesterId) {
        if (requesterId == null) {
            return List.of();
        }
        return savedItemRepository.findByUserIdOrderBySavedAtDesc(requesterId);
    }

    @Override
    @Transactional
    public SavedItem toggle(UUID requesterId, UUID productId) {
        if (requesterId == null || productId == null) {
            return null;
        }

        var existing = savedItemRepository.findByUserIdAndProductId(requesterId, productId);
        if (existing.isPresent()) {
            savedItemRepository.delete(existing.get());
            return null;
        }

        User user = userRepository.findById(requesterId).orElse(null);
        Product product = productRepository.findById(productId).orElse(null);
        if (user == null || product == null) {
            return null;
        }

        return savedItemRepository.save(new SavedItem.Builder()
                .setUser(user)
                .setProduct(product)
                .build());
    }

    @Override
    @Transactional
    public boolean removeByUserAndProduct(UUID requesterId, UUID productId) {
        if (requesterId == null || productId == null) {
            return false;
        }
        return savedItemRepository.findByUserIdAndProductId(requesterId, productId)
                .map(found -> {
                    savedItemRepository.delete(found);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public long countByUser(UUID requesterId) {
        if (requesterId == null) {
            return 0L;
        }
        return savedItemRepository.countByUserId(requesterId);
    }

    private SavedItem find(UUID id) {
        if (id == null) {
            return null;
        }
        return savedItemRepository.findById(id).orElse(null);
    }

    private boolean isOwnedBy(SavedItem item, UUID requesterId) {
        return item != null
                && item.getUser() != null
                && item.getUser().getId() != null
                && item.getUser().getId().equals(requesterId);
    }
}