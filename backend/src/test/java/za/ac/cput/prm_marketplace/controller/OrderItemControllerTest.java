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
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.service.IOrderItemService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.asStudent;

/**
 * Line items are built by checkout and the order total is calculated from them, so this controller
 * offers reads only. The write tests here are regression tests: they pin down that a client can no
 * longer set its own price, retarget a line at another account's order, or strip a line off a live
 * order.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IOrderItemService orderItemService;

    private UUID buyerId;
    private UUID intruderId;
    private UUID itemId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        buyerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("a line item on the caller's order is readable")
    void read_ownItem_returnsOk() throws Exception {
        when(orderItemService.read(itemId, buyerId)).thenReturn(sampleItem());

        mockMvc.perform(get("/api/order-items/{id}", itemId).with(asStudent(buyerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    @DisplayName("another account's line item reads as not found")
    void read_somebodyElsesItemIsNotFound() throws Exception {
        when(orderItemService.read(itemId, intruderId)).thenReturn(null);

        mockMvc.perform(get("/api/order-items/{id}", itemId).with(asStudent(intruderId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a missing line item is not found")
    void read_missingItem_returnsNotFound() throws Exception {
        when(orderItemService.read(itemId, buyerId)).thenReturn(null);

        mockMvc.perform(get("/api/order-items/{id}", itemId).with(asStudent(buyerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the lines for an order are scoped to the caller")
    void getByOrderId_isScopedToTheCaller() throws Exception {
        when(orderItemService.getByOrderId(orderId, buyerId)).thenReturn(List.of(sampleItem()));

        mockMvc.perform(get("/api/order-items/order/{orderId}", orderId).with(asStudent(buyerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("another account's order yields no lines")
    void getByOrderId_somebodyElsesOrderIsEmpty() throws Exception {
        when(orderItemService.getByOrderId(orderId, intruderId)).thenReturn(List.of());

        mockMvc.perform(get("/api/order-items/order/{orderId}", orderId).with(asStudent(intruderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("an anonymous caller cannot read line items")
    void read_rejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/order-items/{id}", itemId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderItemService);
    }

    @Test
    @DisplayName("a client cannot create a line item with its own price")
    void createIsNotAvailable() throws Exception {
        String hostile = """
                {"quantity": 99, "priceAtPurchase": 0.01}
                """;

        // 404: there is no handler for this path at all, which is stronger than a refusal.
        mockMvc.perform(post("/api/order-items")
                        .with(asStudent(buyerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hostile))
                .andExpect(status().isNotFound());

        verifyNoInteractions(orderItemService);
    }

    @Test
    @DisplayName("a client cannot rewrite a line's price or order")
    void updateIsNotAvailable() throws Exception {
        String hostile = """
                {"id": "%s", "quantity": 99, "priceAtPurchase": 0.01}
                """.formatted(itemId);

        mockMvc.perform(put("/api/order-items")
                        .with(asStudent(buyerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hostile))
                .andExpect(status().isNotFound());

        verifyNoInteractions(orderItemService);
    }

    @Test
    @DisplayName("a client cannot delete a line from a live order")
    void deleteIsNotAvailable() throws Exception {
        // 405 here rather than 404: /{id} is a real route for GET, there is just no DELETE on it.
        mockMvc.perform(delete("/api/order-items/{id}", itemId).with(asStudent(buyerId)))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(orderItemService);
    }

    @Test
    @DisplayName("the bare GET no longer returns every customer's purchases")
    void getAllIsNotAvailable() throws Exception {
        mockMvc.perform(get("/api/order-items").with(asStudent(buyerId)))
                .andExpect(status().isNotFound());

        verifyNoInteractions(orderItemService);
    }

    private OrderItem sampleItem() {
        return new OrderItem.Builder()
                .setId(itemId)
                .setQuantity(2)
                .setPriceAtPurchase(new BigDecimal("125.50"))
                .build();
    }
}