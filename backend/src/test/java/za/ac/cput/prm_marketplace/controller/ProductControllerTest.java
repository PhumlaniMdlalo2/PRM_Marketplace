package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductCondition;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.dto.ProductSearchCriteria;
import za.ac.cput.prm_marketplace.factory.ProductFactory;
import za.ac.cput.prm_marketplace.service.IProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.as;

/**
 * Browsing stays public. Writes are scoped to the seller in the token, so the regression tests here
 * are about what a caller can no longer do: publish under another vendor, edit a competitor's
 * listing, or hard delete a listing that an order line still points at.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IProductService productService;

    private UUID vendorId;
    private UUID otherVendorId;
    private UUID sellerId;
    private UUID intruderId;
    private Product laptop;
    private Product phone;

    @BeforeEach
    void setUp() {
        vendorId = UUID.randomUUID();
        otherVendorId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();

        laptop = ProductFactory.createProduct("Laptop", "15 inch", new BigDecimal("8999.99"),
                "Electronics", 5, vendorId);
        phone = ProductFactory.createProduct("Phone", "128GB", new BigDecimal("4999.00"),
                "Electronics", 10, vendorId);
    }

    // create

    @Test
    @DisplayName("a seller can list a product and the listing is returned")
    void create_returnsCreatedProduct() throws Exception {
        when(productService.create(any(Product.class), eq(sellerId))).thenReturn(laptop);

        mockMvc.perform(post("/api/products")
                        .with(as(sellerId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptop)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.category").value("Electronics"));
    }

    @Test
    @DisplayName("the service is given the caller's id, not anything from the body")
    void create_passesTheCallerToTheService() throws Exception {
        when(productService.create(any(Product.class), eq(sellerId))).thenReturn(laptop);

        mockMvc.perform(post("/api/products")
                        .with(as(sellerId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptop)))
                .andExpect(status().isCreated());

        verify(productService).create(any(Product.class), eq(sellerId));
    }

    @Test
    @DisplayName("a body naming another vendor does not get that vendor into the listing")
    void create_bodyVendorIsStrippedBeforeTheServiceSeesIt() throws Exception {
        when(productService.create(any(Product.class), eq(sellerId))).thenReturn(laptop);

        String hostile = """
                {
                  "name": "Laptop",
                  "price": 8999.99,
                  "stockQuantity": 5,
                  "category": "Electronics",
                  "createdAt": "2000-01-01T00:00:00",
                  "vendor": {"id": "%s", "businessName": "Someone Else", "verified": true}
                }
                """.formatted(otherVendorId);

        mockMvc.perform(post("/api/products")
                        .with(as(sellerId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hostile))
                .andExpect(status().isCreated());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productService).create(captor.capture(), eq(sellerId));

        assertNull(captor.getValue().getVendor(), "vendor must come from the token, not the body");
        assertNull(captor.getValue().getCreatedAt(), "creation time is server-owned");
    }

    @Test
    @DisplayName("images supplied in the body are stripped, so the cascaded collection cannot be written")
    void create_bodyImagesAreStrippedBeforeTheServiceSeesIt() throws Exception {
        when(productService.create(any(Product.class), eq(sellerId))).thenReturn(laptop);

        String hostile = """
                {
                  "name": "Laptop",
                  "price": 8999.99,
                  "stockQuantity": 5,
                  "category": "Electronics",
                  "images": [{"imageUrl": "https://attacker.example.com/x.jpg", "primary": true}]
                }
                """;

        mockMvc.perform(post("/api/products")
                        .with(as(sellerId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hostile))
                .andExpect(status().isCreated());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productService).create(captor.capture(), eq(sellerId));

        assertTrue(captor.getValue().getImages().isEmpty(),
                "a request must not be able to write the cascaded image collection");
    }

    @Test
    @DisplayName("a caller with no vendor profile gets a bad request, not a listing")
    void create_withoutAVendorProfile_returnsBadRequest() throws Exception {
        when(productService.create(any(Product.class), eq(sellerId))).thenReturn(null);

        mockMvc.perform(post("/api/products")
                        .with(as(sellerId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptop)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("an anonymous caller cannot list a product")
    void create_rejectsAnonymous() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptop)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    // read

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
    @DisplayName("a product response no longer carries the lazy image collection")
    void read_doesNotSerialiseTheImageCollection() throws Exception {
        UUID id = UUID.randomUUID();
        Product withImage = ProductFactory.createProduct("Laptop", "15 inch",
                new BigDecimal("8999.99"), "Electronics", 5, vendorId);
        withImage.addImage(new za.ac.cput.prm_marketplace.domain.ProductImage.Builder()
                .setProduct(withImage)
                .setImageUrl("https://cdn.example.com/1.jpg")
                .build());
        when(productService.read(id)).thenReturn(withImage);

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images").doesNotExist());
    }

    // update

    @Test
    @DisplayName("a seller can edit their own listing")
    void update_ownProduct_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.update(eq(id), any(Product.class), eq(sellerId))).thenReturn(laptop);

        mockMvc.perform(put("/api/products/{id}", id)
                        .with(as(sellerId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptop)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    @DisplayName("editing a competitor's listing reads as not found")
    void update_somebodyElsesProduct_returnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.update(eq(id), any(Product.class), eq(intruderId))).thenReturn(null);

        mockMvc.perform(put("/api/products/{id}", id)
                        .with(as(intruderId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptop)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a body id cannot redirect the edit onto a different listing")
    void update_bodyIdIsIgnored() throws Exception {
        UUID id = UUID.randomUUID();
        UUID decoy = UUID.randomUUID();
        when(productService.update(eq(id), any(Product.class), eq(sellerId))).thenReturn(laptop);

        Product hostile = Product.builder()
                .copy(laptop)
                .id(decoy)
                .vendor(new VendorProfile.Builder().setId(otherVendorId).build())
                .build();

        mockMvc.perform(put("/api/products/{id}", id)
                        .with(as(sellerId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hostile)))
                .andExpect(status().isOk());

        verify(productService).update(eq(id), any(Product.class), eq(sellerId));
        verify(productService, never()).update(eq(decoy), any(Product.class), any(UUID.class));
    }

    @Test
    @DisplayName("an anonymous caller cannot edit a listing")
    void update_rejectsAnonymous() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laptop)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    // delete

    @Test
    @DisplayName("there is no delete route: a sold listing cannot be removed by id")
    void deleteIsNotAvailable() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/products/{id}", id).with(as(sellerId, Role.VENDOR)))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(productService);
    }

    // getMine

    @Test
    @DisplayName("the caller's own listings are scoped to the token")
    void getMine_isScopedToTheCaller() throws Exception {
        when(productService.getMine(sellerId)).thenReturn(List.of(laptop, phone));

        mockMvc.perform(get("/api/products/mine").with(as(sellerId, Role.VENDOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("another seller's listings are not returned")
    void getMine_somebodyElseGetsNothing() throws Exception {
        when(productService.getMine(intruderId)).thenReturn(List.of());

        mockMvc.perform(get("/api/products/mine").with(as(intruderId, Role.VENDOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("an anonymous caller cannot list somebody's own listings")
    void getMine_rejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/products/mine"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    // public browsing

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