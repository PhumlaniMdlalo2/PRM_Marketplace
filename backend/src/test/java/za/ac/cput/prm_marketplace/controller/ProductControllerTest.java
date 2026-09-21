package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.factory.ProductFactory;
import za.ac.cput.prm_marketplace.service.IProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IProductService productService;

    private Product laptop;
    private Product phone;
    private UUID vendorId;

    @BeforeEach
    void setUp() {
        vendorId = UUID.randomUUID();
        laptop = ProductFactory.createProduct("Laptop", "15 inch", new BigDecimal("8999.99"),
                "Electronics", 5, vendorId);
        phone = ProductFactory.createProduct("Phone", "128GB", new BigDecimal("4999.00"),
                "Electronics", 10, vendorId);
    }

    @Test
    void create_returnsCreatedProduct() throws Exception {
        when(productService.create(any(Product.class))).thenReturn(laptop);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptop)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.category").value("Electronics"));
    }

    @Test
    void read_existingProduct_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.read(id)).thenReturn(laptop);

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void read_missingProduct_returnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.read(id)).thenReturn(null);

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingProduct_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.read(id)).thenReturn(laptop);

        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isNoContent());

        verify(productService).delete(id);
    }

    @Test
    void delete_missingProduct_returnsNotFoundAndDoesNotDelete() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.read(id)).thenReturn(null);

        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isNotFound());

        verify(productService, never()).delete(any(UUID.class));
    }

    @Test
    void getAll_returnsAllProducts() throws Exception {
        when(productService.getAll()).thenReturn(List.of(laptop, phone));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[1].name").value("Phone"));
    }

    @Test
    void getByVendor_returnsVendorsProducts() throws Exception {
        when(productService.getByVendor(vendorId)).thenReturn(List.of(laptop, phone));

        mockMvc.perform(get("/api/products/vendor/{vendorId}", vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByCategory_returnsMatchingProducts() throws Exception {
        when(productService.getByCategory("Electronics")).thenReturn(List.of(laptop, phone));

        mockMvc.perform(get("/api/products/category/{category}", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
