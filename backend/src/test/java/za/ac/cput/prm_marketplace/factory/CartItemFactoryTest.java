package za.ac.cput.prm_marketplace.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CartItemFactoryTest {

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = Mockito.mock(User.class);
        product = Mockito.mock(Product.class);
    }

    @Test
    void createCartItem_validInput_returnsCartItemWithAllFields() {
        CartItem item = CartItemFactory.createCartItem(user, product, 3);

        assertNotNull(item);
        assertSame(user, item.getUser());
        assertSame(product, item.getProduct());
        assertEquals(3, item.getQuantity());
    }

    @Test
    void createCartItem_validInput_leavesIdNull() {
        CartItem item = CartItemFactory.createCartItem(user, product, 1);

        assertNull(item.getId());
    }

    @Test
    void createCartItem_setsAddedAtToCurrentTime() {
        LocalDateTime before = LocalDateTime.now();

        CartItem item = CartItemFactory.createCartItem(user, product, 1);

        LocalDateTime after = LocalDateTime.now();
        assertNotNull(item.getAddedAt());
        assertFalse(item.getAddedAt().isBefore(before));
        assertFalse(item.getAddedAt().isAfter(after));
    }

    // Validation

    @Test
    void createCartItem_nullUser_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                CartItemFactory.createCartItem(null, product, 1));

        assertEquals("User is required", ex.getMessage());
    }

    @Test
    void createCartItem_nullProduct_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                CartItemFactory.createCartItem(user, null, 1));

        assertEquals("Product is required", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void createCartItem_quantityBelowOne_throws(int quantity) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                CartItemFactory.createCartItem(user, product, quantity));

        assertEquals("Quantity must be at least 1", ex.getMessage());
    }

    @Test
    void createCartItem_quantityOfOne_isAllowed() {
        CartItem item = CartItemFactory.createCartItem(user, product, 1);

        assertEquals(1, item.getQuantity());
    }
}
