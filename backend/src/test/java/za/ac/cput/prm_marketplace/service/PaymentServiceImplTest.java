package za.ac.cput.prm_marketplace.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.NotificationType;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;
import za.ac.cput.prm_marketplace.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID id;
    private UUID orderId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
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
                .setCreatedAt(LocalDateTime.now())
                .build();
    }

    // create

    @Test
    void create_validPayment_savesAsPending() {
        Payment request = new Payment.Builder()
                .setOrderId(orderId).setUserId(userId)
                .setAmount(new BigDecimal("780")).setMethod(PaymentMethod.EFT)
                .setStatus(PaymentStatus.COMPLETED) // client must not be able to skip PENDING
                .build();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment created = paymentService.create(request);

        assertNotNull(created);
        assertEquals(PaymentStatus.PENDING, created.getStatus());
        assertNotNull(created.getTransactionReference());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void create_invalidPayment_returnsNull() {
        Payment request = new Payment.Builder().setOrderId(orderId).build();

        assertNull(paymentService.create(request));
        assertNull(paymentService.create(null));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    // read

    @Test
    void read_existingPayment_returnsIt() {
        Payment payment = buildPayment(PaymentStatus.PENDING);
        when(paymentRepository.findById(id)).thenReturn(Optional.of(payment));

        assertEquals(payment, paymentService.read(id));
    }

    @Test
    void read_missingPayment_returnsNull() {
        when(paymentRepository.findById(id)).thenReturn(Optional.empty());

        assertNull(paymentService.read(id));
        assertNull(paymentService.read(null));
    }

    // update

    @Test
    void update_pendingPayment_changesMethodAndAmount() {
        when(paymentRepository.findById(id)).thenReturn(Optional.of(buildPayment(PaymentStatus.PENDING)));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment request = new Payment.Builder()
                .setId(id).setAmount(new BigDecimal("900")).setMethod(PaymentMethod.WALLET).build();

        Payment updated = paymentService.update(request);

        assertNotNull(updated);
        assertEquals(PaymentMethod.WALLET, updated.getMethod());
        assertEquals(0, new BigDecimal("900.00").compareTo(updated.getAmount()));
        assertEquals("PAY-TEST00000001", updated.getTransactionReference());
    }

    @Test
    void update_completedPayment_returnsNull() {
        when(paymentRepository.findById(id)).thenReturn(Optional.of(buildPayment(PaymentStatus.COMPLETED)));

        assertNull(paymentService.update(new Payment.Builder().setId(id).setMethod(PaymentMethod.EFT).build()));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    // delete

    @Test
    void delete_existingPayment_returnsTrue() {
        when(paymentRepository.existsById(id)).thenReturn(true);

        assertTrue(paymentService.delete(id));
        verify(paymentRepository).deleteById(id);
    }

    @Test
    void delete_missingPayment_returnsFalse() {
        when(paymentRepository.existsById(id)).thenReturn(false);

        assertFalse(paymentService.delete(id));
        verify(paymentRepository, never()).deleteById(any(UUID.class));
    }

    // updateStatus

    @Test
    void updateStatus_pendingToCompleted_setsPaidAtAndNotifiesUser() {
        when(paymentRepository.findById(id)).thenReturn(Optional.of(buildPayment(PaymentStatus.PENDING)));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.updateStatus(id, PaymentStatus.COMPLETED);

        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getPaidAt());
        verify(notificationService).send(eq(userId), eq(NotificationType.PAYMENT), eq("Payment successful"), anyString());
    }

    @Test
    void updateStatus_pendingToFailed_notifiesUser() {
        when(paymentRepository.findById(id)).thenReturn(Optional.of(buildPayment(PaymentStatus.PENDING)));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.updateStatus(id, PaymentStatus.FAILED);

        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertNull(result.getPaidAt());
        verify(notificationService).send(eq(userId), eq(NotificationType.PAYMENT), eq("Payment failed"), anyString());
    }

    @Test
    void updateStatus_failedBackToPending_doesNotNotify() {
        when(paymentRepository.findById(id)).thenReturn(Optional.of(buildPayment(PaymentStatus.FAILED)));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.updateStatus(id, PaymentStatus.PENDING);

        assertEquals(PaymentStatus.PENDING, result.getStatus());
        verifyNoInteractions(notificationService);
    }

    @Test
    void updateStatus_invalidTransition_returnsNullAndDoesNotSave() {
        when(paymentRepository.findById(id)).thenReturn(Optional.of(buildPayment(PaymentStatus.PENDING)));

        assertNull(paymentService.updateStatus(id, PaymentStatus.REFUNDED));
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void updateStatus_missingPayment_returnsNull() {
        when(paymentRepository.findById(id)).thenReturn(Optional.empty());

        assertNull(paymentService.updateStatus(id, PaymentStatus.COMPLETED));
    }

    @Test
    void updateStatus_notificationFailure_stillReturnsSavedPayment() {
        when(paymentRepository.findById(id)).thenReturn(Optional.of(buildPayment(PaymentStatus.PENDING)));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationService.send(any(), any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("db down"));

        Payment result = paymentService.updateStatus(id, PaymentStatus.COMPLETED);

        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
    }
}
