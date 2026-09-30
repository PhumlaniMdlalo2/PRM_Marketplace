package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderStatus;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.IOrderService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IOrderService orderService;

    private UUID id;
    private UUID buyerId;
    private Order order;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        order = buildOrder(OrderStatus.PENDING);
    }

    private Order buildOrder(OrderStatus orderStatus) {
        return new Order.Builder()
                .setId(id)
                .setBuyer(new User.Builder()
                        .setId(buyerId)
                        .setName("Buyer")
                        .setEmail("buyer@example.com")
                        .setPasswordHash("hash")
                        .build())
                .setStatus(orderStatus)
                .setTotalAmount(new BigDecimal("250.00"))
                .build();
    }

    @Test
    @DisplayName("create returns 201 with the order")
    void create_returnsCreated() throws Exception {
        when(orderService.create(any(Order.class))).thenReturn(order);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("create returns 400 when the service refuses")
    void create_returnsBadRequest() throws Exception {
        when(orderService.create(any(Order.class))).thenReturn(null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("read returns the order")
    void read_returnsOrder() throws Exception {
        when(orderService.read(id)).thenReturn(order);

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("read returns 404 for an unknown order")
    void read_returnsNotFound() throws Exception {
        when(orderService.read(id)).thenReturn(null);

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update forces the path id onto the entity")
    void update_usesPathId() throws Exception {
        when(orderService.read(id)).thenReturn(order);
        when(orderService.update(any(Order.class))).thenReturn(order);

        mockMvc.perform(put("/api/orders/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).update(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("update returns 404 when the order is unknown")
    void update_returnsNotFound() throws Exception {
        when(orderService.read(id)).thenReturn(null);

        mockMvc.perform(put("/api/orders/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isNotFound());

        verify(orderService, never()).update(any());
    }

    @Test
    @DisplayName("delete returns 204 on success")
    void delete_returnsNoContent() throws Exception {
        when(orderService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/api/orders/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete returns 404 for an unknown order")
    void delete_returnsNotFound() throws Exception {
        when(orderService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/api/orders/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getAll returns every order")
    void getAll_returnsList() throws Exception {
        when(orderService.getAll()).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("getByBuyer returns the buyer's orders")
    void getByBuyer_returnsList() throws Exception {
        when(orderService.getByBuyer(buyerId)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/buyer/{buyerId}", buyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("getByBuyerAndStatus filters by status")
    void getByBuyerAndStatus_filters() throws Exception {
        when(orderService.getByBuyerAndStatus(buyerId, OrderStatus.SHIPPED)).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/buyer/{buyerId}/status/{status}", buyerId, "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(orderService).getByBuyerAndStatus(buyerId, OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("getByBuyerAndStatus rejects an unknown status with 400")
    void getByBuyerAndStatus_withInvalidStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/orders/buyer/{buyerId}/status/{status}", buyerId, "NOT_A_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("updateStatus applies the requested status")
    void updateStatus_appliesStatus() throws Exception {
        Order shipped = buildOrder(OrderStatus.SHIPPED);
        when(orderService.updateStatus(id, OrderStatus.SHIPPED)).thenReturn(shipped);

        mockMvc.perform(patch("/api/orders/{id}/status", id)
                        .param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        verify(orderService).updateStatus(id, OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("updateStatus returns 404 for an unknown order")
    void updateStatus_returnsNotFound() throws Exception {
        when(orderService.updateStatus(id, OrderStatus.SHIPPED)).thenReturn(null);

        mockMvc.perform(patch("/api/orders/{id}/status", id)
                        .param("status", "SHIPPED"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("updateStatus requires the status parameter")
    void updateStatus_withoutStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/orders/{id}/status", id))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cancel returns 204 when the order is cancellable")
    void cancel_returnsNoContent() throws Exception {
        when(orderService.cancel(id, buyerId)).thenReturn(true);

        mockMvc.perform(patch("/api/orders/{id}/cancel", id)
                        .param("buyerId", buyerId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("cancel returns 400 when the order cannot be cancelled")
    void cancel_returnsBadRequest() throws Exception {
        when(orderService.cancel(id, buyerId)).thenReturn(false);

        mockMvc.perform(patch("/api/orders/{id}/cancel", id)
                        .param("buyerId", buyerId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cancel requires the buyerId parameter")
    void cancel_withoutBuyerId_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/orders/{id}/cancel", id))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("an order never leaks the buyer's password hash")
    void order_doesNotLeakPasswordHash() throws Exception {
        when(orderService.read(id)).thenReturn(order);

        String body = mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("passwordHash").doesNotContain("\"hash\"");
    }
}
