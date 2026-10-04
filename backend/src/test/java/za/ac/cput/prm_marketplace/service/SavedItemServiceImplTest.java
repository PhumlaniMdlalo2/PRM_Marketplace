package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.SavedItem;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.SavedItemRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

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

    private SavedItemServiceImpl service;

    private UUID ownerId;
    private UUID intruderId;
    private UUID productId;
    private UUID savedItemId;

    @BeforeEach
    void setUp() {
        service = new SavedItemServiceImpl(savedItemRepository, userRepository, productRepository);
        ownerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        productId = UUID.randomUUID();
        savedItemId = UUID.randomUUID();
    }

    @Test
    @DisplayName("toggle saves the product when it is not already saved")
    void toggle_newItemCreatesRow() {
        when(savedItemRepository.findByUserIdAndProductId(ownerId, productId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(buildUser(ownerId)));
        when(productRepository.findById(productId)).thenReturn(Optional.of(buildProduct()));
        when(savedItemRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        SavedItem result = service.toggle(ownerId, productId);

        assertThat(result).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("toggle removes the product when it is already saved")
    void toggle_existingItemRemovesRow() {
        when(savedItemRepository.findByUserIdAndProductId(ownerId, productId))
                .thenReturn(Optional.of(buildSavedItem(ownerId)));

        assertThat(service.toggle(ownerId, productId)).isNull();
        verify(savedItemRepository).delete(any(SavedItem.class));
        verify(savedItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggle returns null when the caller or product does not exist")
    void toggle_missingEntitiesReturnNull() {
        when(savedItemRepository.findByUserIdAndProductId(ownerId, productId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());
        assertThat(service.toggle(ownerId, productId)).isNull();

        when(savedItemRepository.findByUserIdAndProductId(ownerId, productId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(buildUser(ownerId)));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        assertThat(service.toggle(ownerId, productId)).isNull();

        verify(savedItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggle rejects nulls")
    void toggle_rejectsNulls() {
        assertThat(service.toggle(null, productId)).isNull();
        assertThat(service.toggle(ownerId, null)).isNull();
    }

    @Test
    @DisplayName("read returns the caller's own saved item")
    void read_ownedItemIsReturned() {
        SavedItem item = buildSavedItem(ownerId);
        when(savedItemRepository.findById(savedItemId)).thenReturn(Optional.of(item));

        assertThat(service.read(savedItemId, ownerId)).isSameAs(item);
    }

    @Test
    @DisplayName("read hides a saved item on somebody else's list")
    void read_foreignItemIsHidden() {
        when(savedItemRepository.findById(savedItemId))
                .thenReturn(Optional.of(buildSavedItem(intruderId)));

        assertThat(service.read(savedItemId, ownerId)).isNull();
    }

    @Test
    @DisplayName("read returns null for a missing item or a null id")
    void read_missingReturnsNull() {
        when(savedItemRepository.findById(savedItemId)).thenReturn(Optional.empty());

        assertThat(service.read(savedItemId, ownerId)).isNull();
        assertThat(service.read(null, ownerId)).isNull();
    }

    @Test
    @DisplayName("delete removes the caller's own saved item")
    void delete_ownedItemIsRemoved() {
        when(savedItemRepository.findById(savedItemId))
                .thenReturn(Optional.of(buildSavedItem(ownerId)));

        assertThat(service.delete(savedItemId, ownerId)).isTrue();
        verify(savedItemRepository).deleteById(savedItemId);
    }

    @Test
    @DisplayName("delete refuses to remove somebody else's saved item")
    void delete_foreignItemIsRefused() {
        when(savedItemRepository.findById(savedItemId))
                .thenReturn(Optional.of(buildSavedItem(intruderId)));

        assertThat(service.delete(savedItemId, ownerId)).isFalse();
        verify(savedItemRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("the saved list is the caller's only")
    void getByUser_scopesToTheRequester() {
        SavedItem item = buildSavedItem(ownerId);
        when(savedItemRepository.findByUserIdOrderBySavedAtDesc(ownerId))
                .thenReturn(List.of(item));

        assertThat(service.getByUser(ownerId)).containsExactly(item);
        assertThat(service.getByUser(null)).isEmpty();
    }

    @Test
    @DisplayName("removing a saved product only touches the caller's list")
    void removeByUserAndProduct_scopesToTheRequester() {
        when(savedItemRepository.findByUserIdAndProductId(ownerId, productId))
                .thenReturn(Optional.of(buildSavedItem(ownerId)));

        assertThat(service.removeByUserAndProduct(ownerId, productId)).isTrue();
        verify(savedItemRepository).delete(any(SavedItem.class));
    }

    @Test
    @DisplayName("removing a product that is not saved reports false")
    void removeByUserAndProduct_missingReturnsFalse() {
        when(savedItemRepository.findByUserIdAndProductId(ownerId, productId))
                .thenReturn(Optional.empty());

        assertThat(service.removeByUserAndProduct(ownerId, productId)).isFalse();
        assertThat(service.removeByUserAndProduct(null, productId)).isFalse();
        assertThat(service.removeByUserAndProduct(ownerId, null)).isFalse();
    }

    @Test
    @DisplayName("the saved count is the caller's only")
    void countByUser_scopesToTheRequester() {
        when(savedItemRepository.countByUserId(ownerId)).thenReturn(3L);

        assertThat(service.countByUser(ownerId)).isEqualTo(3L);
        assertThat(service.countByUser(null)).isZero();
    }

    private User buildUser(UUID id) {
        return new User.Builder().setId(id).setEmail(id + "@example.com").build();
    }

    private Product buildProduct() {
        return new Product.Builder().id(productId).name("Widget").build();
    }

    private SavedItem buildSavedItem(UUID ownerId) {
        return new SavedItem.Builder()
                .setId(savedItemId)
                .setUser(buildUser(ownerId))
                .setProduct(buildProduct())
                .build();
    }
}