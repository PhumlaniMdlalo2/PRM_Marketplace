package za.ac.cput.prm_marketplace.controller;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;
import za.ac.cput.prm_marketplace.service.IPaymentService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IPaymentService paymentService;

    private UUID id;
    private UUID orderId;
    private UUID userId;
    private Payment payment;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
        payment = buildPayment(PaymentStatus.PENDING);
    }

    private Payment buildPayment(PaymentStatus status) {
        return new Payment.Builder()
                .setId(id)
                .setOrderId(orderId)
                .setUserId(userId)
                .setAmount(new BigDecimal("780.00"))
                .setMethod(PaymentMethod.CARD)
                .setStatus(status)
                .setTransactionReference("PAY-TEST00000001")
                .build();
    }

    @Test
    void create_returnsCreatedWhenServiceSucceeds() throws Exception {
        when(paymentService.create(any(Payment.class))).thenReturn(payment);

        mockMvc.perform(post("/payments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.transactionReference").value("PAY-TEST00000001"));
    }

    @Test
    void create_returnsBadRequestWhenServiceRejects() throws Exception {
        when(paymentService.create(any(Payment.class))).thenReturn(null);

        mockMvc.perform(post("/payments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void read_existingPayment_returnsOk() throws Exception {
        when(paymentService.read(id)).thenReturn(payment);

        mockMvc.perform(get("/payments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("CARD"));
    }

    @Test
    void read_missingPayment_returnsNotFound() throws Exception {
        when(paymentService.read(id)).thenReturn(null);

        mockMvc.perform(get("/payments/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_existingPendingPayment_returnsOk() throws Exception {
        when(paymentService.read(id)).thenReturn(payment);
        when(paymentService.update(any(Payment.class))).thenReturn(payment);

        mockMvc.perform(put("/payments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isOk());
    }

    @Test
    void update_missingPayment_returnsNotFound() throws Exception {
        when(paymentService.read(id)).thenReturn(null);

        mockMvc.perform(put("/payments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_nonPendingPayment_returnsConflict() throws Exception {
        when(paymentService.read(id)).thenReturn(buildPayment(PaymentStatus.COMPLETED));
        when(paymentService.update(any(Payment.class))).thenReturn(null);

        mockMvc.perform(put("/payments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_existingPayment_returnsNoContent() throws Exception {
        when(paymentService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/payments/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_missingPayment_returnsNotFound() throws Exception {
        when(paymentService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/payments/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_returnsList() throws Exception {
        when(paymentService.getAll()).thenReturn(Arrays.asList(payment, buildPayment(PaymentStatus.FAILED)));

        mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByOrderId_returnsPaymentsForOrder() throws Exception {
        when(paymentService.getByOrderId(orderId)).thenReturn(Arrays.asList(payment));

        mockMvc.perform(get("/payments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByUserId_returnsPaymentsForUser() throws Exception {
        when(paymentService.getByUserId(userId)).thenReturn(Arrays.asList(payment));

        mockMvc.perform(get("/payments/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updateStatus_validTransition_returnsOk() throws Exception {
        when(paymentService.read(id)).thenReturn(payment);
        when(paymentService.updateStatus(id, PaymentStatus.COMPLETED))
                .thenReturn(buildPayment(PaymentStatus.COMPLETED));

        mockMvc.perform(patch("/payments/{id}/status", id).param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void updateStatus_invalidTransition_returnsConflict() throws Exception {
        when(paymentService.read(id)).thenReturn(payment);
        when(paymentService.updateStatus(id, PaymentStatus.REFUNDED)).thenReturn(null);

        mockMvc.perform(patch("/payments/{id}/status", id).param("status", "REFUNDED"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateStatus_missingPayment_returnsNotFound() throws Exception {
        when(paymentService.read(id)).thenReturn(null);

        mockMvc.perform(patch("/payments/{id}/status", id).param("status", "COMPLETED"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_unknownStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/payments/{id}/status", id).param("status", "NOPE"))
                .andExpect(status().isBadRequest());
    }
}
