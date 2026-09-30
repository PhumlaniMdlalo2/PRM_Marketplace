package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
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
    public SavedItem create(SavedItem savedItem) {
        if (savedItem == null) {
            return null;
        }
        return savedItemRepository.save(savedItem);
    }

    @Override
    public SavedItem read(UUID id) {
        if (id == null) {
            return null;
        }
        return savedItemRepository.findById(id).orElse(null);
    }

    @Override
    public SavedItem update(SavedItem savedItem) {
        if (savedItem == null || savedItem.getId() == null
                || !savedItemRepository.existsById(savedItem.getId())) {
            return null;
        }
        return savedItemRepository.save(savedItem);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !savedItemRepository.existsById(id)) {
            return false;
        }
        savedItemRepository.deleteById(id);
        return true;
    }

    @Override
    public List<SavedItem> getAll() {
        return savedItemRepository.findAll();
    }

    @Override
    public List<SavedItem> getByUser(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return savedItemRepository.findByUserIdOrderBySavedAtDesc(userId);
    }

    @Override
    public SavedItem toggle(UUID userId, UUID productId) {
        if (userId == null || productId == null) {
            return null;
        }

        var existing = savedItemRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            savedItemRepository.delete(existing.get());
            return null;
        }

        User user = userRepository.findById(userId).orElse(null);
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
    public boolean removeByUserAndProduct(UUID userId, UUID productId) {
        if (userId == null || productId == null) {
            return false;
        }
        return savedItemRepository.findByUserIdAndProductId(userId, productId)
                .map(found -> {
                    savedItemRepository.delete(found);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public long countByUser(UUID userId) {
        if (userId == null) {
            return 0L;
        }
        return savedItemRepository.countByUserId(userId);
    }
}