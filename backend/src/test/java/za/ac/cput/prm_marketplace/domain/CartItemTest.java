package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CartItemTest {

    private UUID id;
    private User user;
    private Product product;
    private LocalDateTime addedAt;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        user = Mockito.mock(User.class);
        product = Mockito.mock(Product.class);
        addedAt = LocalDateTime.of(2026, 9, 10, 8, 30);

        cartItem = CartItem.builder()
                .id(id)
                .user(user)
                .product(product)
                .quantity(2)
                .addedAt(addedAt)
                .build();
    }

    @Test
    void builder_setsAllFields() {
        assertEquals(id, cartItem.getId());
        assertSame(user, cartItem.getUser());
        assertSame(product, cartItem.getProduct());
        assertEquals(2, cartItem.getQuantity());
        assertEquals(addedAt, cartItem.getAddedAt());
    }

    @Test
    void builder_withoutId_leavesIdNull() {
        CartItem unsaved = CartItem.builder()
                .user(user)
                .product(product)
                .quantity(1)
                .addedAt(addedAt)
                .build();

        assertNull(unsaved.getId());
    }

    @Test
    void copy_duplicatesEveryField() {
        CartItem copy = CartItem.builder().copy(cartItem).build();

        assertNotSame(cartItem, copy);
        assertEquals(cartItem.getId(), copy.getId());
        assertSame(cartItem.getUser(), copy.getUser());
        assertSame(cartItem.getProduct(), copy.getProduct());
        assertEquals(cartItem.getQuantity(), copy.getQuantity());
        assertEquals(cartItem.getAddedAt(), copy.getAddedAt());
    }

    @Test
    void copy_thenChangeQuantity_keepsIdAndAddedAt() {
        CartItem updated = CartItem.builder()
                .copy(cartItem)
                .quantity(5)
                .build();

        assertEquals(5, updated.getQuantity());
        assertEquals(cartItem.getId(), updated.getId());
        assertEquals(cartItem.getAddedAt(), updated.getAddedAt());
        assertSame(cartItem.getUser(), updated.getUser());
        assertSame(cartItem.getProduct(), updated.getProduct());
    }

    @Test
    void copy_thenChange_leavesOriginalUntouched() {
        CartItem.builder().copy(cartItem).quantity(9).build();

        assertEquals(2, cartItem.getQuantity());
    }
}
