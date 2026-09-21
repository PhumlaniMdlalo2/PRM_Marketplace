package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.service.ICartItemService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartItemController.class)
class CartItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ICartItemService cartItemService;

    private UUID id;
    private UUID userId;
    private UUID productId;
    private CartItem item;
    private CartItem otherItem;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        item = CartItem.builder().id(id).quantity(2).build();
        otherItem = CartItem.builder().id(UUID.randomUUID()).quantity(1).build();
    }

    // create

    @Test
    void create_returnsCreatedCartItem() throws Exception {
        when(cartItemService.create(any(CartItem.class))).thenReturn(item);

        mockMvc.perform(post("/api/cart-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(2));
    }

    // read

    @Test
    void read_existingItem_returnsOk() throws Exception {
        when(cartItemService.read(id)).thenReturn(item);

        mockMvc.perform(get("/api/cart-items/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void read_missingItem_returnsNotFound() throws Exception {
        when(cartItemService.read(id)).thenReturn(null);

        mockMvc.perform(get("/api/cart-items/{id}", id))
                .andExpect(status().isNotFound());
    }

    // update

    @Test
    void update_existingItem_returnsOk() throws Exception {
        when(cartItemService.read(id)).thenReturn(item);
        when(cartItemService.update(any(CartItem.class))).thenReturn(item);

        mockMvc.perform(put("/api/cart-items/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void update_missingItem_returnsNotFound() throws Exception {
        when(cartItemService.read(id)).thenReturn(null);

        mockMvc.perform(put("/api/cart-items/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isNotFound());

        verify(cartItemService, never()).update(any(CartItem.class));
    }

    // delete

    @Test
    void delete_existingItem_returnsNoContent() throws Exception {
        when(cartItemService.read(id)).thenReturn(item);

        mockMvc.perform(delete("/api/cart-items/{id}", id))
                .andExpect(status().isNoContent());

        verify(cartItemService).delete(id);
    }

    @Test
    void delete_missingItem_returnsNotFoundAndDoesNotDelete() throws Exception {
        when(cartItemService.read(id)).thenReturn(null);

        mockMvc.perform(delete("/api/cart-items/{id}", id))
                .andExpect(status().isNotFound());

        verify(cartItemService, never()).delete(any(UUID.class));
    }


    @Test
    void getByUser_returnsUsersCart() throws Exception {
        when(cartItemService.getByUser(userId)).thenReturn(List.of(item, otherItem));

        mockMvc.perform(get("/api/cart-items/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[1].quantity").value(1));
    }

    @Test
    void getByUser_emptyCart_returnsEmptyArray() throws Exception {
        when(cartItemService.getByUser(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/cart-items/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void addToCart_returnsCreatedItem() throws Exception {
        when(cartItemService.addToCart(userId, productId, 2)).thenReturn(item);

        mockMvc.perform(post("/api/cart-items/add")
                        .param("userId", userId.toString())
                        .param("productId", productId.toString())
                        .param("quantity", "2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void updateQuantity_existingItem_returnsOk() throws Exception {
        when(cartItemService.updateQuantity(id, 2)).thenReturn(item);

        mockMvc.perform(put("/api/cart-items/{id}/quantity", id)
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void updateQuantity_serviceReturnsNull_returnsNoContent() throws Exception {
        when(cartItemService.updateQuantity(id, 0)).thenReturn(null);

        mockMvc.perform(put("/api/cart-items/{id}/quantity", id)
                        .param("quantity", "0"))
                .andExpect(status().isNoContent());
    }

    @Test
    void clearCart_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/cart-items/user/{userId}", userId))
                .andExpect(status().isNoContent());

        verify(cartItemService).clearCart(userId);
    }
}
