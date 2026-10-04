package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.service.ICartItemService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.asStudent;

@SpringBootTest
@AutoConfigureMockMvc
class CartItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICartItemService cartItemService;

    private UUID callerId;
    private UUID intruderId;
    private UUID productId;
    private UUID cartItemId;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        callerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        productId = UUID.randomUUID();
        cartItemId = UUID.randomUUID();
        cartItem = new CartItem.Builder().id(cartItemId).product(null).quantity(2).build();
    }

    @Test
    @DisplayName("adding to the cart uses the caller's id, not a userId in the request")
    void addToCart_scopesToTheToken() throws Exception {
        when(cartItemService.addToCart(eq(callerId), eq(productId), eq(3))).thenReturn(cartItem);

        mockMvc.perform(post("/api/cart-items")
                        .param("productId", productId.toString())
                        .param("quantity", "3")
                        .with(asStudent(callerId)))
                .andExpect(status().isCreated());

        verify(cartItemService).addToCart(callerId, productId, 3);
    }

    @Test
    @DisplayName("the old /add route that took an arbitrary userId is gone")
    void addToCart_legacyUserIdRouteIsGone() throws Exception {
        // "/api/cart-items/add" is no longer mapped; POST on it is refused outright. Either way the
        // userId in the request never reaches the service.
        mockMvc.perform(post("/api/cart-items/add")
                        .param("userId", intruderId.toString())
                        .param("productId", productId.toString())
                        .param("quantity", "1")
                        .with(asStudent(callerId)))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(cartItemService);
    }

    @Test
    @DisplayName("the cart list is the caller's, and the /user/{userId} route is gone")
    void getAll_isScopedToTheCaller() throws Exception {
        when(cartItemService.getByUser(callerId)).thenReturn(List.of(cartItem));

        mockMvc.perform(get("/api/cart-items").with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(cartItemId.toString()));

        verify(cartItemService).getByUser(callerId);

        mockMvc.perform(get("/api/cart-items/user/" + intruderId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("reading a cart line that is not the caller's returns 404")
    void read_returnsNotFoundForAnotherAccountsLine() throws Exception {
        when(cartItemService.read(cartItemId, callerId)).thenReturn(null);

        mockMvc.perform(get("/api/cart-items/" + cartItemId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("changing a quantity on someone else's cart line has no effect")
    void updateQuantity_returnsNotFoundForAnotherAccountsLine() throws Exception {
        when(cartItemService.updateQuantity(eq(cartItemId), eq(callerId), anyInt())).thenReturn(null);

        mockMvc.perform(put("/api/cart-items/" + cartItemId + "/quantity")
                        .param("quantity", "99")
                        .with(asStudent(callerId)))
                .andExpect(status().isNoContent());

        verify(cartItemService).updateQuantity(cartItemId, callerId, 99);
    }

    @Test
    @DisplayName("changing a quantity on the caller's own line succeeds")
    void updateQuantity_returnsUpdatedLine() throws Exception {
        when(cartItemService.updateQuantity(cartItemId, callerId, 5)).thenReturn(cartItem);

        mockMvc.perform(put("/api/cart-items/" + cartItemId + "/quantity")
                        .param("quantity", "5")
                        .with(asStudent(callerId)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("deleting a cart line that is not the caller's returns 404")
    void delete_returnsNotFoundForAnotherAccountsLine() throws Exception {
        when(cartItemService.delete(cartItemId, callerId)).thenReturn(false);

        mockMvc.perform(delete("/api/cart-items/" + cartItemId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("emptying the cart only ever touches the caller's cart")
    void clearCart_scopesToTheCaller() throws Exception {
        mockMvc.perform(delete("/api/cart-items").with(asStudent(callerId)))
                .andExpect(status().isNoContent());

        verify(cartItemService).clearCart(callerId);

        mockMvc.perform(delete("/api/cart-items/user/" + intruderId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("cart endpoints reject anonymous callers")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/cart-items")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/cart-items")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/cart-items/" + cartItemId)).andExpect(status().isUnauthorized());
        verify(cartItemService, never()).clearCart(any());
    }
}