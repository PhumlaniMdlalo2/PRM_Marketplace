package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductImage;
import za.ac.cput.prm_marketplace.service.IProductImageService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductImageController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IProductImageService productImageService;

    private UUID id;
    private UUID productId;
    private ProductImage image;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        productId = UUID.randomUUID();
        image = buildImage(true, 0);
    }

    private ProductImage buildImage(boolean primary, int sortOrder) {
        return new ProductImage.Builder()
                .setId(id)
                .setProduct(new Product.Builder()
                        .id(productId)
                        .name("Textbook")
                        .build())
                .setImageUrl("https://cdn.example.com/book.jpg")
                .setPrimary(primary)
                .setSortOrder(sortOrder)
                .build();
    }

    @Test
    @DisplayName("create returns 201 with the image")
    void create_returnsCreated() throws Exception {
        when(productImageService.create(any(ProductImage.class))).thenReturn(image);

        mockMvc.perform(post("/api/product-images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.example.com/book.jpg"));
    }

    @Test
    @DisplayName("create returns 400 when the service refuses")
    void create_returnsBadRequest() throws Exception {
        when(productImageService.create(any(ProductImage.class))).thenReturn(null);

        mockMvc.perform(post("/api/product-images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create accepts the owning product in the request body")
    void create_acceptsNestedProduct() throws Exception {
        when(productImageService.create(any(ProductImage.class))).thenReturn(image);

        String body = "{\"id\":\"" + id + "\",\"imageUrl\":\"https://cdn.example.com/book.jpg\","
                + "\"product\":{\"id\":\"" + productId + "\",\"name\":\"Textbook\"}}";

        mockMvc.perform(post("/api/product-images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        org.mockito.ArgumentCaptor<ProductImage> captor =
                org.mockito.ArgumentCaptor.forClass(ProductImage.class);
        verify(productImageService).create(captor.capture());
        assertThat(captor.getValue().getProduct()).isNotNull();
        assertThat(captor.getValue().getProduct().getId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("the owning product is not echoed back in the response")
    void create_doesNotEchoProduct() throws Exception {
        when(productImageService.create(any(ProductImage.class))).thenReturn(image);

        String body = mockMvc.perform(post("/api/product-images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("\"product\"");
    }

    @Test
    @DisplayName("read returns the image")
    void read_returnsImage() throws Exception {
        when(productImageService.read(id)).thenReturn(image);

        mockMvc.perform(get("/api/product-images/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("read returns 404 for an unknown image")
    void read_returnsNotFound() throws Exception {
        when(productImageService.read(id)).thenReturn(null);

        mockMvc.perform(get("/api/product-images/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update returns 200 on success")
    void update_returnsOk() throws Exception {
        when(productImageService.read(id)).thenReturn(image);
        when(productImageService.update(any(ProductImage.class))).thenReturn(image);

        mockMvc.perform(put("/api/product-images/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("update returns 404 for an unknown image")
    void update_returnsNotFound() throws Exception {
        when(productImageService.read(id)).thenReturn(null);

        mockMvc.perform(put("/api/product-images/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isNotFound());

        verify(productImageService, never()).update(any());
    }

    @Test
    @DisplayName("delete returns 204 on success")
    void delete_returnsNoContent() throws Exception {
        when(productImageService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/api/product-images/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete returns 404 for an unknown image")
    void delete_returnsNotFound() throws Exception {
        when(productImageService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/api/product-images/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getAll returns every image")
    void getAll_returnsList() throws Exception {
        when(productImageService.getAll()).thenReturn(List.of(image));

        mockMvc.perform(get("/api/product-images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("getByProduct returns the product's images in order")
    void getByProduct_returnsList() throws Exception {
        when(productImageService.getByProduct(productId)).thenReturn(List.of(image));

        mockMvc.perform(get("/api/product-images/product/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("getPrimary returns the primary image")
    void getPrimary_returnsImage() throws Exception {
        when(productImageService.getPrimary(productId)).thenReturn(image);

        mockMvc.perform(get("/api/product-images/product/{productId}/primary", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("getPrimary returns 404 when a product has no primary image")
    void getPrimary_returnsNotFound() throws Exception {
        when(productImageService.getPrimary(productId)).thenReturn(null);

        mockMvc.perform(get("/api/product-images/product/{productId}/primary", productId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deleteByProduct clears every image of a product")
    void deleteByProduct_returnsNoContent() throws Exception {
        when(productImageService.deleteByProduct(productId)).thenReturn(true);

        mockMvc.perform(delete("/api/product-images/product/{productId}", productId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("deleteByProduct returns 404 when the service refuses")
    void deleteByProduct_returnsNotFound() throws Exception {
        when(productImageService.deleteByProduct(productId)).thenReturn(false);

        mockMvc.perform(delete("/api/product-images/product/{productId}", productId))
                .andExpect(status().isNotFound());
    }
}
