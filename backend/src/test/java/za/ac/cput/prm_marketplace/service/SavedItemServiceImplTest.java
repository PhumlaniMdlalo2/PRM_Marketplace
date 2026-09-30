package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.SavedItem;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.SavedItemRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedItemServiceImplTest {

    @Mock
    private SavedItemRepository savedItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private SavedItemServiceImpl savedItemService;

    private User buildUser(UUID id) {
        return new User.Builder()
                .setId(id)
                .setName("Shopper")
                .setEmail("shopper@example.com")
                .setPasswordHash("hash")
                .build();
    }

    private Product buildProduct(UUID id) {
        return new Product.Builder()
                .id(id)
                .name("Textbook")
                .price(new BigDecimal("250.00"))
                .build();
    }

    @Test
    @DisplayName("create persists the saved item")
    void create_saves() {
        SavedItem item = new SavedItem.Builder()
                .setId(UUID.randomUUID())
                .setUser(buildUser(UUID.randomUUID()))
                .setProduct(buildProduct(UUID.randomUUID()))
                .build();
        when(savedItemRepository.save(item)).thenReturn(item);

        assertThat(savedItemService.create(item)).isSameAs(item);
    }

    @Test
    @DisplayName("read missing saved item returns null")
    void read_missing_returnsNull() {
        UUID id = UUID.randomUUID();
        when(savedItemRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(savedItemService.read(id)).isNull();
    }

    @Test
    @DisplayName("update requires an existing saved item")
    void update_missing_returnsNull() {
        SavedItem item = new SavedItem.Builder()
                .setId(UUID.randomUUID())
                .setUser(buildUser(UUID.randomUUID()))
                .setProduct(buildProduct(UUID.randomUUID()))
                .build();
        when(savedItemRepository.existsById(item.getId())).thenReturn(false);

        assertThat(savedItemService.update(item)).isNull();
    }

    @Test
    @DisplayName("delete reports false for an unknown saved item")
    void delete_missing_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(savedItemRepository.existsById(id)).thenReturn(false);

        assertThat(savedItemService.delete(id)).isFalse();
    }

    @Test
    @DisplayName("getByUser with null id returns empty")
    void getByUser_withNull_returnsEmpty() {
        assertThat(savedItemService.getByUser(null)).isEmpty();
    }

    @Test
    @DisplayName("toggle removes an existing save")
    void toggle_existing_removes() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        SavedItem existing = new SavedItem.Builder()
                .setId(UUID.randomUUID())
                .setUser(buildUser(userId))
                .setProduct(buildProduct(productId))
                .build();

        when(savedItemRepository.findByUserIdAndProductId(userId, productId))
                .thenReturn(Optional.of(existing));

        assertThat(savedItemService.toggle(userId, productId)).isNull();
        verify(savedItemRepository).delete(existing);
        verify(savedItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggle creates a save when the product is not saved yet")
    void toggle_new_creates() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        User user = buildUser(userId);
        Product product = buildProduct(productId);

        when(savedItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(savedItemRepository.save(any(SavedItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SavedItem saved = savedItemService.toggle(userId, productId);

        assertThat(saved).isNotNull();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getProduct()).isSameAs(product);
    }

    @Test
    @DisplayName("toggle returns null when the user or product no longer exists")
    void toggle_missingEntities_returnsNull() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(savedItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(productRepository.findById(productId)).thenReturn(Optional.of(buildProduct(productId)));

        assertThat(savedItemService.toggle(userId, productId)).isNull();
        verify(savedItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggle rejects null arguments")
    void toggle_rejectsNulls() {
        assertThat(savedItemService.toggle(null, UUID.randomUUID())).isNull();
        assertThat(savedItemService.toggle(UUID.randomUUID(), null)).isNull();
    }

    @Test
    @DisplayName("removeByUserAndProduct deletes an existing save")
    void removeByUserAndProduct_existing_deletes() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        SavedItem existing = new SavedItem.Builder()
                .setId(UUID.randomUUID())
                .setUser(buildUser(userId))
                .setProduct(buildProduct(productId))
                .build();

        when(savedItemRepository.findByUserIdAndProductId(userId, productId))
                .thenReturn(Optional.of(existing));

        assertThat(savedItemService.removeByUserAndProduct(userId, productId)).isTrue();
        verify(savedItemRepository).delete(existing);
    }

    @Test
    @DisplayName("removeByUserAndProduct is a no-op when nothing is saved")
    void removeByUserAndProduct_missing_returnsFalse() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(savedItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());

        assertThat(savedItemService.removeByUserAndProduct(userId, productId)).isFalse();
        verify(savedItemRepository, never()).delete(any(SavedItem.class));
    }

    @Test
    @DisplayName("removeByUserAndProduct rejects null arguments")
    void removeByUserAndProduct_rejectsNulls() {
        assertThat(savedItemService.removeByUserAndProduct(null, UUID.randomUUID())).isFalse();
        assertThat(savedItemService.removeByUserAndProduct(UUID.randomUUID(), null)).isFalse();
    }

    @Test
    @DisplayName("countByUser with null id is zero")
    void countByUser_withNull_isZero() {
        assertThat(savedItemService.countByUser(null)).isZero();
    }

    @Test
    @DisplayName("getByUser returns the user's saves newest first")
    void getByUser_delegates() {
        UUID userId = UUID.randomUUID();
        List<SavedItem> items = List.of(new SavedItem.Builder()
                .setId(UUID.randomUUID())
                .setUser(buildUser(userId))
                .setProduct(buildProduct(UUID.randomUUID()))
                .build());
        when(savedItemRepository.findByUserIdOrderBySavedAtDesc(userId)).thenReturn(items);

        assertThat(savedItemService.getByUser(userId)).isEqualTo(items);
    }
}
