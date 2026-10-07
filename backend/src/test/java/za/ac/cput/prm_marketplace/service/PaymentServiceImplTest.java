package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.NotificationType;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.domain.OrderStatus;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;
import za.ac.cput.prm_marketplace.domain.PaymentSimulationOutcome;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.exception.ConflictException;
import za.ac.cput.prm_marketplace.repository.OrderRepository;
import za.ac.cput.prm_marketplace.repository.OrderItemRepository;
import za.ac.cput.prm_marketplace.repository.PaymentRepository;
import za.ac.cput.prm_marketplace.repository.VendorProfileRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    private OrderItemRepository orderItemRepository;

    @Mock
    private VendorProfileRepository vendorProfileRepository;

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

    @Test
    @DisplayName("sandbox success is recorded only when simulation is enabled for the payment owner")
    void simulate_successCompletesOwnedSandboxPayment() {
        PaymentServiceImpl simulator = new PaymentServiceImpl(
                paymentRepository, orderRepository, orderItemRepository, vendorProfileRepository, notificationService);
        ReflectionTestUtils.setField(simulator, "simulationEnabled", true);
        Payment sandbox = new Payment.Builder()
                .copy(buildPayment(PaymentStatus.PENDING))
                .setMethod(PaymentMethod.SANDBOX)
                .build();
        when(paymentRepository.findByIdAndUserId(id, payerId)).thenReturn(Optional.of(sandbox));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = simulator.simulate(id, PaymentSimulationOutcome.SUCCESS, payerId);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getPaidAt()).isNotNull();
        verify(notificationService).send(eq(payerId), eq(NotificationType.PAYMENT),
                eq("Payment successful"), anyString());
    }

    @Test
    @DisplayName("sandbox failure stays a failed payment and does not set paidAt")
    void simulate_failureMarksPaymentFailed() {
        PaymentServiceImpl simulator = new PaymentServiceImpl(
                paymentRepository, orderRepository, orderItemRepository, vendorProfileRepository, notificationService);
        ReflectionTestUtils.setField(simulator, "simulationEnabled", true);
        Payment sandbox = new Payment.Builder()
                .copy(buildPayment(PaymentStatus.PENDING))
                .setMethod(PaymentMethod.SANDBOX)
                .build();
        when(paymentRepository.findByIdAndUserId(id, payerId)).thenReturn(Optional.of(sandbox));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = simulator.simulate(id, PaymentSimulationOutcome.FAILURE, payerId);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getPaidAt()).isNull();
        verify(notificationService).send(eq(payerId), eq(NotificationType.PAYMENT),
                eq("Payment failed"), anyString());
    }

    @Test
    @DisplayName("sandbox results are refused when disabled, for another account, or for a non-sandbox payment")
    void simulate_refusesDisabledForeignAndRealPayments() {
        Payment sandbox = new Payment.Builder()
                .copy(buildPayment(PaymentStatus.PENDING))
                .setMethod(PaymentMethod.SANDBOX)
                .build();
        PaymentServiceImpl disabled = new PaymentServiceImpl(
                paymentRepository, orderRepository, orderItemRepository, vendorProfileRepository, notificationService);
        PaymentServiceImpl enabled = new PaymentServiceImpl(
                paymentRepository, orderRepository, orderItemRepository, vendorProfileRepository, notificationService);
        ReflectionTestUtils.setField(enabled, "simulationEnabled", true);

        assertThat(disabled.simulate(id, PaymentSimulationOutcome.SUCCESS, payerId)).isNull();
        when(paymentRepository.findByIdAndUserId(id, intruderId)).thenReturn(Optional.empty());
        assertThat(enabled.simulate(id, PaymentSimulationOutcome.SUCCESS, intruderId)).isNull();

        Payment realMethod = new Payment.Builder().copy(sandbox).setMethod(PaymentMethod.CARD).build();
        when(paymentRepository.findByIdAndUserId(id, payerId)).thenReturn(Optional.of(realMethod));
        assertThat(enabled.simulate(id, PaymentSimulationOutcome.SUCCESS, payerId)).isNull();
        verify(paymentRepository, never()).save(any(Payment.class));
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

    @Test
    @DisplayName("manual payment belongs to one verified seller and uses the seller's exact order total")
    void createForSeller_assignsSellerAndServerCalculatedAmount() {
        User seller = new User.Builder().setId(intruderId).setName("Seller").build();
        VendorProfile profile = new VendorProfile.Builder()
                .setUser(seller)
                .setBusinessName("Seller Shop")
                .setVerified(true)
                .setPayoutDetails("Seller Name", "Bank", "12345678", "123456", "CURRENT")
                .build();
        Product product = new Product.Builder().id(UUID.randomUUID())
                .name("Book").price(new BigDecimal("25.00")).vendor(profile).build();
        OrderItem item = new OrderItem.Builder().setProduct(product)
                .setQuantity(2).setPriceAtPurchase(new BigDecimal("25.00")).build();
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "50.00")));
        when(vendorProfileRepository.findByUserId(intruderId)).thenReturn(Optional.of(profile));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.createForSeller(orderId, PaymentMethod.EFT, payerId, intruderId);

        assertThat(result.getAmount()).isEqualByComparingTo("50.00");
        assertThat(result.getSellerUserId()).isEqualTo(intruderId);
        assertThat(result.getUserId()).isEqualTo(payerId);
    }

    @Test
    @DisplayName("EFT checkout refuses a seller without payout details")
    void createForSeller_reportsMissingPayoutDetails() {
        VendorProfile profile = new VendorProfile.Builder()
                .setUser(new User.Builder().setId(intruderId).build())
                .setVerified(true)
                .build();
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "50.00")));
        when(vendorProfileRepository.findByUserId(intruderId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> paymentService.createForSeller(
                orderId, PaymentMethod.EFT, payerId, intruderId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Choose cash on pickup");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("only the assigned seller can confirm an EFT receipt")
    void confirmReceipt_completesAssignedSellerPayment() {
        Payment pending = new Payment.Builder().copy(buildPayment(PaymentStatus.PENDING))
                .setMethod(PaymentMethod.EFT).setSellerUserId(intruderId).build();
        when(paymentRepository.findById(id)).thenReturn(Optional.of(pending));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(
                new Order.Builder().setId(orderId).setStatus(OrderStatus.PENDING).build()));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.confirmReceipt(id, intruderId, Role.VENDOR);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getPaidAt()).isNotNull();
        assertThat(paymentService.confirmReceipt(id, payerId, Role.VENDOR)).isNull();
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
    @DisplayName("an omitted amount on a part paid order settles what is left, not the total again")
    void create_coversOnlyTheRemainingBalance() {
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "780.00")));
        givenCommittedPayments(committed(PaymentStatus.PENDING, "500.00"));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment request = new Payment.Builder()
                .setOrderId(orderId).setMethod(PaymentMethod.EFT).build();

        Payment saved = paymentService.create(request, payerId);

        assertThat(saved.getAmount())
                .as("paying the 780 total again would take R1260 for goods worth R780")
                .isEqualByComparingTo("280.00");
    }

    @Test
    @DisplayName("a second attempt cannot restate the whole total")
    void create_rejectsAnAmountAboveTheRemainingBalance() {
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "780.00")));
        givenCommittedPayments(committed(PaymentStatus.PENDING, "500.00"));

        Payment request = new Payment.Builder()
                .setOrderId(orderId)
                .setAmount(new BigDecimal("780.00"))
                .setMethod(PaymentMethod.EFT)
                .build();

        assertThat(paymentService.create(request, payerId)).isNull();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("an attempt that exactly clears the balance is accepted")
    void create_acceptsAnAmountEqualToTheRemainingBalance() {
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "780.00")));
        givenCommittedPayments(committed(PaymentStatus.PENDING, "500.00"));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment request = new Payment.Builder()
                .setOrderId(orderId)
                .setAmount(new BigDecimal("280.00"))
                .setMethod(PaymentMethod.EFT)
                .build();

        assertThat(paymentService.create(request, payerId)).isNotNull();
    }

    @Test
    @DisplayName("nothing more can be filed against a fully paid order")
    void create_rejectsAnythingOnceTheOrderIsSettled() {
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "780.00")));
        givenCommittedPayments(committed(PaymentStatus.COMPLETED, "780.00"));

        Payment withAmount = new Payment.Builder()
                .setOrderId(orderId)
                .setAmount(new BigDecimal("10.00"))
                .setMethod(PaymentMethod.EFT)
                .build();
        Payment withoutAmount = new Payment.Builder()
                .setOrderId(orderId).setMethod(PaymentMethod.EFT).build();

        assertThat(paymentService.create(withAmount, payerId)).isNull();
        assertThat(paymentService.create(withoutAmount, payerId)).isNull();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("a refunded payment no longer counts towards what the order owes")
    void create_ignoresARefundedPaymentWhenWorkingOutTheBalance() {
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "780.00")));
        givenCommittedPayments(committed(PaymentStatus.REFUNDED, "780.00"));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment request = new Payment.Builder()
                .setOrderId(orderId).setMethod(PaymentMethod.EFT).build();

        assertThat(paymentService.create(request, payerId).getAmount())
                .as("the money came back, so the order is owed the full amount again")
                .isEqualByComparingTo("780.00");
    }

    @Test
    @DisplayName("a failed attempt does not block paying again")
    void create_ignoresAFailedPaymentWhenWorkingOutTheBalance() {
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "780.00")));
        givenCommittedPayments(committed(PaymentStatus.FAILED, "780.00"));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment request = new Payment.Builder()
                .setOrderId(orderId).setMethod(PaymentMethod.EFT).build();

        assertThat(paymentService.create(request, payerId).getAmount())
                .isEqualByComparingTo("780.00");
    }

    @Test
    @DisplayName("only payments that still claim the money are asked about")
    void create_asksOnlyForTheStatusesThatOweSomething() {
        when(orderRepository.findByIdAndBuyerId(orderId, payerId))
                .thenReturn(Optional.of(orderOwnedBy(payerId, "780.00")));

        paymentService.create(new Payment.Builder()
                .setOrderId(orderId).setMethod(PaymentMethod.EFT).build(), payerId);

        verify(paymentRepository).findByOrderIdAndStatusIn(orderId,
                List.of(PaymentStatus.PENDING, PaymentStatus.COMPLETED));
    }

    /**
     * Answers the committed-payments lookup the way the database does, by filtering the rows it was
     * given down to the statuses the caller asked for. Returning them unfiltered would quietly assert
     * that a refunded payment still counts towards the balance, which is the opposite of the intent.
     */
    private void givenCommittedPayments(Payment... payments) {
        when(paymentRepository.findByOrderIdAndStatusIn(eq(orderId), anyList()))
                .thenAnswer(invocation -> {
                    List<PaymentStatus> wanted = invocation.getArgument(1);
                    return List.of(payments).stream()
                            .filter(payment -> wanted.contains(payment.getStatus()))
                            .toList();
                });
    }

    private Payment committed(PaymentStatus status, String amount) {
        return new Payment.Builder()
                .setId(UUID.randomUUID())
                .setOrderId(orderId)
                .setUserId(payerId)
                .setAmount(new BigDecimal(amount))
                .setMethod(PaymentMethod.CARD)
                .setStatus(status)
                .setTransactionReference("PAY-" + UUID.randomUUID())
                .setCreatedAt(LocalDateTime.now())
                .build();
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

        Payment updated = paymentService.updateStatus(id, PaymentStatus.COMPLETED, payerId, Role.ADMIN);

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(updated.getPaidAt()).isNotNull();
        verify(notificationService).send(eq(payerId), eq(NotificationType.PAYMENT),
                anyString(), anyString());
    }

    @Test
    @DisplayName("the payer cannot mark their own payment as paid")
    void updateStatus_completionRequiresAdmin() {
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
    void updateStatus_failureRequiresAdmin() {
        // Otherwise a buyer could erase a completed charge they regret.
        assertThat(paymentService.updateStatus(id, PaymentStatus.FAILED, payerId, Role.VENDOR))
                .isNull();

        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("no role other than admin can settle a payment")
    void updateStatus_everyNonAdminRoleIsRefused() {
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
    void updateStatus_refundRequiresAdmin() {
        assertThat(paymentService.updateStatus(id, PaymentStatus.REFUNDED, payerId, Role.STUDENT)).isNull();

        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("admin may refund")
    void updateStatus_adminMayRefund() {
        when(paymentRepository.findByIdAndUserId(id, payerId))
                .thenReturn(Optional.of(buildPayment(PaymentStatus.COMPLETED)));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment updated = paymentService.updateStatus(id, PaymentStatus.REFUNDED, payerId, Role.ADMIN);

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("even admin cannot move somebody else's payment")
    void updateStatus_rejectsSomebodyElsesPayment() {
        when(paymentRepository.findByIdAndUserId(id, intruderId)).thenReturn(Optional.empty());

        // Admin has authority over settling, not over whose payment it is. Ownership is still
        // scoped to the caller, so this stays refused at full privilege.
        assertThat(paymentService.updateStatus(id, PaymentStatus.COMPLETED, intruderId, Role.ADMIN))
                .isNull();

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("a transition the lifecycle does not allow is refused")
    void updateStatus_rejectsAnIllegalTransition() {
        when(paymentRepository.findByIdAndUserId(id, payerId))
                .thenReturn(Optional.of(buildPayment(PaymentStatus.PENDING)));

        // PENDING cannot go straight to REFUNDED.
        assertThat(paymentService.updateStatus(id, PaymentStatus.REFUNDED, payerId, Role.ADMIN))
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
    @DisplayName("a null role is not admin, so settling is refused without a database read")
    void updateStatus_nullRoleCannotSettle() {
        // Worth pinning down because `status != PENDING && role != ADMIN` would otherwise be read
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

        Payment updated = paymentService.updateStatus(id, PaymentStatus.COMPLETED, payerId, Role.ADMIN);

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRepository).save(any(Payment.class));
    }
}