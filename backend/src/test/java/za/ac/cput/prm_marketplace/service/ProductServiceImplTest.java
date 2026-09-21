package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.factory.ProductFactory;
import za.ac.cput.prm_marketplace.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private UUID vendorId;
    private UUID id;
    private Product laptop;
    private Product savedLaptop;
    private Product phone;

    @BeforeEach
    void setUp() {
        vendorId = UUID.randomUUID();
        id = UUID.randomUUID();

        laptop = ProductFactory.createProduct("Laptop", "15 inch",
                new BigDecimal("9999.99"), "Electronics", 7, vendorId);
        phone = ProductFactory.createProduct("Phone", "128GB",
                new BigDecimal("3999.00"), "Electronics", 10, vendorId);

        savedLaptop = new Product.Builder()
                .id(id)
                .name("Laptop")
                .description("15 inch")
                .price(new BigDecimal("9999.99"))
                .category("Electronics")
                .stockQuantity(7)
                .build();
    }

    // create

    @Test
    void create_savesAndReturnsProduct() {
        when(productRepository.save(laptop)).thenReturn(savedLaptop);

        Product result = productService.create(laptop);

        assertSame(savedLaptop, result);
        verify(productRepository).save(laptop);
    }

    // read

    @Test
    void read_existingId_returnsProduct() {
        when(productRepository.findById(id)).thenReturn(Optional.of(savedLaptop));

        assertSame(savedLaptop, productService.read(id));
    }

    @Test
    void read_missingId_returnsNull() {
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertNull(productService.read(id));
    }

    // update

    @Test
    void update_existingProduct_savesAndReturnsIt() {
        when(productRepository.existsById(id)).thenReturn(true);
        when(productRepository.save(savedLaptop)).thenReturn(savedLaptop);

        Product result = productService.update(savedLaptop);

        assertSame(savedLaptop, result);
        verify(productRepository).save(savedLaptop);
    }

    @Test
    void update_missingProduct_returnsNullAndDoesNotSave() {
        when(productRepository.existsById(id)).thenReturn(false);

        assertNull(productService.update(savedLaptop));

        verify(productRepository, never()).save(any(Product.class));
    }

    // delete

    @Test
    void delete_existingId_deletesAndReturnsTrue() {
        when(productRepository.existsById(id)).thenReturn(true);

        assertTrue(productService.delete(id));

        verify(productRepository).deleteById(id);
    }

    @Test
    void delete_missingId_returnsFalseAndDoesNotDelete() {
        when(productRepository.existsById(id)).thenReturn(false);

        assertFalse(productService.delete(id));

        verify(productRepository, never()).deleteById(any(UUID.class));
    }

    // getAll

    @Test
    void getAll_returnsEveryProduct() {
        when(productRepository.findAll()).thenReturn(List.of(laptop, phone));

        List<Product> result = productService.getAll();

        assertEquals(2, result.size());
        assertTrue(result.contains(laptop));
        assertTrue(result.contains(phone));
    }

    @Test
    void getAll_noProducts_returnsEmptyList() {
        when(productRepository.findAll()).thenReturn(List.of());

        assertTrue(productService.getAll().isEmpty());
    }

    // getByVendor / getByCategory

    @Test
    void getByVendor_returnsVendorsProducts() {
        when(productRepository.findByVendorId(vendorId)).thenReturn(List.of(laptop, phone));

        List<Product> result = productService.getByVendor(vendorId);

        assertEquals(2, result.size());
        verify(productRepository).findByVendorId(vendorId);
    }

    @Test
    void getByCategory_returnsMatchingProducts() {
        when(productRepository.findByCategory("Electronics")).thenReturn(List.of(laptop, phone));

        List<Product> result = productService.getByCategory("Electronics");

        assertEquals(2, result.size());
        verify(productRepository).findByCategory("Electronics");
    }
}
