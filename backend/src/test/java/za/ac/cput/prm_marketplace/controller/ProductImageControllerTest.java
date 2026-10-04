package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductImage;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.service.IProductImageService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.as;

/**
 * The product is now named by the path rather than the body, and every write carries the caller.
 * The regression tests here pin down what a client can no longer do: attach an image to a
 * competitor's product, move an existing image onto another listing, or wipe another seller's
 * gallery.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IProductImageService productImageService;

    private UUID id;
    private UUID productId;
    private UUID sellerId;
    private UUID intruderId;
    private ProductImage image;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        productId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
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

    // create

    @Test
    @DisplayName("a seller can attach an image to their own product")
    void create_returnsCreated() throws Exception {
        when(productImageService.create(eq(productId), any(ProductImage.class), eq(sellerId)))
                .thenReturn(image);

        mockMvc.perform(post("/api/product-images/product/{productId}", productId)
                        .with(as(sellerId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.example.com/book.jpg"));
    }

    @Test
    @DisplayName("attaching to a product the caller does not own reads as not found")
    void create_somebodyElsesProduct_returnsNotFound() throws Exception {
        when(productImageService.create(eq(productId), any(ProductImage.class), eq(intruderId)))
                .thenReturn(null);

        mockMvc.perform(post("/api/product-images/product/{productId}", productId)
                        .with(as(intruderId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a product named in the body does not override the one in the path")
    void create_bodyProductCannotRedirectTheImage() throws Exception {
        when(productImageService.create(eq(productId), any(ProductImage.class), eq(sellerId)))
                .thenReturn(image);

        UUID decoyProductId = UUID.randomUUID();
        String body = """
                {"imageUrl": "https://cdn.example.com/book.jpg",
                 "product": {"id": "%s", "name": "Competitor"}}
                """.formatted(decoyProductId);

        mockMvc.perform(post("/api/product-images/product/{productId}", productId)
                        .with(as(sellerId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // The service is told which product to use, so the body's association is never trusted.
        verify(productImageService).create(eq(productId), any(ProductImage.class), eq(sellerId));
    }

    @Test
    @DisplayName("the owning product is not echoed back in the response")
    void create_doesNotEchoProduct() throws Exception {
        when(productImageService.create(eq(productId), any(ProductImage.class), eq(sellerId)))
                .thenReturn(image);

        String body = mockMvc.perform(post("/api/product-images/product/{productId}", productId)
                        .with(as(sellerId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("\"product\"");
    }

    @Test
    @DisplayName("a client cannot backdate an image")
    void create_createdAtIsStrippedFromTheBody() throws Exception {
        when(productImageService.create(eq(productId), any(ProductImage.class), eq(sellerId)))
                .thenReturn(image);

        String hostile = """
                {"imageUrl": "https://cdn.example.com/book.jpg", "createdAt": "2000-01-01T00:00:00"}
                """;

        mockMvc.perform(post("/api/product-images/product/{productId}", productId)
                        .with(as(sellerId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hostile))
                .andExpect(status().isCreated());

        org.mockito.ArgumentCaptor<ProductImage> captor =
                org.mockito.ArgumentCaptor.forClass(ProductImage.class);
        verify(productImageService).create(eq(productId), captor.capture(), eq(sellerId));

        assertThat(captor.getValue().getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("the old body-only create route no longer exists")
    void createWithoutAProductRouteIsNotAvailable() throws Exception {
        mockMvc.perform(post("/api/product-images")
                        .with(as(sellerId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isNotFound());

        verifyNoInteractions(productImageService);
    }

    @Test
    @DisplayName("an anonymous caller cannot attach an image")
    void create_rejectsAnonymous() throws Exception {
        mockMvc.perform(post("/api/product-images/product/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productImageService);
    }

    // read

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

    // update

    @Test
    @DisplayName("a seller can edit an image on their own product")
    void update_returnsOk() throws Exception {
        when(productImageService.update(eq(id), any(ProductImage.class), eq(sellerId)))
                .thenReturn(image);

        mockMvc.perform(put("/api/product-images/{id}", id)
                        .with(as(sellerId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("editing an image on somebody else's product reads as not found")
    void update_somebodyElsesImage_returnsNotFound() throws Exception {
        when(productImageService.update(eq(id), any(ProductImage.class), eq(intruderId)))
                .thenReturn(null);

        mockMvc.perform(put("/api/product-images/{id}", id)
                        .with(as(intruderId, Role.VENDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("an anonymous caller cannot edit an image")
    void update_rejectsAnonymous() throws Exception {
        mockMvc.perform(put("/api/product-images/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productImageService);
    }

    // delete

    @Test
    @DisplayName("a seller can remove an image from their own product")
    void delete_returnsNoContent() throws Exception {
        when(productImageService.delete(id, sellerId)).thenReturn(true);

        mockMvc.perform(delete("/api/product-images/{id}", id).with(as(sellerId, Role.VENDOR)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("removing an image on somebody else's product reads as not found")
    void delete_somebodyElsesImage_returnsNotFound() throws Exception {
        when(productImageService.delete(id, intruderId)).thenReturn(false);

        mockMvc.perform(delete("/api/product-images/{id}", id).with(as(intruderId, Role.VENDOR)))
                .andExpect(status().isNotFound());

        // The caller's own id is what reaches the service, so the refusal is the service's.
        verify(productImageService).delete(id, intruderId);
    }

    @Test
    @DisplayName("an anonymous caller cannot remove an image")
    void delete_rejectsAnonymous() throws Exception {
        mockMvc.perform(delete("/api/product-images/{id}", id))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productImageService);
    }

    // getAll

    @Test
    @DisplayName("the bare GET no longer returns every seller's photography")
    void getAllIsNotAvailable() throws Exception {
        mockMvc.perform(get("/api/product-images"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(productImageService);
    }

    // getByProduct / getPrimary

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

    // deleteByProduct

    @Test
    @DisplayName("a seller can clear the gallery on their own product")
    void deleteByProduct_returnsNoContent() throws Exception {
        when(productImageService.deleteByProduct(productId, sellerId)).thenReturn(true);

        mockMvc.perform(delete("/api/product-images/product/{productId}", productId)
                        .with(as(sellerId, Role.VENDOR)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("clearing somebody else's gallery reads as not found")
    void deleteByProduct_somebodyElsesProduct_returnsNotFound() throws Exception {
        when(productImageService.deleteByProduct(productId, intruderId)).thenReturn(false);

        mockMvc.perform(delete("/api/product-images/product/{productId}", productId)
                        .with(as(intruderId, Role.VENDOR)))
                .andExpect(status().isNotFound());

        verify(productImageService).deleteByProduct(productId, intruderId);
    }

    @Test
    @DisplayName("an anonymous caller cannot clear a gallery")
    void deleteByProduct_rejectsAnonymous() throws Exception {
        mockMvc.perform(delete("/api/product-images/product/{productId}", productId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productImageService);
    }
}