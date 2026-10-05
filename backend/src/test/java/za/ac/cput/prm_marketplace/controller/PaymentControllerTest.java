package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.service.IPaymentService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.as;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.asFaculty;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.asStudent;

/**
 * These run the real security filter chain so the routes are exercised the way a caller reaches
 * them, and so the tests can prove that the old unguarded routes are gone rather than merely
 * unused.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IPaymentService paymentService;

    private UUID payerId;
    private UUID intruderId;
    private UUID paymentId;
    private UUID orderId;
    private Payment payment;

    @BeforeEach
    void setUp() {
        payerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        payment = buildPayment(PaymentStatus.PENDING, payerId);
    }

    private Payment buildPayment(PaymentStatus status, UUID payerId) {
        return new Payment.Builder()
                .setId(paymentId)
                .setOrderId(orderId)
                .setUserId(payerId)
                .setAmount(new BigDecimal("780.00"))
                .setMethod(PaymentMethod.CARD)
                .setStatus(status)
                .setTransactionReference("PAY-TEST00000001")
                .build();
    }

    private String body(Payment value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    @DisplayName("a payment is filed for the caller, not for the userId in the body")
    void create_takesThePayerFromTheToken() throws Exception {
        when(paymentService.create(any(), eq(payerId))).thenReturn(payment);

        // The body claims the payment belongs to somebody else.
        Payment hostile = new Payment.Builder()
                .copy(payment)
                .setUserId(intruderId)
                .build();

        mockMvc.perform(post("/api/payments")
                        .with(asStudent(payerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(hostile)))
                .andExpect(status().isCreated());

        verify(paymentService).create(any(), eq(payerId));
    }

    @Test
    @DisplayName("the payer is never taken from the request body")
    void create_doesNotBindThePayerFromTheBody() throws Exception {
        when(paymentService.create(any(), eq(payerId))).thenReturn(payment);

        Payment hostile = new Payment.Builder()
                .copy(payment)
                .setUserId(intruderId)
                .build();

        mockMvc.perform(post("/api/payments")
                        .with(asStudent(payerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(hostile)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(payerId.toString()));
    }

    @Test
    @DisplayName("a rejected payment is a bad request")
    void create_returnsBadRequestWhenServiceRejects() throws Exception {
        when(paymentService.create(any(), eq(payerId))).thenReturn(null);

        mockMvc.perform(post("/api/payments")
                        .with(asStudent(payerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(payment)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("an anonymous caller cannot start a payment")
    void create_rejectsAnonymous() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(payment)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("the bare GET returns the caller's own payments")
    void getAll_isScopedToTheCaller() throws Exception {
        when(paymentService.getByUserId(payerId)).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/payments").with(asStudent(payerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("reading a payment is scoped to the caller")
    void read_isScopedToTheCaller() throws Exception {
        when(paymentService.read(paymentId, payerId)).thenReturn(payment);

        mockMvc.perform(get("/api/payments/{id}", paymentId).with(asStudent(payerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("CARD"));
    }

    @Test
    @DisplayName("another account's payment reads as not found")
    void read_ofSomebodyElsesPaymentIsNotFound() throws Exception {
        when(paymentService.read(paymentId, intruderId)).thenReturn(null);

        mockMvc.perform(get("/api/payments/{id}", paymentId).with(asStudent(intruderId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a missing payment is not found")
    void read_missingPayment_returnsNotFound() throws Exception {
        when(paymentService.read(paymentId, payerId)).thenReturn(null);

        mockMvc.perform(get("/api/payments/{id}", paymentId).with(asStudent(payerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("updating targets the payment named in the path")
    void update_usesTheIdFromThePath() throws Exception {
        when(paymentService.update(eq(paymentId), any(), eq(payerId))).thenReturn(payment);

        mockMvc.perform(put("/api/payments/{id}", paymentId)
                        .with(asStudent(payerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(payment)))
                .andExpect(status().isOk());

        verify(paymentService).update(eq(paymentId), any(), eq(payerId));
    }

    @Test
    @DisplayName("a body id cannot redirect the update to another payment")
    void update_ignoresTheBodyId() throws Exception {
        UUID otherPaymentId = UUID.randomUUID();
        when(paymentService.update(eq(paymentId), any(), eq(payerId))).thenReturn(payment);

        Payment hostile = new Payment.Builder().copy(payment).setId(otherPaymentId).build();

        mockMvc.perform(put("/api/payments/{id}", paymentId)
                        .with(asStudent(payerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(hostile)))
                .andExpect(status().isOk());

        verify(paymentService).update(eq(paymentId), any(), eq(payerId));
    }

    @Test
    @DisplayName("a payment that is no longer pending cannot be edited")
    void update_nonPendingPayment_returnsNotFound() throws Exception {
        when(paymentService.update(eq(paymentId), any(), eq(payerId))).thenReturn(null);

        mockMvc.perform(put("/api/payments/{id}", paymentId)
                        .with(asStudent(payerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(payment)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("payments for an order are scoped to the caller")
    void getByOrderId_isScopedToTheCaller() throws Exception {
        when(paymentService.getByOrderId(orderId, payerId)).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/payments/order/{orderId}", orderId).with(asStudent(payerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("another account's order yields no payments")
    void getByOrderId_ofSomebodyElsesOrderIsEmpty() throws Exception {
        when(paymentService.getByOrderId(orderId, intruderId)).thenReturn(List.of());

        mockMvc.perform(get("/api/payments/order/{orderId}", orderId).with(asStudent(intruderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("faculty can complete a payment")
    void updateStatus_validTransition_returnsOk() throws Exception {
        when(paymentService.updateStatus(eq(paymentId), eq(PaymentStatus.COMPLETED),
                eq(payerId), eq(Role.FACULTY))).thenReturn(buildPayment(PaymentStatus.COMPLETED, payerId));

        mockMvc.perform(patch("/api/payments/{id}/status", paymentId)
                        .with(asFaculty(payerId))
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("a payer asking to settle their own payment is not found")
    void updateStatus_payerCannotSettle_returnsNotFound() throws Exception {
        // The role the controller hands the service is the token's, so a student cannot claim
        // faculty authority by putting anything in the request.
        when(paymentService.updateStatus(eq(paymentId), eq(PaymentStatus.COMPLETED),
                eq(payerId), eq(Role.STUDENT))).thenReturn(null);

        mockMvc.perform(patch("/api/payments/{id}/status", paymentId)
                        .with(asStudent(payerId))
                        .param("status", "COMPLETED"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the role comes from the token, not from the request")
    void updateStatus_roleIsNotTakenFromTheRequest() throws Exception {
        when(paymentService.updateStatus(eq(paymentId), eq(PaymentStatus.COMPLETED),
                eq(payerId), eq(Role.STUDENT))).thenReturn(null);

        // Asking for the role by name in the body must not elevate the caller.
        mockMvc.perform(patch("/api/payments/{id}/status", paymentId)
                        .with(asStudent(payerId))
                        .param("status", "COMPLETED")
                        .param("role", "FACULTY"))
                .andExpect(status().isNotFound());

        verify(paymentService).updateStatus(eq(paymentId), eq(PaymentStatus.COMPLETED),
                eq(payerId), eq(Role.STUDENT));
    }

    @Test
    @DisplayName("a caller cannot move somebody else's payment")
    void updateStatus_ofSomebodyElsesPaymentIsNotFound() throws Exception {
        when(paymentService.updateStatus(eq(paymentId), eq(PaymentStatus.COMPLETED),
                eq(intruderId), eq(Role.STUDENT))).thenReturn(null);

        mockMvc.perform(patch("/api/payments/{id}/status", paymentId)
                        .with(asStudent(intruderId))
                        .param("status", "COMPLETED"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a refused transition is not found")
    void updateStatus_refusedTransition_returnsNotFound() throws Exception {
        when(paymentService.updateStatus(eq(paymentId), eq(PaymentStatus.REFUNDED),
                eq(payerId), eq(Role.STUDENT))).thenReturn(null);

        mockMvc.perform(patch("/api/payments/{id}/status", paymentId)
                        .with(asStudent(payerId))
                        .param("status", "REFUNDED"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("faculty may refund")
    void updateStatus_facultyMayRefund() throws Exception {
        Payment refunded = buildPayment(PaymentStatus.REFUNDED, payerId);
        when(paymentService.updateStatus(eq(paymentId), eq(PaymentStatus.REFUNDED),
                eq(payerId), eq(Role.FACULTY))).thenReturn(refunded);

        mockMvc.perform(patch("/api/payments/{id}/status", paymentId)
                        .with(as(payerId, Role.FACULTY))
                        .param("status", "REFUNDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    @DisplayName("an unknown status is a bad request")
    void updateStatus_unknownStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/payments/{id}/status", paymentId)
                        .with(asStudent(payerId))
                        .param("status", "NOPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("listing another account's payments by user id is no longer possible")
    void getByUserIdRouteIsGone() throws Exception {
        mockMvc.perform(get("/api/payments/user/{userId}", intruderId).with(asStudent(intruderId)))
                .andExpect(status().isNotFound());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("the old unprefixed routes are gone")
    void legacyRoutesAreGone() throws Exception {
        mockMvc.perform(get("/payments").with(asStudent(payerId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/payments/{id}", paymentId).with(asStudent(payerId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/payments/{id}", paymentId).with(asStudent(payerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a payment cannot be deleted: it is a financial record")
    void deleteIsNotAvailable() throws Exception {
        // 405 rather than 404: the path is a real route for GET and PUT, there is simply no DELETE.
        mockMvc.perform(delete("/api/payments/{id}", paymentId)
                        .with(asStudent(payerId)))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("a body cannot set the status, reference or timestamps directly")
    void serverOwnedFieldsAreNotClientSettable() throws Exception {
        when(paymentService.create(any(), eq(payerId))).thenReturn(payment);

        String hostile = """
                {
                  "orderId": "%s",
                  "userId": "%s",
                  "amount": 1.00,
                  "method": "CARD",
                  "status": "COMPLETED",
                  "transactionReference": "PAY-FORGED",
                  "createdAt": "2020-01-01T00:00:00"
                }
                """.formatted(orderId, intruderId);

        mockMvc.perform(post("/api/payments")
                        .with(asStudent(payerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hostile))
                .andExpect(status().isCreated());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentService).create(captor.capture(), eq(payerId));

        Payment submitted = captor.getValue();
        assertThat(submitted.getUserId()).isNull();
        assertThat(submitted.getStatus()).isNull();
        assertThat(submitted.getTransactionReference()).isNull();
        assertThat(submitted.getCreatedAt()).isNull();
    }
}