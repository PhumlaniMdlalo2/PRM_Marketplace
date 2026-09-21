package za.ac.cput.prm_marketplace.factory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import za.ac.cput.prm_marketplace.domain.Product;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductFactoryTest {

    private final UUID vendorId = UUID.randomUUID();

    @Test
    void createProduct_validInput_returnsProductWithAllFields() {
        Product product = ProductFactory.createProduct(
                "Laptop", "15 inch", new BigDecimal("9999.99"), "Electronics", 7, vendorId);

        assertNotNull(product);
        assertEquals("Laptop", product.getName());
        assertEquals("15 inch", product.getDescription());
        assertEquals(new BigDecimal("9999.99"), product.getPrice());
        assertEquals("Electronics", product.getCategory());
        assertEquals(7, product.getStockQuantity());
    }

    @Test
    void createProduct_validInput_leavesIdNull() {
        Product product = ProductFactory.createProduct(
                "Laptop", "15 inch", new BigDecimal("9999.99"), "Electronics", 7, vendorId);

        assertNull(product.getId());
    }

    @Test
    void createProduct_zeroStock_isAllowed() {
        Product product = ProductFactory.createProduct(
                "Laptop", "15 inch", new BigDecimal("9999.99"), "Electronics", 0, vendorId);

        assertEquals(0, product.getStockQuantity());
    }

    @Test
    void createProduct_nullDescription_isAllowed() {
        Product product = ProductFactory.createProduct(
                "Laptop", null, new BigDecimal("9999.99"), "Electronics", 7, vendorId);

        assertNull(product.getDescription());
    }

    // Validation

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void createProduct_blankName_throws(String name) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ProductFactory.createProduct(name, "desc", new BigDecimal("10.00"), "Electronics", 5, vendorId));

        assertEquals("Product name is required", ex.getMessage());
    }

    @Test
    void createProduct_nullPrice_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ProductFactory.createProduct("Laptop", "desc", null, "Electronics", 7, vendorId));

        assertEquals("Price must be greater than zero", ex.getMessage());
    }

    @Test
    void createProduct_zeroPrice_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                ProductFactory.createProduct("Laptop", "desc", BigDecimal.ZERO, "Electronics", 7, vendorId));
    }

    @Test
    void createProduct_negativePrice_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                ProductFactory.createProduct("Laptop", "desc", new BigDecimal("-1.00"), "Electronics", 7, vendorId));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createProduct_blankCategory_throws(String category) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ProductFactory.createProduct("Laptop", "desc", new BigDecimal("10.00"), category, 7, vendorId));

        assertEquals("Category is required", ex.getMessage());
    }

    @Test
    void createProduct_negativeStock_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ProductFactory.createProduct("Laptop", "desc", new BigDecimal("10.00"), "Electronics", -1, vendorId));

        assertEquals("Stock quantity cannot be negative", ex.getMessage());
    }

    @Test
    void createProduct_nullVendorId_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ProductFactory.createProduct("Laptop", "desc", new BigDecimal("10.00"), "Electronics", 7, null));

        assertEquals("Vendor ID is required", ex.getMessage());
    }
}
