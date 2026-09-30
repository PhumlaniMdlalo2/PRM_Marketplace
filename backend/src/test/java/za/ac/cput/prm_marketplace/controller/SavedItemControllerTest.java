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
import za.ac.cput.prm_marketplace.domain.SavedItem;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.ISavedItemService;

import java.util.List;
import java.util.UUID;

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

@WebMvcTest(SavedItemController.class)
@AutoConfigureMockMvc(addFilters = false)
class SavedItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ISavedItemService savedItemService;

    private UUID id;
    private UUID userId;
    private UUID productId;
    private SavedItem savedItem;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        savedItem = buildSavedItem();
    }

    private SavedItem buildSavedItem() {
        return new SavedItem.Builder()
                .setId(id)
                .setUser(new User.Builder()
                        .setId(userId)
                        .setName("Shopper")
                        .setEmail("shopper@example.com")
                        .setPasswordHash("hash")
                        .build())
                .setProduct(new Product.Builder()
                        .id(productId)
                        .name("Textbook")
                        .build())
                .build();
    }

    @Test
    @DisplayName("create returns 201 with the saved item")
    void create_returnsCreated() throws Exception {
        when(savedItemService.create(any(SavedItem.class))).thenReturn(savedItem);

        mockMvc.perform(post("/api/saved-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(savedItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("create returns 400 when the service refuses")
    void create_returnsBadRequest() throws Exception {
        when(savedItemService.create(any(SavedItem.class))).thenReturn(null);

        mockMvc.perform(post("/api/saved-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(savedItem)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("read returns the saved item")
    void read_returnsSavedItem() throws Exception {
        when(savedItemService.read(id)).thenReturn(savedItem);

        mockMvc.perform(get("/api/saved-items/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("read returns 404 for an unknown saved item")
    void read_returnsNotFound() throws Exception {
        when(savedItemService.read(id)).thenReturn(null);

        mockMvc.perform(get("/api/saved-items/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update returns 200 on success")
    void update_returnsOk() throws Exception {
        when(savedItemService.read(id)).thenReturn(savedItem);
        when(savedItemService.update(any(SavedItem.class))).thenReturn(savedItem);

        mockMvc.perform(put("/api/saved-items/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(savedItem)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("update returns 404 for an unknown saved item")
    void update_returnsNotFound() throws Exception {
        when(savedItemService.read(id)).thenReturn(null);

        mockMvc.perform(put("/api/saved-items/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(savedItem)))
                .andExpect(status().isNotFound());

        verify(savedItemService, never()).update(any());
    }

    @Test
    @DisplayName("delete returns 204 on success")
    void delete_returnsNoContent() throws Exception {
        when(savedItemService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/api/saved-items/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete returns 404 for an unknown saved item")
    void delete_returnsNotFound() throws Exception {
        when(savedItemService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/api/saved-items/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getAll returns every saved item")
    void getAll_returnsList() throws Exception {
        when(savedItemService.getAll()).thenReturn(List.of(savedItem));

        mockMvc.perform(get("/api/saved-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("getByUser returns the user's saved items")
    void getByUser_returnsList() throws Exception {
        when(savedItemService.getByUser(userId)).thenReturn(List.of(savedItem));

        mockMvc.perform(get("/api/saved-items/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("countByUser returns the saved total")
    void countByUser_returnsNumber() throws Exception {
        when(savedItemService.countByUser(userId)).thenReturn(5L);

        mockMvc.perform(get("/api/saved-items/user/{userId}/count", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5));
    }

    @Test
    @DisplayName("toggle saves the product and returns the saved item")
    void toggle_saves() throws Exception {
        when(savedItemService.toggle(userId, productId)).thenReturn(savedItem);

        mockMvc.perform(post("/api/saved-items/user/{userId}/product/{productId}/toggle",
                        userId, productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("toggle reports 204 when the save is removed")
    void toggle_removes() throws Exception {
        when(savedItemService.toggle(userId, productId)).thenReturn(null);

        mockMvc.perform(post("/api/saved-items/user/{userId}/product/{productId}/toggle",
                        userId, productId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("removeByUserAndProduct returns 204 on success")
    void removeByUserAndProduct_returnsNoContent() throws Exception {
        when(savedItemService.removeByUserAndProduct(userId, productId)).thenReturn(true);

        mockMvc.perform(delete("/api/saved-items/user/{userId}/product/{productId}", userId, productId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("removeByUserAndProduct returns 404 when nothing was saved")
    void removeByUserAndProduct_returnsNotFound() throws Exception {
        when(savedItemService.removeByUserAndProduct(userId, productId)).thenReturn(false);

        mockMvc.perform(delete("/api/saved-items/user/{userId}/product/{productId}", userId, productId))
                .andExpect(status().isNotFound());
    }
}
