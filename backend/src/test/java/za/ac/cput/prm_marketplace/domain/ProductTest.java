package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    private UUID id;
    private VendorProfile vendor;
    private Product product;

    @BeforeEach
    void setUp(){
        id = UUID.randomUUID();
        vendor = new VendorProfile();

        product = new Product.Builder()
                .id(id)
                .name("Laptop")
                .description("15 inch")
                .price(new BigDecimal("9999.99"))
                .category("Electronics")
                .stockQuantity(7)
                .vendor(vendor)
                .build();
    }

    @Test
    void builder_setsAllFields() {
        assertEquals(id, product.getId());
        assertEquals("Laptop", product.getName());
        assertEquals("15 inch", product.getDescription());
        assertEquals(new BigDecimal("9999.99"), product.getPrice());
        assertEquals("Electronics", product.getCategory());
        assertEquals(7, product.getStockQuantity());
        assertEquals(vendor, product.getVendor());
    }

    @Test
    void builder_withoutId_leavesIdNull() {
        Product unsaved = new Product.Builder()
                .name("Phone")
                .price(new BigDecimal("3999.00"))
                .category("Electronics")
                .stockQuantity(10)
                .build();

        assertNull(unsaved.getId());
    }

    @Test
    void copy_duplicatesEveryField() {
        Product copy = new Product.Builder().copy(product).build();

        assertNotSame(product, copy);
        assertEquals(product.getId(), copy.getId());
        assertEquals(product.getName(), copy.getName());
        assertEquals(product.getDescription(), copy.getDescription());
        assertEquals(product.getPrice(), copy.getPrice());
        assertEquals(product.getCategory(), copy.getCategory());
        assertEquals(product.getStockQuantity(), copy.getStockQuantity());
        assertEquals(product.getVendor(), copy.getVendor());
    }

    @Test
    void copy_thenChange_keepsIdAndLeavesOriginalUntouched() {
        Product updated = new Product.Builder()
                .copy(product)
                .price(new BigDecimal("7999.99"))
                .stockQuantity(3)
                .build();

        assertEquals(product.getId(), updated.getId());
        assertEquals(new BigDecimal("7999.99"), updated.getPrice());
        assertEquals(3, updated.getStockQuantity());

        assertEquals(new BigDecimal("9999.99"), product.getPrice());
        assertEquals(7, product.getStockQuantity());
    }

    @Test
    void copy_thenChange_doesNotAffectOtherFields() {
        Product updated = new Product.Builder()
                .copy(product)
                .name("Gaming Laptop")
                .build();

        assertEquals("Gaming Laptop", updated.getName());
        assertEquals(product.getCategory(), updated.getCategory());
        assertEquals(product.getVendor(), updated.getVendor());
    }
}
