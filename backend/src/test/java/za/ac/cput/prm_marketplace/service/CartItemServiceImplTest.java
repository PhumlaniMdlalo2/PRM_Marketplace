package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.CartItemRepository;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartItemServiceImplTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    private CartItemServiceImpl service;

    private UUID ownerId;
    private UUID intruderId;
    private UUID productId;
    private UUID cartItemId;

    @BeforeEach
    void setUp() {
        service = new CartItemServiceImpl(cartItemRepository, userRepository, productRepository);
        ownerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        productId = UUID.randomUUID();
        cartItemId = UUID.randomUUID();
    }

    @Test
    @DisplayName("adding a product the caller does not already have creates a line")
    void addToCart_newProductCreatesLine() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(buildUser(ownerId)));
        when(productRepository.findById(productId)).thenReturn(Optional.of(buildProduct(true)));
        when(cartItemRepository.findByUser_IdAndProduct_Id(ownerId, productId))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        CartItem created = service.addToCart(ownerId, productId, 2);

        assertThat(created).isNotNull();
        assertThat(created.getUser().getId()).isEqualTo(ownerId);
        assertThat(created.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("adding a product already in the cart increases the quantity")
    void addToCart_existingProductIncreasesQuantity() {
        CartItem existing = buildCartItem(ownerId, 3);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(buildUser(ownerId)));
        when(productRepository.findById(productId)).thenReturn(Optional.of(buildProduct(true)));
        when(cartItemRepository.findByUser_IdAndProduct_Id(ownerId, productId))
                .thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        CartItem updated = service.addToCart(ownerId, productId, 2);

        assertThat(updated.getQuantity()).isEqualTo(5);
        assertThat(updated.getId()).isEqualTo(cartItemId);
    }

    @Test
    @DisplayName("a quantity below one is rejected before anything is looked up")
    void addToCart_rejectsZeroQuantity() {
        assertThatThrownBy(() -> service.addToCart(ownerId, productId, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.addToCart(ownerId, productId, -5))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(cartItemRepository);
    }

    @Test
    @DisplayName("an absurd quantity is rejected")
    void addToCart_rejectsOversizedQuantity() {
        assertThatThrownBy(() -> service.addToCart(ownerId, productId, 100_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(cartItemRepository);
    }

    @Test
    @DisplayName("an inactive product cannot be added")
    void addToCart_rejectsInactiveProduct() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(buildUser(ownerId)));
        when(productRepository.findById(productId)).thenReturn(Optional.of(buildProduct(false)));

        assertThatThrownBy(() -> service.addToCart(ownerId, productId, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("an unknown user or product is reported as a bad request")
    void addToCart_rejectsUnknownEntities() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addToCart(ownerId, productId, 1))
                .isInstanceOf(IllegalArgumentException.class);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(buildUser(ownerId)));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addToCart(ownerId, productId, 1))
                .isInstanceOf(IllegalArgumentException.class);

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("adding requires a user and a product")
    void addToCart_rejectsNulls() {
        assertThatThrownBy(() -> service.addToCart(null, productId, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.addToCart(ownerId, null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("read returns the caller's own line")
    void read_ownedLineIsReturned() {
        CartItem item = buildCartItem(ownerId, 1);
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(item));

        assertThat(service.read(cartItemId, ownerId)).isSameAs(item);
    }

    @Test
    @DisplayName("read hides a line in somebody else's cart")
    void read_foreignLineIsHidden() {
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(buildCartItem(intruderId, 1)));

        assertThat(service.read(cartItemId, ownerId)).isNull();
    }

    @Test
    @DisplayName("read returns null for a missing line or a null id")
    void read_missingReturnsNull() {
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.empty());

        assertThat(service.read(cartItemId, ownerId)).isNull();
        assertThat(service.read(null, ownerId)).isNull();
    }

    @Test
    @DisplayName("setting a positive quantity updates the caller's line")
    void updateQuantity_savesUpdatedCopy() {
        when(cartItemRepository.findById(cartItemId))
                .thenReturn(Optional.of(buildCartItem(ownerId, 1)));
        when(cartItemRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        CartItem updated = service.updateQuantity(cartItemId, ownerId, 7);

        assertThat(updated.getQuantity()).isEqualTo(7);
        assertThat(updated.getUser().getId()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("setting the quantity to zero or less removes the line")
    void updateQuantity_nonPositiveRemovesLine() {
        when(cartItemRepository.findById(cartItemId))
                .thenReturn(Optional.of(buildCartItem(ownerId, 4)));

        assertThat(service.updateQuantity(cartItemId, ownerId, 0)).isNull();
        verify(cartItemRepository).deleteById(cartItemId);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("a quantity above the per-line cap is trimmed")
    void updateQuantity_capsOversizedQuantity() {
        when(cartItemRepository.findById(cartItemId))
                .thenReturn(Optional.of(buildCartItem(ownerId, 1)));
        when(cartItemRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        assertThat(service.updateQuantity(cartItemId, ownerId, 5_000).getQuantity()).isEqualTo(99);
    }

    @Test
    @DisplayName("changing the quantity on somebody else's line changes nothing")
    void updateQuantity_foreignLineIsRefused() {
        when(cartItemRepository.findById(cartItemId))
                .thenReturn(Optional.of(buildCartItem(intruderId, 1)));

        assertThat(service.updateQuantity(cartItemId, ownerId, 99)).isNull();
        verify(cartItemRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleting the caller's own line works")
    void delete_ownedLineIsRemoved() {
        when(cartItemRepository.findById(cartItemId))
                .thenReturn(Optional.of(buildCartItem(ownerId, 1)));

        assertThat(service.delete(cartItemId, ownerId)).isTrue();
        verify(cartItemRepository).deleteById(cartItemId);
    }

    @Test
    @DisplayName("deleting somebody else's line is refused")
    void delete_foreignLineIsRefused() {
        when(cartItemRepository.findById(cartItemId))
                .thenReturn(Optional.of(buildCartItem(intruderId, 1)));

        assertThat(service.delete(cartItemId, ownerId)).isFalse();
        verify(cartItemRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("getByUser returns only the caller's cart")
    void getByUser_scopesToTheRequester() {
        CartItem item = buildCartItem(ownerId, 1);
        when(cartItemRepository.findByUser_Id(ownerId)).thenReturn(List.of(item));

        assertThat(service.getByUser(ownerId)).containsExactly(item);
        assertThat(service.getByUser(null)).isEmpty();
    }

    @Test
    @DisplayName("clearing only ever removes the caller's rows")
    void clearCart_scopesToTheRequester() {
        service.clearCart(ownerId);

        verify(cartItemRepository).deleteByUser_Id(ownerId);
    }

    @Test
    @DisplayName("clearing with no caller deletes nothing")
    void clearCart_withNullRequesterDeletesNothing() {
        service.clearCart(null);

        verify(cartItemRepository, never()).deleteByUser_Id(any());
    }

    private User buildUser(UUID id) {
        return new User.Builder().setId(id).setEmail(id + "@example.com").build();
    }

    private Product buildProduct(boolean active) {
        return new Product.Builder()
                .id(productId)
                .name("Widget")
                .price(new BigDecimal("10.00"))
                .stockQuantity(50)
                .active(active)
                .build();
    }

    private CartItem buildCartItem(UUID ownerId, int quantity) {
        return new CartItem.Builder()
                .id(cartItemId)
                .user(buildUser(ownerId))
                .product(buildProduct(true))
                .quantity(quantity)
                .build();
    }
}