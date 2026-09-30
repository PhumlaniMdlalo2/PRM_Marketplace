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
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.service.IOrderItemService;

import java.math.BigDecimal;
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

@WebMvcTest(OrderItemController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IOrderItemService orderItemService;

    private UUID id;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        orderItem = new OrderItem.Builder()
                .setId(id)
                .setProduct(new Product.Builder()
                        .id(UUID.randomUUID())
                        .name("Textbook")
                        .build())
                .setQuantity(2)
                .setPriceAtPurchase(new BigDecimal("45.00"))
                .build();
    }

    @Test
    @DisplayName("create returns 201 with the order item")
    void create_returnsCreated() throws Exception {
        when(orderItemService.create(any(OrderItem.class))).thenReturn(orderItem);

        mockMvc.perform(post("/api/order-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("create returns 400 when the service refuses")
    void create_returnsBadRequest() throws Exception {
        when(orderItemService.create(any(OrderItem.class))).thenReturn(null);

        mockMvc.perform(post("/api/order-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderItem)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("read returns the order item")
    void read_returnsOrderItem() throws Exception {
        when(orderItemService.read(id)).thenReturn(orderItem);

        mockMvc.perform(get("/api/order-items/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("read returns 404 for an unknown order item")
    void read_returnsNotFound() throws Exception {
        when(orderItemService.read(id)).thenReturn(null);

        mockMvc.perform(get("/api/order-items/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update returns 200 on success")
    void update_returnsOk() throws Exception {
        when(orderItemService.update(any(OrderItem.class))).thenReturn(orderItem);

        mockMvc.perform(put("/api/order-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderItem)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("update returns 404 when the service cannot update")
    void update_returnsNotFound() throws Exception {
        when(orderItemService.update(any(OrderItem.class))).thenReturn(null);

        mockMvc.perform(put("/api/order-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderItem)))
                .andExpect(status().isNotFound());

        verify(orderItemService).update(any());
    }

    @Test
    @DisplayName("delete returns 204 on success")
    void delete_returnsNoContent() throws Exception {
        when(orderItemService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/api/order-items/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete returns 404 for an unknown order item")
    void delete_returnsNotFound() throws Exception {
        when(orderItemService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/api/order-items/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getAll returns every order item")
    void getAll_returnsList() throws Exception {
        when(orderItemService.getAll()).thenReturn(List.of(orderItem));

        mockMvc.perform(get("/api/order-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }
}
