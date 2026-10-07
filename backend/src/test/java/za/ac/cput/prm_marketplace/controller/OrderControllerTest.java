package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderStatus;
import za.ac.cput.prm_marketplace.domain.FulfillmentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.security.UserPrincipal;
import za.ac.cput.prm_marketplace.service.IOrderService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The controller must take the acting user from the validated token and never from the request.
 * Several of these tests exist purely to prove that: an order id in the path is a lookup key, not
 * a permission, and a buyer id in a query string is not proof of ownership.
 */
@SpringBootTest
@AutoConfigureMockMvc
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

    /** A second account, used to prove one caller cannot reach the other's order. */
    private UUID intruderId;

    /** Identifies the caller for the request currently being built. */
    private UUID callerId;

    private Role callerRole = Role.STUDENT;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        callerId = buyerId;
        callerRole = Role.STUDENT;
        order = buildOrder(OrderStatus.PENDING);
    }

    /**
     * Builds a request post-processor that authenticates as the given account. The real filter
     * chain then places the principal in the security context, which is what
     * {@code @AuthenticationPrincipal} reads.
     */
    private RequestPostProcessor as(UUID userId, Role role) {
        UserPrincipal principal = new UserPrincipal(
                userId, userId + "@example.com", "hash", role, true);
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    }

    /** Authenticates as whoever {@link #callerId} currently names. */
    private RequestPostProcessor asCaller() {
        return as(callerId, callerRole);
    }

    /** Switches the acting account for subsequent requests in the same test. */
    private void actAs(UUID userId, Role role) {
        this.callerId = userId;
        this.callerRole = role;
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
    @DisplayName("create checks out the caller's cart and returns 201")
    void create_returnsCreated() throws Exception {
        when(orderService.checkout(eq(buyerId), any(), any(), nullable(FulfillmentMethod.class))).thenReturn(order);

        mockMvc.perform(post("/api/orders").with(asCaller()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("create takes the buyer from the token, not from the request")
    void create_usesCallerFromToken() throws Exception {
        when(orderService.checkout(eq(buyerId), any(), any(), nullable(FulfillmentMethod.class))).thenReturn(order);

        mockMvc.perform(post("/api/orders").with(asCaller()))
                .andExpect(status().isCreated());

        verify(orderService).checkout(eq(buyerId), any(), any(), nullable(FulfillmentMethod.class));
    }

    @Test
    @DisplayName("create returns 400 when the service refuses")
    void create_returnsBadRequest() throws Exception {
        when(orderService.checkout(eq(buyerId), any(), any(), nullable(FulfillmentMethod.class))).thenReturn(null);

        mockMvc.perform(post("/api/orders").with(asCaller()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("checkout returns 201")
    void checkout_returnsCreated() throws Exception {
        UUID addressId = UUID.randomUUID();
        when(orderService.checkout(buyerId, addressId, null, null)).thenReturn(order);

        mockMvc.perform(post("/api/orders/checkout").param("shippingAddressId", addressId.toString()).with(asCaller()))
                .andExpect(status().isCreated());

verify(orderService).checkout(buyerId, addressId, null, null);
    }

    @Test
    @DisplayName("checkout forwards the chosen payment method")
    void checkout_forwardsPaymentMethod() throws Exception {
        when(orderService.checkout(eq(buyerId), any(), eq(PaymentMethod.EFT),
                nullable(FulfillmentMethod.class))).thenReturn(order);

        mockMvc.perform(post("/api/orders/checkout")
                        .param("paymentMethod", "EFT")
                        .with(asCaller()))
                .andExpect(status().isCreated());

        verify(orderService).checkout(eq(buyerId), any(), eq(PaymentMethod.EFT),
                nullable(FulfillmentMethod.class));
    }

    @Test
    @DisplayName("an unknown payment method is a 400, not a silent fallback")
    void checkout_rejectsUnknownPaymentMethod() throws Exception {
        mockMvc.perform(post("/api/orders/checkout")
                        .param("paymentMethod", "bitcoin")
                        .with(asCaller()))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).checkout(any(), any(), any(), any());
    }

    @Test
    @DisplayName("checkout leaves the method unset when the caller names none")
    void checkout_passesNullWhenNoMethodGiven() throws Exception {
        when(orderService.checkout(buyerId, null, null, null)).thenReturn(order);

        mockMvc.perform(post("/api/orders/checkout").with(asCaller()))
                .andExpect(status().isCreated());

        verify(orderService).checkout(buyerId, null, null, null);
    }

    @Test
    @DisplayName("checkout forwards the selected fulfillment method")
    void checkout_forwardsFulfillmentMethod() throws Exception {
        when(orderService.checkout(eq(buyerId), any(), eq(PaymentMethod.EFT),
                eq(FulfillmentMethod.DELIVERY))).thenReturn(order);

        mockMvc.perform(post("/api/orders/checkout")
                        .param("paymentMethod", "EFT")
                        .param("fulfillmentMethod", "DELIVERY")
                        .param("shippingAddressId", UUID.randomUUID().toString())
                        .with(asCaller()))
                .andExpect(status().isCreated());

        verify(orderService).checkout(eq(buyerId), any(), eq(PaymentMethod.EFT),
                eq(FulfillmentMethod.DELIVERY));
    }

    @Test
    @DisplayName("read returns the caller's own order")
    void read_returnsOrder() throws Exception {
        when(orderService.read(id, buyerId)).thenReturn(order);

        mockMvc.perform(get("/api/orders/{id}", id).with(asCaller()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("read returns 404 when the order is not the caller's")
    void read_returnsNotFound() throws Exception {
        actAs(intruderId, Role.STUDENT);
        when(orderService.read(id, intruderId)).thenReturn(null);

        mockMvc.perform(get("/api/orders/{id}", id).with(asCaller()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("seller order list is scoped to the authenticated seller")
    void sellerOrders_usesCallerIdentityAndRole() throws Exception {
        actAs(intruderId, Role.VENDOR);
        when(orderService.getSellerOrders(intruderId, Role.VENDOR)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/seller").with(asCaller()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));

        verify(orderService).getSellerOrders(intruderId, Role.VENDOR);
    }

    @Test
    @DisplayName("update forces the path id onto the entity")
    void update_usesPathId() throws Exception {
        when(orderService.update(any(Order.class), eq(buyerId))).thenReturn(order);

        mockMvc.perform(put("/api/orders/{id}", id)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)).with(asCaller()))
                .andExpect(status().isOk());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).update(captor.capture(), eq(buyerId));
        assertThat(captor.getValue().getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("update returns 404 when the caller does not own the order")
    void update_returnsNotFound() throws Exception {
        actAs(intruderId, Role.STUDENT);
        when(orderService.update(any(Order.class), eq(intruderId))).thenReturn(null);

        mockMvc.perform(put("/api/orders/{id}", id)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)).with(asCaller()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a body cannot set the order status or total")
    void update_bodyCannotOverrideServerOwnedFields() throws Exception {
        when(orderService.update(any(Order.class), eq(buyerId))).thenReturn(order);

        mockMvc.perform(put("/api/orders/{id}", id)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + id + "\",\"status\":\"SHIPPED\",\"totalAmount\":0.01}").with(asCaller()))
                .andExpect(status().isOk());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).update(captor.capture(), eq(buyerId));
        assertThat(captor.getValue().getStatus())
                .as("status is server-owned and must not survive binding")
                .isNotEqualTo(OrderStatus.SHIPPED);
        assertThat(captor.getValue().getTotalAmount()).isNull();
    }

    @Test
    @DisplayName("delete returns 204 on success")
    void delete_returnsNoContent() throws Exception {
        when(orderService.delete(id, buyerId)).thenReturn(true);

        mockMvc.perform(delete("/api/orders/{id}", id).with(asCaller()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete returns 404 when the order is not the caller's")
    void delete_returnsNotFound() throws Exception {
        actAs(intruderId, Role.STUDENT);
        when(orderService.delete(id, intruderId)).thenReturn(false);

        mockMvc.perform(delete("/api/orders/{id}", id).with(asCaller()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getAll is scoped to the caller")
    void getAll_returnsList() throws Exception {
        when(orderService.getAll(buyerId)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders").with(asCaller()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));

        verify(orderService).getAll(buyerId);
    }

    @Test
    @DisplayName("getByBuyer returns the caller's orders")
    void getByBuyer_returnsList() throws Exception {
        when(orderService.getByBuyer(buyerId)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/buyer/{buyerId}", buyerId).with(asCaller()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("getByBuyer refuses another user's id without asking the service")
    void getByBuyer_rejectsOtherBuyer() throws Exception {
        mockMvc.perform(get("/api/orders/buyer/{buyerId}", intruderId).with(asCaller()))
                .andExpect(status().isNotFound());

        verify(orderService, never()).getByBuyer(any());
    }

    @Test
    @DisplayName("getByBuyerAndStatus filters by status")
    void getByBuyerAndStatus_filters() throws Exception {
        when(orderService.getByBuyerAndStatus(buyerId, OrderStatus.SHIPPED)).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/buyer/{buyerId}/status/{status}", buyerId, "SHIPPED").with(asCaller()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(orderService).getByBuyerAndStatus(buyerId, OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("getByBuyerAndStatus refuses another user's id")
    void getByBuyerAndStatus_rejectsOtherBuyer() throws Exception {
        mockMvc.perform(get("/api/orders/buyer/{buyerId}/status/{status}", intruderId, "SHIPPED").with(asCaller()))
                .andExpect(status().isNotFound());

        verify(orderService, never()).getByBuyerAndStatus(any(), any());
    }

    @Test
    @DisplayName("getByBuyerAndStatus rejects an unknown status with 400")
    void getByBuyerAndStatus_withInvalidStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/orders/buyer/{buyerId}/status/{status}", buyerId, "NOT_A_STATUS").with(asCaller()))
                .andExpect(status().isBadRequest());
    }

@Test
    @DisplayName("updateStatus passes the caller's id and role to the service")
    void updateStatus_appliesStatus() throws Exception {
        actAs(buyerId, Role.VENDOR);
        Order shipped = buildOrder(OrderStatus.SHIPPED);
        when(orderService.updateStatus(id, OrderStatus.SHIPPED, buyerId, Role.VENDOR)).thenReturn(shipped);

        mockMvc.perform(patch("/api/orders/{id}/status", id)
                        .param("status", "SHIPPED").with(asCaller()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        // The id matters as much as the role: without it the service cannot tell whose order this
        // is, which is how any vendor ended up able to advance any other vendor's order.
        verify(orderService).updateStatus(id, OrderStatus.SHIPPED, buyerId, Role.VENDOR);
    }

    @Test
    @DisplayName("updateStatus returns 404 when the service refuses")
    void updateStatus_returnsNotFound() throws Exception {
        when(orderService.updateStatus(id, OrderStatus.SHIPPED, buyerId, Role.STUDENT)).thenReturn(null);

        mockMvc.perform(patch("/api/orders/{id}/status", id)
                        .param("status", "SHIPPED").with(asCaller()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("updateStatus requires the status parameter")
    void updateStatus_withoutStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/orders/{id}/status", id).with(asCaller()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cancel returns 204 when the order is cancellable")
    void cancel_returnsNoContent() throws Exception {
        when(orderService.cancel(id, buyerId)).thenReturn(true);

        mockMvc.perform(patch("/api/orders/{id}/cancel", id).with(asCaller()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("cancel returns 400 when the order cannot be cancelled")
    void cancel_returnsBadRequest() throws Exception {
        when(orderService.cancel(id, buyerId)).thenReturn(false);

        mockMvc.perform(patch("/api/orders/{id}/cancel", id).with(asCaller()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cancel takes the buyer from the token and ignores a buyerId parameter")
    void cancel_ignoresBuyerIdParameter() throws Exception {
        when(orderService.cancel(id, buyerId)).thenReturn(true);

        // The old endpoint accepted ?buyerId= and trusted it, so anybody could cancel anybody's
        // order by passing the victim's id.
        mockMvc.perform(patch("/api/orders/{id}/cancel", id)
                        .param("buyerId", intruderId.toString()).with(asCaller()))
                .andExpect(status().isNoContent());

        verify(orderService).cancel(id, buyerId);
    }

    @Test
    @DisplayName("cancel returns 400 when another user tries to cancel the order")
    void cancel_rejectsOtherBuyer() throws Exception {
        actAs(intruderId, Role.STUDENT);
        when(orderService.cancel(id, intruderId)).thenReturn(false);

        mockMvc.perform(patch("/api/orders/{id}/cancel", id).with(asCaller()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("an order never leaks the buyer's password hash")
    void order_doesNotLeakPasswordHash() throws Exception {
        when(orderService.read(id, buyerId)).thenReturn(order);

        String body = mockMvc.perform(get("/api/orders/{id}", id).with(asCaller()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("passwordHash").doesNotContain("\"hash\"");
    }
}