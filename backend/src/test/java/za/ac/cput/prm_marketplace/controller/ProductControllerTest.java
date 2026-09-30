package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductCondition;
import za.ac.cput.prm_marketplace.dto.ProductSearchCriteria;
import za.ac.cput.prm_marketplace.factory.ProductFactory;
import za.ac.cput.prm_marketplace.service.IProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
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

    // search

    private static Page<Product> pageOf(List<Product> content, int number, int size, long total) {
        return new PageImpl<>(content, PageRequest.of(number, size), total);
    }

    @Test
    void search_returnsAPageEnvelope() throws Exception {
        when(productService.search(any(ProductSearchCriteria.class)))
                .thenReturn(pageOf(List.of(laptop, phone), 0, 20, 2));

        mockMvc.perform(get("/api/products/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Laptop"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void search_appliesDefaultCriteria() throws Exception {
        when(productService.search(any(ProductSearchCriteria.class)))
                .thenReturn(pageOf(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/products/search"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductSearchCriteria> captor =
                ArgumentCaptor.forClass(ProductSearchCriteria.class);
        verify(productService).search(captor.capture());

        ProductSearchCriteria criteria = captor.getValue();
        assertNull(criteria.keyword());
        assertEquals(0, criteria.page());
        assertEquals(20, criteria.size());
        assertEquals("createdAt", criteria.sortBy());
        assertEquals("desc", criteria.direction());
    }

    @Test
    void search_excludesInactiveProductsByDefault() throws Exception {
        when(productService.search(any(ProductSearchCriteria.class)))
                .thenReturn(pageOf(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/products/search"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductSearchCriteria> captor =
                ArgumentCaptor.forClass(ProductSearchCriteria.class);
        verify(productService).search(captor.capture());

        assertTrue(captor.getValue().activeOnly());
    }

    @Test
    void search_canIncludeInactiveProducts() throws Exception {
        when(productService.search(any(ProductSearchCriteria.class)))
                .thenReturn(pageOf(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/products/search").param("activeOnly", "false"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductSearchCriteria> captor =
                ArgumentCaptor.forClass(ProductSearchCriteria.class);
        verify(productService).search(captor.capture());

        assertFalse(captor.getValue().activeOnly());
    }

    @Test
    void search_mapsEveryFilterParameter() throws Exception {
        when(productService.search(any(ProductSearchCriteria.class)))
                .thenReturn(pageOf(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/products/search")
                        .param("keyword", "textbook")
                        .param("category", "Books")
                        .param("city", "Cape Town")
                        .param("minPrice", "50")
                        .param("maxPrice", "500")
                        .param("condition", "LIKE_NEW")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sortBy", "price")
                        .param("direction", "asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductSearchCriteria> captor =
                ArgumentCaptor.forClass(ProductSearchCriteria.class);
        verify(productService).search(captor.capture());

        ProductSearchCriteria criteria = captor.getValue();
        assertEquals("textbook", criteria.keyword());
        assertEquals("Books", criteria.category());
        assertEquals("Cape Town", criteria.city());
        assertEquals(new BigDecimal("50"), criteria.minPrice());
        assertEquals(new BigDecimal("500"), criteria.maxPrice());
        assertEquals(ProductCondition.LIKE_NEW, criteria.condition());
        assertEquals(2, criteria.page());
        assertEquals(5, criteria.size());
        assertEquals("price", criteria.sortBy());
        assertEquals("asc", criteria.direction());
    }

    @Test
    void search_rejectsAnUnknownConditionWithBadRequest() throws Exception {
        mockMvc.perform(get("/api/products/search").param("condition", "BROKEN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_rejectsANonNumericPriceWithBadRequest() throws Exception {
        mockMvc.perform(get("/api/products/search").param("minPrice", "cheap"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_rejectsAnUnknownSortFieldWithBadRequest() throws Exception {
        when(productService.search(any(ProductSearchCriteria.class)))
                .thenThrow(new IllegalArgumentException("Unsupported sort field: passwordHash"));

        mockMvc.perform(get("/api/products/search").param("sortBy", "passwordHash"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_rejectsAnOversizedPageWithBadRequest() throws Exception {
        mockMvc.perform(get("/api/products/search").param("size", "5000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_rejectsANegativePageWithBadRequest() throws Exception {
        mockMvc.perform(get("/api/products/search").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_rejectsAnInvertedPriceRangeWithBadRequest() throws Exception {
        mockMvc.perform(get("/api/products/search")
                        .param("minPrice", "500")
                        .param("maxPrice", "50"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_isNotShadowedByTheIdPathVariable() throws Exception {
        UUID someId = UUID.randomUUID();
        when(productService.search(any(ProductSearchCriteria.class)))
                .thenReturn(pageOf(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/products/search"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/products/{id}", someId))
                .andExpect(status().isNotFound());

        verify(productService).search(any(ProductSearchCriteria.class));
    }
}
