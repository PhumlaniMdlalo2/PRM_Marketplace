package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.NotificationType;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.OrderRepository;
import za.ac.cput.prm_marketplace.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID id;
    private UUID orderId;
    private UUID payerId;
    private UUID intruderId;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        orderId = UUID.randomUUID();
        payerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
    }

    private Order orderOwnedBy(UUID buyerId, String total) {
        return new Order.Builder()
                .setId(orderId)
                .setBuyer(new User.Builder().setId(buyerId).setEmail(buyerId + "@example.com").build())
                .setTotalAmount(new BigDecimal(total))
                .build();
    }

    private Payment buildPayment(PaymentStatus status) {
        return new Payment.Builder()
                .setId(id)
                .setOrderId(orderId)
                .setUserId(payerId)
                .setAmount(new BigDecimal("780.00"))
                .setMethod(PaymentMethod.CARD)
                .setStatus(status)
                .setTransactionReference("PAY-TEST00000001")
                .setCreatedAt(LocalDateTime.now())
                .build();
    }

    // create

    @Test
    @DisplayName("the payment is saved with the caller as payer, not the body's userId")
    void create_takesThePayerFromTheRequester() {
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "780.00")));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment request = new Payment.Builder()
                .setOrderId(orderId)
                .setUserId(intruderId)
                .setAmount(new BigDecimal("780.00"))
                .setMethod(PaymentMethod.EFT)
                .build();

        Payment saved = paymentService.create(request, payerId);

        assertThat(saved.getUserId()).isEqualTo(payerId);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getTransactionReference()).isNotBlank();
    }

    @Test
    @DisplayName("a payment cannot be filed against somebody else's order")
    void create_rejectsAnOrderTheCallerDoesNotOwn() {
        when(orderRepository.findByIdAndBuyerId(orderId, intruderId))
                .thenReturn(Optional.empty());

        Payment request = new Payment.Builder()
                .setOrderId(orderId)
                .setAmount(new BigDecimal("780.00"))
                .setMethod(PaymentMethod.EFT)
                .build();

        assertThat(paymentService.create(request, intruderId)).isNull();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("an omitted amount defaults to the order total")
    void create_defaultsTheAmountToTheOrderTotal() {
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "1234.56")));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment request = new Payment.Builder()
                .setOrderId(orderId).setMethod(PaymentMethod.EFT).build();

        Payment saved = paymentService.create(request, payerId);

        assertThat(saved.getAmount()).isEqualByComparingTo("1234.56");
    }

    @Test
    @DisplayName("a client cannot pay more than the order is for")
    void create_rejectsAnAmountAboveTheOrderTotal() {
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "780.00")));

        Payment request = new Payment.Builder()
                .setOrderId(orderId)
                .setAmount(new BigDecimal("5000.00"))
                .setMethod(PaymentMethod.EFT)
                .build();

        assertThat(paymentService.create(request, payerId)).isNull();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("a zero or negative amount is rejected")
    void create_rejectsNonPositiveAmounts() {
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "780.00")));

        Payment request = new Payment.Builder()
                .setOrderId(orderId)
                .setAmount(new BigDecimal("0.00"))
                .setMethod(PaymentMethod.EFT)
                .build();

        assertThat(paymentService.create(request, payerId)).isNull();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("an order with nothing owing cannot be paid")
    void create_rejectsAnOrderWithNoTotal() {
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "0.00")));

        Payment request = new Payment.Builder()
                .setOrderId(orderId).setMethod(PaymentMethod.EFT).build();

        assertThat(paymentService.create(request, payerId)).isNull();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("a missing method is rejected")
    void create_rejectsAMissingMethod() {
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "780.00")));

        Payment request = new Payment.Builder()
                .setOrderId(orderId).setAmount(new BigDecimal("10.00")).build();

        assertThat(paymentService.create(request, payerId)).isNull();
    }

    @Test
    @DisplayName("null inputs are rejected without touching the repositories")
    void create_rejectsNulls() {
        assertThat(paymentService.create(null, payerId)).isNull();
        assertThat(paymentService.create(buildPayment(PaymentStatus.PENDING), null)).isNull();

        verifyNoInteractions(orderRepository, paymentRepository);
    }

    // read

    @Test
    @DisplayName("reading is scoped to the payer")
    void read_isScopedToThePayer() {
        Payment payment = buildPayment(PaymentStatus.PENDING);
        when(paymentRepository.findByIdAndUserId(id, payerId)).thenReturn(Optional.of(payment));

        assertThat(paymentService.read(id, payerId)).isSameAs(payment);
        verify(paymentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("another account's payment is not found")
    void read_ofSomebodyElsesPaymentIsNull() {
        when(paymentRepository.findByIdAndUserId(id, intruderId)).thenReturn(Optional.empty());

        assertThat(paymentService.read(id, intruderId)).isNull();
    }

    @Test
    @DisplayName("read is null-safe")
    void read_isNullSafe() {
        assertThat(paymentService.read(null, payerId)).isNull();
        assertThat(paymentService.read(id, null)).isNull();

        verifyNoInteractions(paymentRepository);
    }

    // update

    @Test
    @DisplayName("only the method changes; the amount in the body is ignored")
    void update_doesNotLetTheBodyChangeTheAmount() {
        Payment existing = buildPayment(PaymentStatus.PENDING);
        when(paymentRepository.findByIdAndUserId(id, payerId)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment request = new Payment.Builder()
                .setAmount(new BigDecimal("1.00"))
                .setMethod(PaymentMethod.EFT)
                .build();

        Payment updated = paymentService.update(id, request, payerId);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());

        assertThat(updated.getAmount()).isEqualByComparingTo("780.00");
        assertThat(updated.getMethod()).isEqualTo(PaymentMethod.EFT);
        assertThat(updated.getUserId()).isEqualTo(payerId);
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(captor.getValue().getTransactionReference()).isEqualTo("PAY-TEST00000001");
    }

    @Test
    @DisplayName("a payment that is no longer pending cannot be edited")
    void update_rejectsNonPending() {
        when(paymentRepository.findByIdAndUserId(id, payerId))
                .thenReturn(Optional.of(buildPayment(PaymentStatus.COMPLETED)));

        assertThat(paymentService.update(id,
                new Payment.Builder().setMethod(PaymentMethod.EFT).build(), payerId)).isNull();

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("another account's payment cannot be edited")
    void update_rejectsSomebodyElsesPayment() {
        when(paymentRepository.findByIdAndUserId(id, intruderId)).thenReturn(Optional.empty());

        assertThat(paymentService.update(id,
                new Payment.Builder().setMethod(PaymentMethod.EFT).build(), intruderId)).isNull();

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("update is null-safe")
    void update_isNullSafe() {
        assertThat(paymentService.update(null, buildPayment(PaymentStatus.PENDING), payerId)).isNull();
        assertThat(paymentService.update(id, null, payerId)).isNull();
        assertThat(paymentService.update(id, buildPayment(PaymentStatus.PENDING), null)).isNull();

        verifyNoInteractions(paymentRepository);
    }

    // listing

    @Test
    @DisplayName("listing returns the caller's own payments")
    void getByUserId_returnsTheCallersPayments() {
        List<Payment> payments = List.of(buildPayment(PaymentStatus.PENDING));
        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(payerId)).thenReturn(payments);

        assertThat(paymentService.getByUserId(payerId)).isSameAs(payments);
    }

    @Test
    @DisplayName("listing is empty for a null requester")
    void getByUserId_isEmptyForNull() {
        assertThat(paymentService.getByUserId(null)).isEmpty();
    }

    @Test
    @DisplayName("payments for a caller's order are returned")
    void getByOrderId_returnsPaymentsForAnOwnedOrder() {
        List<Payment> payments = List.of(buildPayment(PaymentStatus.PENDING));
        when(orderRepository.existsByIdAndBuyerId(orderId, payerId)).thenReturn(true);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(payments);

        assertThat(paymentService.getByOrderId(orderId, payerId)).isSameAs(payments);
    }

    @Test
    @DisplayName("another account's order yields no payments")
    void getByOrderId_isEmptyForAnOrderTheCallerDoesNotOwn() {
        when(orderRepository.existsByIdAndBuyerId(orderId, intruderId)).thenReturn(false);

        assertThat(paymentService.getByOrderId(orderId, intruderId)).isEmpty();

        verify(paymentRepository, never()).findByOrderId(any());
    }

    // updateStatus

    @Test
    @DisplayName("a valid transition is saved and the payer is notified")
    void updateStatus_savesAndNotifies() {
        Payment existing = buildPayment(PaymentStatus.PENDING);
        when(paymentRepository.findByIdAndUserId(id, payerId)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment updated = paymentService.updateStatus(id, PaymentStatus.COMPLETED, payerId, Role.FACULTY);

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(updated.getPaidAt()).isNotNull();
        verify(notificationService).send(eq(payerId), eq(NotificationType.PAYMENT),
                anyString(), anyString());
    }

    @Test
    @DisplayName("the payer cannot mark their own payment as paid")
    void updateStatus_completionRequiresFaculty() {
        // No stubbing: the authority check runs before the payment is loaded, so the repository is
        // never consulted. That ordering is the point -- an unauthorised caller learns nothing about
        // whether the id exists.
        assertThat(paymentService.updateStatus(id, PaymentStatus.COMPLETED, payerId, Role.STUDENT))
                .isNull();

        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("the payer cannot mark their own payment as failed either")
    void updateStatus_failureRequiresFaculty() {
        // Otherwise a buyer could erase a completed charge they regret.
        assertThat(paymentService.updateStatus(id, PaymentStatus.FAILED, payerId, Role.VENDOR))
                .isNull();

        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("no role other than faculty can settle a payment")
    void updateStatus_everyNonFacultyRoleIsRefused() {
        for (Role role : List.of(Role.STUDENT, Role.VENDOR, Role.RESIDENT)) {
            assertThat(paymentService.updateStatus(id, PaymentStatus.COMPLETED, payerId, role))
                    .as("role %s must not settle a payment", role)
                    .isNull();
        }

        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("the payer may retry their own payment")
    void updateStatus_retryingIsAllowedForThePayer() {
        when(paymentRepository.findByIdAndUserId(id, payerId))
                .thenReturn(Optional.of(buildPayment(PaymentStatus.FAILED)));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment updated = paymentService.updateStatus(id, PaymentStatus.PENDING, payerId, Role.STUDENT);

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.PENDING);
        // A retry asserts nothing about money that has moved, so it is not a settlement.
        verify(notificationService, never()).send(any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("the payer cannot refund their own payment")
    void updateStatus_refundRequiresFaculty() {
        assertThat(paymentService.updateStatus(id, PaymentStatus.REFUNDED, payerId, Role.STUDENT)).isNull();

        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("faculty may refund")
    void updateStatus_facultyMayRefund() {
        when(paymentRepository.findByIdAndUserId(id, payerId))
                .thenReturn(Optional.of(buildPayment(PaymentStatus.COMPLETED)));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment updated = paymentService.updateStatus(id, PaymentStatus.REFUNDED, payerId, Role.FACULTY);

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("even faculty cannot move somebody else's payment")
    void updateStatus_rejectsSomebodyElsesPayment() {
        when(paymentRepository.findByIdAndUserId(id, intruderId)).thenReturn(Optional.empty());

        // Faculty has authority over settling, not over whose payment it is. Ownership is still
        // scoped to the caller, so this stays refused at full privilege.
        assertThat(paymentService.updateStatus(id, PaymentStatus.COMPLETED, intruderId, Role.FACULTY))
                .isNull();

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("a transition the lifecycle does not allow is refused")
    void updateStatus_rejectsAnIllegalTransition() {
        when(paymentRepository.findByIdAndUserId(id, payerId))
                .thenReturn(Optional.of(buildPayment(PaymentStatus.PENDING)));

        // PENDING cannot go straight to REFUNDED.
        assertThat(paymentService.updateStatus(id, PaymentStatus.REFUNDED, payerId, Role.FACULTY))
                .isNull();

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus is null-safe")
    void updateStatus_isNullSafe() {
        assertThat(paymentService.updateStatus(null, PaymentStatus.COMPLETED, payerId, Role.STUDENT)).isNull();
        assertThat(paymentService.updateStatus(id, null, payerId, Role.STUDENT)).isNull();
        assertThat(paymentService.updateStatus(id, PaymentStatus.COMPLETED, null, Role.STUDENT)).isNull();

        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("a null role is not faculty, so settling is refused without a database read")
    void updateStatus_nullRoleCannotSettle() {
        // Worth pinning down because `status != PENDING && role != FACULTY` would otherwise be read
        // as permitting a null role through some other path.
        assertThat(paymentService.updateStatus(id, PaymentStatus.COMPLETED, payerId, null)).isNull();

        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("a notification failure does not undo the saved payment")
    void updateStatus_survivesANotificationFailure() {
        when(paymentRepository.findByIdAndUserId(id, payerId))
                .thenReturn(Optional.of(buildPayment(PaymentStatus.PENDING)));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("sms gateway down"))
                .when(notificationService).send(any(), any(), anyString(), anyString());

        Payment updated = paymentService.updateStatus(id, PaymentStatus.COMPLETED, payerId, Role.FACULTY);

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRepository).save(any(Payment.class));
    }
}