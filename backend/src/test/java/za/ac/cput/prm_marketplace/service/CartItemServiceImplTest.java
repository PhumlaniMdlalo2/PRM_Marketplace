package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.CartItemRepository;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartItemServiceImplTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartItemServiceImpl cartItemService;

    private UUID cartItemId;
    private UUID userId;
    private UUID productId;
    private User user;
    private Product product;
    private LocalDateTime addedAt;
    private CartItem existingItem;

    @BeforeEach
    void setUp() {
        cartItemId = UUID.randomUUID();
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        user = mock(User.class);
        product = mock(Product.class);
        addedAt = LocalDateTime.of(2026, 9, 10, 8, 30);

        existingItem = CartItem.builder()
                .id(cartItemId)
                .user(user)
                .product(product)
                .quantity(2)
                .addedAt(addedAt)
                .build();
    }


    @Test
    void create_savesAndReturnsItem() {
        when(cartItemRepository.save(existingItem)).thenReturn(existingItem);

        assertSame(existingItem, cartItemService.create(existingItem));
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    void read_existingId_returnsItem() {
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(existingItem));

        assertSame(existingItem, cartItemService.read(cartItemId));
    }

    @Test
    void read_missingId_returnsNull() {
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.empty());

        assertNull(cartItemService.read(cartItemId));
    }

    @Test
    void update_existingItem_savesAndReturnsIt() {
        when(cartItemRepository.existsById(cartItemId)).thenReturn(true);
        when(cartItemRepository.save(existingItem)).thenReturn(existingItem);

        assertSame(existingItem, cartItemService.update(existingItem));
    }

    @Test
    void update_missingItem_returnsNullAndDoesNotSave() {
        when(cartItemRepository.existsById(cartItemId)).thenReturn(false);

        assertNull(cartItemService.update(existingItem));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void update_itemWithNullId_returnsNullAndDoesNotSave() {
        CartItem unsaved = CartItem.builder().user(user).product(product).quantity(1).build();

        assertNull(cartItemService.update(unsaved));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void delete_existingId_deletesAndReturnsTrue() {
        when(cartItemRepository.existsById(cartItemId)).thenReturn(true);

        assertTrue(cartItemService.delete(cartItemId));
        verify(cartItemRepository).deleteById(cartItemId);
    }

    @Test
    void delete_missingId_returnsFalseAndDoesNotDelete() {
        when(cartItemRepository.existsById(cartItemId)).thenReturn(false);

        assertFalse(cartItemService.delete(cartItemId));
        verify(cartItemRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void getAll_returnsEveryItem() {
        when(cartItemRepository.findAll()).thenReturn(List.of(existingItem));

        assertEquals(1, cartItemService.getAll().size());
    }

    // getByUser

    @Test
    void getByUser_returnsUsersItems() {
        when(cartItemRepository.findByUser_Id(userId)).thenReturn(List.of(existingItem));

        List<CartItem> result = cartItemService.getByUser(userId);

        assertEquals(1, result.size());
        assertSame(existingItem, result.get(0));
    }

    // addToCart

    @Test
    void addToCart_newProduct_createsNewLine() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUser_IdAndProduct_Id(userId, productId)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItem result = cartItemService.addToCart(userId, productId, 3);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        CartItem saved = captor.getValue();

        assertSame(user, saved.getUser());
        assertSame(product, saved.getProduct());
        assertEquals(3, saved.getQuantity());
        assertNull(saved.getId());
        assertNotNull(saved.getAddedAt());
        assertSame(saved, result);
    }

    @Test
    void addToCart_productAlreadyInCart_increasesQuantity() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUser_IdAndProduct_Id(userId, productId)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItem result = cartItemService.addToCart(userId, productId, 3);

        assertEquals(5, result.getQuantity());
        assertEquals(cartItemId, result.getId());
        assertEquals(addedAt, result.getAddedAt());
        assertSame(user, result.getUser());
        assertSame(product, result.getProduct());
        assertEquals(2, existingItem.getQuantity());
    }

    @Test
    void addToCart_quantityBelowOne_throwsBeforeAnyLookup() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                cartItemService.addToCart(userId, productId, 0));

        assertEquals("Quantity must be at least 1", ex.getMessage());
        verifyNoInteractions(userRepository, productRepository, cartItemRepository);
    }

    @Test
    void addToCart_unknownUser_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                cartItemService.addToCart(userId, productId, 1));

        assertEquals("User not found: " + userId, ex.getMessage());
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addToCart_unknownProduct_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                cartItemService.addToCart(userId, productId, 1));

        assertEquals("Product not found: " + productId, ex.getMessage());
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // updateQuantity

    @Test
    void updateQuantity_validQuantity_savesUpdatedCopy() {
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItem result = cartItemService.updateQuantity(cartItemId, 7);

        assertEquals(7, result.getQuantity());
        assertEquals(cartItemId, result.getId());
        assertEquals(addedAt, result.getAddedAt());
        assertEquals(2, existingItem.getQuantity());   // original untouched
    }

    @Test
    void updateQuantity_missingItem_returnsNull() {
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.empty());

        assertNull(cartItemService.updateQuantity(cartItemId, 3));
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verify(cartItemRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void updateQuantity_zeroOrLess_deletesLineAndReturnsNull() {
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(existingItem));

        assertNull(cartItemService.updateQuantity(cartItemId, 0));

        verify(cartItemRepository).deleteById(cartItemId);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // clearCart

    @Test
    void clearCart_deletesAllItemsForUser() {
        cartItemService.clearCart(userId);

        verify(cartItemRepository).deleteByUser_Id(userId);
    }
}
