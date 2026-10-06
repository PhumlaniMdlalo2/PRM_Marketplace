package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.domain.NotificationType;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderStatus;
import za.ac.cput.prm_marketplace.domain.Payment;
import za.ac.cput.prm_marketplace.domain.PaymentMethod;
import za.ac.cput.prm_marketplace.domain.PaymentStatus;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.repository.AddressRepository;
import za.ac.cput.prm_marketplace.repository.CartItemRepository;
import za.ac.cput.prm_marketplace.repository.OrderRepository;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.OrderItemRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The interesting cases here are the ones where a caller asks for something they should not get,
 * and the ones where two buyers want the last item at the same time.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private IPaymentService paymentService;

    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User buildBuyer() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Buyer")
                .setEmail("buyer@example.com")
                .setPasswordHash("hash")
                .build();
    }

    private User buildUser(String name, Role role) {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName(name)
                .setEmail(name + "-" + UUID.randomUUID() + "@example.com")
                .setPasswordHash("hash")
                .setRole(role)
                .build();
    }

    /** A listing with a seller on it, which is what checkout needs to know whom to tell. */
    private Product buildSoldProduct(BigDecimal price, User seller) {
        return new Product.Builder()
                .id(UUID.randomUUID())
                .name("Textbook")
                .price(price)
                .vendor(new VendorProfile.Builder()
                        .setId(UUID.randomUUID())
                        .setUser(seller)
                        .setBusinessName(seller.getName() + " Books")
                        .build())
                .build();
    }

    private Product buildProduct(BigDecimal price) {
        return new Product.Builder()
                .id(UUID.randomUUID())
                .name("Textbook")
                .price(price)
                .build();
    }

    private CartItem buildCartItem(Product product, int quantity) {
        return new CartItem.Builder()
                .id(UUID.randomUUID())
                .user(buildBuyer())
                .product(product)
                .quantity(quantity)
                .build();
    }

    /**
     * Lets the payment filing succeed for a checkout that has otherwise been arranged to work.
     * Stubbed rather than real so the order assertions do not depend on payment internals.
     */
    private void givenAPaymentIsAccepted() {
        when(paymentService.create(any(Payment.class), any(UUID.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------- read / update / delete scoping ----------

    @Test
    @DisplayName("read only matches an order the caller actually owns")
    void read_looksUpByBuyer() {
        UUID id = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        Order owned = new Order.Builder().setId(id).setBuyer(buildBuyer()).build();
        when(orderRepository.findByIdAndBuyerId(id, callerId)).thenReturn(Optional.of(owned));

        assertThat(orderService.read(id, callerId)).isSameAs(owned);
    }

    @Test
    @DisplayName("read returns null for an order belonging to somebody else")
    void read_otherUsersOrder_returnsNull() {
        UUID id = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        // The buyer-scoped lookup finds nothing, which is the whole point of using it.
        when(orderRepository.findByIdAndBuyerId(id, callerId)).thenReturn(Optional.empty());

        assertThat(orderService.read(id, callerId)).isNull();
    }

    @Test
    @DisplayName("read rejects null arguments")
    void read_rejectsNulls() {
        assertThat(orderService.read(null, UUID.randomUUID())).isNull();
        assertThat(orderService.read(UUID.randomUUID(), null)).isNull();
    }

    @Test
    @DisplayName("update refuses an order the caller does not own")
    void update_otherUsersOrder_returnsNull() {
        UUID callerId = UUID.randomUUID();
        Order incoming = new Order.Builder().setId(UUID.randomUUID()).setBuyer(buildBuyer()).build();
        when(orderRepository.findByIdAndBuyerId(incoming.getId(), callerId)).thenReturn(Optional.empty());

        assertThat(orderService.update(incoming, callerId)).isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("update leaves status and total alone")
    void update_doesNotLetTheBodyRewriteServerOwnedFields() {
        UUID callerId = UUID.randomUUID();
        User buyer = buildBuyer();
        Order existing = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buyer)
                .setStatus(OrderStatus.PENDING)
                .setTotalAmount(new BigDecimal("250.00"))
                .build();
        when(orderRepository.findByIdAndBuyerId(existing.getId(), callerId)).thenReturn(Optional.of(existing));
        when(orderRepository.save(existing)).thenReturn(existing);

        Order body = new Order.Builder()
                .setId(existing.getId())
                .setStatus(OrderStatus.SHIPPED)
                .setTotalAmount(new BigDecimal("0.01"))
                .build();

        Order updated = orderService.update(body, callerId);

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(updated.getTotalAmount()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("update refuses a shipping address belonging to somebody else")
    void update_rejectsForeignShippingAddress() {
        UUID callerId = UUID.randomUUID();
        Order existing = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buildBuyer())
                .setStatus(OrderStatus.PENDING)
                .build();
        UUID foreignAddressId = UUID.randomUUID();
        when(orderRepository.findByIdAndBuyerId(existing.getId(), callerId)).thenReturn(Optional.of(existing));
        // The address exists, but it is registered to another account.
        when(addressRepository.findById(foreignAddressId))
                .thenReturn(Optional.of(new Address.Builder().setId(foreignAddressId).setUser(buildBuyer()).build()));

        Order body = new Order.Builder()
                .setId(existing.getId())
                .setShippingAddress(new Address.Builder().setId(foreignAddressId).build())
                .build();

        assertThat(orderService.update(body, callerId)).isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("update is refused once the order has moved past PENDING")
    void update_refusedAfterProgress() {
        UUID callerId = UUID.randomUUID();
        Order existing = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buildBuyer())
                .setStatus(OrderStatus.SHIPPED)
                .build();
        when(orderRepository.findByIdAndBuyerId(existing.getId(), callerId)).thenReturn(Optional.of(existing));

        Order body = new Order.Builder().setId(existing.getId()).build();

        assertThat(orderService.update(body, callerId)).isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete reports false for an order the caller does not own")
    void delete_otherUsersOrder_returnsFalse() {
        UUID id = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        when(orderRepository.findByIdAndBuyerId(id, callerId)).thenReturn(Optional.empty());

        assertThat(orderService.delete(id, callerId)).isFalse();
        verify(orderRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("getAll is scoped to the caller rather than returning every row")
    void getAll_scopesToCaller() {
        UUID callerId = UUID.randomUUID();
        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(callerId)).thenReturn(List.of());

        orderService.getAll(callerId);

        verify(orderRepository).findByBuyerIdOrderByCreatedAtDesc(callerId);
        verify(orderRepository, never()).findAll();
    }

    // ---------- status transitions ----------

    @Test
    @DisplayName("updateStatus changes the status for a vendor selling on the order")
    void updateStatus_existing_persists() {
        UUID vendorId = UUID.randomUUID();
        Order order = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buildBuyer())
                .setStatus(OrderStatus.PENDING)
                .build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderItemRepository.existsByOrderIdAndVendorUserId(order.getId(), vendorId))
                .thenReturn(true);
        when(orderRepository.save(order)).thenReturn(order);

        Order updated = orderService.updateStatus(order.getId(), OrderStatus.CONFIRMED, vendorId, Role.VENDOR);

        assertThat(updated).isSameAs(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("a vendor cannot advance an order that holds none of their products")
    void updateStatus_refusedForUnrelatedVendor() {
        UUID sellerId = UUID.randomUUID();
        UUID intruderId = UUID.randomUUID();
        Order order = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buildBuyer())
                .setStatus(OrderStatus.PENDING)
                .build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderItemRepository.existsByOrderIdAndVendorUserId(order.getId(), intruderId))
                .thenReturn(false);

        assertThat(orderService.updateStatus(order.getId(), OrderStatus.CONFIRMED, intruderId, Role.VENDOR))
                .isNull();
        // Ownership is refused before the transition is even considered, so nothing is written.
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("faculty may advance any order without selling anything on it")
    void updateStatus_facultyNeedsNoProductOwnership() {
        UUID facultyId = UUID.randomUUID();
        Order order = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buildBuyer())
                .setStatus(OrderStatus.PENDING)
                .build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order updated = orderService.updateStatus(order.getId(), OrderStatus.CONFIRMED, facultyId, Role.FACULTY);

        assertThat(updated).isSameAs(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderItemRepository, never()).existsByOrderIdAndVendorUserId(any(), any());
    }

    @Test
    @DisplayName("a cancelled order cannot be reopened to delivered")
    void updateStatus_refusesReopeningATerminalOrder() {
        UUID facultyId = UUID.randomUUID();
        Order order = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buildBuyer())
                .setStatus(OrderStatus.CANCELLED)
                .build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThat(orderService.updateStatus(order.getId(), OrderStatus.DELIVERED, facultyId, Role.FACULTY))
                .isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("an order cannot skip straight from pending to delivered")
    void updateStatus_refusesSkippingTheLifecycle() {
        UUID facultyId = UUID.randomUUID();
        Order order = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buildBuyer())
                .setStatus(OrderStatus.PENDING)
                .build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThat(orderService.updateStatus(order.getId(), OrderStatus.DELIVERED, facultyId, Role.FACULTY))
                .isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("a delivered order cannot be moved back to pending")
    void updateStatus_refusesGoingBackwards() {
        UUID facultyId = UUID.randomUUID();
        Order order = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buildBuyer())
                .setStatus(OrderStatus.DELIVERED)
                .build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThat(orderService.updateStatus(order.getId(), OrderStatus.PENDING, facultyId, Role.FACULTY))
                .isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("a plain student cannot advance an order's status")
    void updateStatus_refusedForStudent() {
        UUID id = UUID.randomUUID();

        assertThat(orderService.updateStatus(id, OrderStatus.SHIPPED, UUID.randomUUID(), Role.STUDENT)).isNull();
        // The role check happens before any lookup, so a student never even reaches the order.
        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus rejects null arguments and unknown orders")
    void updateStatus_rejectsInvalidInput() {
        UUID id = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        assertThat(orderService.updateStatus(null, OrderStatus.SHIPPED, callerId, Role.VENDOR)).isNull();
        assertThat(orderService.updateStatus(id, null, callerId, Role.VENDOR)).isNull();
        assertThat(orderService.updateStatus(id, OrderStatus.SHIPPED, null, Role.VENDOR)).isNull();

        when(orderRepository.findById(id)).thenReturn(Optional.empty());
        assertThat(orderService.updateStatus(id, OrderStatus.SHIPPED, callerId, Role.VENDOR)).isNull();
    }

    // ---------- checkout ----------

    @Test
    @DisplayName("checkout prices the order from the catalogue, not the cart")
    void checkout_pricesFromCatalog() {
        User buyer = buildBuyer();
        Product product = buildProduct(new BigDecimal("100.00"));
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUser_Id(buyer.getId()))
                .thenReturn(List.of(buildCartItem(product, 2)));
        when(productRepository.decrementStock(product.getId(), 2)).thenReturn(1);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        givenAPaymentIsAccepted();

        Order order = orderService.checkout(buyer.getId(), null, null);

        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("200.00");
    }

    // ---------- notifications ----------

    @Test
    @DisplayName("checkout tells the seller that one of their listings was bought")
    void checkout_notifiesTheSeller() {
        User buyer = buildBuyer();
        User seller = buildUser("Seller", Role.VENDOR);
        Product product = buildSoldProduct(new BigDecimal("100.00"), seller);
        givenACartWith(buyer, product);
        givenAPaymentIsAccepted();

        orderService.checkout(buyer.getId(), null, null);

        verify(notificationService).send(eq(seller.getId()), eq(NotificationType.ORDER),
                eq("New order received"), argThat(message -> message.contains("100.00")));
        verify(notificationService, never()).send(eq(buyer.getId()), eq(NotificationType.ORDER),
                anyString(), anyString());
    }

    @Test
    @DisplayName("each seller is told once, however many of their lines the order holds")
    void checkout_notifiesEachSellerOnceForTheirOwnLines() {
        User buyer = buildBuyer();
        User first = buildUser("First", Role.VENDOR);
        User second = buildUser("Second", Role.VENDOR);
        Product textbook = buildSoldProduct(new BigDecimal("100.00"), first);
        Product laptop = buildSoldProduct(new BigDecimal("50.00"), second);
        Product spareTextbook = buildSoldProduct(new BigDecimal("100.00"), first);
        givenACartWith(buyer, textbook, spareTextbook, laptop);
        givenAPaymentIsAccepted();

        Order order = orderService.checkout(buyer.getId(), null, null);

        assertThat(order.getTotalAmount()).isEqualByComparingTo("250.00");
        assertThat(titlesSentTo(first.getId())).hasSize(1);
        assertThat(titlesSentTo(second.getId())).hasSize(1);
    }

    @Test
    @DisplayName("buying your own listing does not notify you about your own order")
    void checkout_doesNotNotifyTheBuyerAsTheirOwnSeller() {
        User buyer = buildBuyer();
        givenACartWith(buyer, buildSoldProduct(new BigDecimal("100.00"), buyer));
        givenAPaymentIsAccepted();

        Order order = orderService.checkout(buyer.getId(), null, null);

        assertThat(order).isNotNull();
        verify(notificationService, never()).send(any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("a notification that fails does not lose the order")
    void checkout_survivesANotificationFailure() {
        User buyer = buildBuyer();
        Product product = buildSoldProduct(new BigDecimal("100.00"), buildUser("Seller", Role.VENDOR));
        givenACartWith(buyer, product);
        givenAPaymentIsAccepted();
        when(notificationService.send(any(), any(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("notification table unavailable"));

        Order order = orderService.checkout(buyer.getId(), null, null);

        assertThat(order).isNotNull();
        assertThat(order.getTotalAmount()).isEqualByComparingTo("100.00");
        verify(cartItemRepository).deleteAll(any());
    }

    @Test
    @DisplayName("no order is written, so nobody is notified, when checkout is refused")
    void checkout_notifiesNobodyWhenItRefuses() {
        User buyer = buildBuyer();
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUser_Id(buyer.getId())).thenReturn(List.of());

        assertThat(orderService.checkout(buyer.getId(), null, null)).isNull();

        verify(notificationService, never()).send(any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("the buyer is told when faculty advances their order")
    void updateStatus_notifiesTheBuyer() {
        User buyer = buildBuyer();
        Order order = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buyer)
                .setStatus(OrderStatus.PENDING)
                .setTotalAmount(new BigDecimal("100.00"))
                .build();
        givenFacultyMayUpdate(order);

        orderService.updateStatus(order.getId(), OrderStatus.CONFIRMED, buyer.getId(), Role.FACULTY);

        verify(notificationService).send(eq(buyer.getId()), eq(NotificationType.ORDER),
                eq("Order confirmed"), argThat(message -> message.contains("100.00")));
    }

    @Test
    @DisplayName("a cancellation reads as a cancellation rather than a status name")
    void updateStatus_titlesACancellationPlainly() {
        User buyer = buildBuyer();
        Order order = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buyer)
                .setStatus(OrderStatus.CONFIRMED)
                .build();
        givenFacultyMayUpdate(order);

        orderService.updateStatus(order.getId(), OrderStatus.CANCELLED, buyer.getId(), Role.FACULTY);

        verify(notificationService).send(eq(buyer.getId()), eq(NotificationType.ORDER),
                eq("Order cancelled"), argThat(message -> message.contains("cancelled")));
    }

    @Test
    @DisplayName("a refused status change notifies nobody")
    void updateStatus_notifiesNobodyWhenRefused() {
        UUID intruderId = UUID.randomUUID();
        Order order = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buildBuyer())
                .setStatus(OrderStatus.PENDING)
                .build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        // The order holds none of this vendor's products, so the change is refused after the lookup.
        when(orderItemRepository.existsByOrderIdAndVendorUserId(order.getId(), intruderId))
                .thenReturn(false);

        assertThat(orderService.updateStatus(order.getId(), OrderStatus.CONFIRMED,
                intruderId, Role.VENDOR)).isNull();

        verify(notificationService, never()).send(any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("a notification that fails does not undo the status change")
    void updateStatus_survivesANotificationFailure() {
        User buyer = buildBuyer();
        Order order = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buyer)
                .setStatus(OrderStatus.PENDING)
                .build();
        givenFacultyMayUpdate(order);
        when(notificationService.send(any(), any(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("notification table unavailable"));

        Order updated = orderService.updateStatus(order.getId(), OrderStatus.CONFIRMED,
                buyer.getId(), Role.FACULTY);

        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
    }

    /** Faculty may act on any order, so the ownership check is the only gate left to arrange. */
    private void givenFacultyMayUpdate(Order order) {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenACartWith(User buyer, Product... products) {
        CartItem[] lines = new CartItem[products.length];
        for (int i = 0; i < products.length; i++) {
            lines[i] = buildCartItem(products[i], 1);
            when(productRepository.decrementStock(products[i].getId(), 1)).thenReturn(1);
            when(productRepository.findById(products[i].getId())).thenReturn(Optional.of(products[i]));
        }
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUser_Id(buyer.getId())).thenReturn(List.of(lines));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** The notification titles one particular recipient was sent, read back off the recorded calls. */
    private List<String> titlesSentTo(UUID userId) {
        ArgumentCaptor<UUID> recipients = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> titles = ArgumentCaptor.forClass(String.class);
        verify(notificationService, atLeastOnce())
                .send(recipients.capture(), any(NotificationType.class), titles.capture(), anyString());

        List<String> sent = new ArrayList<>();
        for (int i = 0; i < recipients.getAllValues().size(); i++) {
            if (userId.equals(recipients.getAllValues().get(i))) {
                sent.add(titles.getAllValues().get(i));
            }
        }
        return sent;
    }

    @Test
    @DisplayName("checkout refuses to oversell: a failed reservation aborts the order")
    void checkout_refusesWhenStockIsInsufficient() {
        User buyer = buildBuyer();
        Product product = buildProduct(new BigDecimal("100.00"));
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUser_Id(buyer.getId()))
                .thenReturn(List.of(buildCartItem(product, 5)));
        // Zero rows means somebody else took the units first.
        when(productRepository.decrementStock(product.getId(), 5)).thenReturn(0);

        assertThat(orderService.checkout(buyer.getId(), null, null)).isNull();
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("checkout ignores cart rows belonging to other users")
    void checkout_usesOnlyTheCallersCart() {
        User buyer = buildBuyer();
        Product product = buildProduct(new BigDecimal("10.00"));
        UUID strangerId = UUID.randomUUID();
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        // The caller's own cart is empty. Another user's cart exists but must never be consulted.
        when(cartItemRepository.findByUser_Id(buyer.getId())).thenReturn(List.of());

        assertThat(orderService.checkout(buyer.getId(), null, null)).isNull();
        verify(cartItemRepository, never()).findByUser_Id(strangerId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkout refuses a shipping address owned by somebody else")
    void checkout_rejectsForeignShippingAddress() {
        User buyer = buildBuyer();
        Product product = buildProduct(new BigDecimal("10.00"));
        UUID foreignAddressId = UUID.randomUUID();
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUser_Id(buyer.getId()))
                .thenReturn(List.of(buildCartItem(product, 1)));
        when(addressRepository.findById(foreignAddressId))
                .thenReturn(Optional.of(new Address.Builder()
                        .setId(foreignAddressId).setUser(buildBuyer()).build()));

        assertThat(orderService.checkout(buyer.getId(), foreignAddressId, null)).isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkout needs a known buyer and a non-empty cart")
    void checkout_requiresBuyerAndItems() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());
        assertThat(orderService.checkout(unknown, null, null)).isNull();
        assertThat(orderService.checkout(null, null, null)).isNull();

        User buyer = buildBuyer();
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUser_Id(buyer.getId())).thenReturn(List.of());
        assertThat(orderService.checkout(buyer.getId(), null, null)).isNull();

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkout only empties the cart lines it actually ordered")
    void checkout_clearsOnlyPurchasedLines() {
        User buyer = buildBuyer();
        Product product = buildProduct(new BigDecimal("25.00"));
        CartItem purchased = buildCartItem(product, 1);
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUser_Id(buyer.getId()))
                .thenReturn(List.of(purchased, buildCartItem(null, 1)));
        when(productRepository.decrementStock(product.getId(), 1)).thenReturn(1);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        givenAPaymentIsAccepted();

        orderService.checkout(buyer.getId(), null, null);

        verify(cartItemRepository).deleteAll(List.of(purchased));
    }

    // ---------- cancel ----------

    @Test
    @DisplayName("cancel is allowed while an order is pending or confirmed")
    void cancel_allowedStatuses() {
        for (OrderStatus status : List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED)) {
            UUID id = UUID.randomUUID();
            UUID buyerId = UUID.randomUUID();
            Order order = new Order.Builder()
                    .setId(id)
                    .setBuyer(buildBuyer())
                    .setStatus(status)
                    .build();
            when(orderRepository.findByIdAndBuyerId(id, buyerId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            assertThat(orderService.cancel(id, buyerId)).as("status %s", status).isTrue();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }

    @Test
    @DisplayName("cancelling puts the reserved stock back")
    void cancel_restocks() {
        UUID id = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        Product product = buildProduct(new BigDecimal("10.00"));
        Order order = new Order.Builder()
                .setId(id)
                .setBuyer(buildBuyer())
                .setStatus(OrderStatus.PENDING)
                .build();
        order.addItem(new za.ac.cput.prm_marketplace.domain.OrderItem.Builder()
                .setOrder(order)
                .setProduct(product)
                .setQuantity(3)
                .setPriceAtPurchase(new BigDecimal("10.00"))
                .build());
        when(orderRepository.findByIdAndBuyerId(id, buyerId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        assertThat(orderService.cancel(id, buyerId)).isTrue();

        verify(productRepository).incrementStock(product.getId(), 3);
    }

    @Test
    @DisplayName("cancel is refused once the order has shipped or completed")
    void cancel_refusedAfterDispatch() {
        for (OrderStatus status : List.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.CANCELLED)) {
            UUID id = UUID.randomUUID();
            UUID buyerId = UUID.randomUUID();
            Order order = new Order.Builder()
                    .setId(id)
                    .setBuyer(buildBuyer())
                    .setStatus(status)
                    .build();
            when(orderRepository.findByIdAndBuyerId(id, buyerId)).thenReturn(Optional.of(order));

            assertThat(orderService.cancel(id, buyerId)).as("status %s", status).isFalse();
            assertThat(order.getStatus()).isEqualTo(status);
            verify(productRepository, never()).incrementStock(any(), anyInt());
        }
    }

    @Test
    @DisplayName("cancel only matches orders owned by the given buyer")
    void cancel_wrongBuyer_returnsFalse() {
        UUID id = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        when(orderRepository.findByIdAndBuyerId(id, buyerId)).thenReturn(Optional.empty());

        assertThat(orderService.cancel(id, buyerId)).isFalse();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancel rejects null arguments")
    void cancel_rejectsNulls() {
        assertThat(orderService.cancel(null, UUID.randomUUID())).isFalse();
        assertThat(orderService.cancel(UUID.randomUUID(), null)).isFalse();
    }

    // ---------- totals ----------

    @Test
    @DisplayName("getByBuyer with null id returns empty")
    void getByBuyer_withNull_returnsEmpty() {
        assertThat(orderService.getByBuyer(null)).isEmpty();
    }

    @Test
    @DisplayName("getByBuyerAndStatus requires both a buyer and a status")
    void getByBuyerAndStatus_requiresBothArguments() {
        assertThat(orderService.getByBuyerAndStatus(null, OrderStatus.PENDING)).isEmpty();
        assertThat(orderService.getByBuyerAndStatus(UUID.randomUUID(), null)).isEmpty();
    }

    @Test
    @DisplayName("calculateTotal multiplies price by quantity across the cart")
    void calculateTotal_sumsLineItems() {
        BigDecimal total = orderService.calculateTotal(List.of(
                buildCartItem(buildProduct(new BigDecimal("100.00")), 2),
                buildCartItem(buildProduct(new BigDecimal("15.50")), 3)
        ));

        assertThat(total).isEqualByComparingTo("246.50");
    }

    @Test
    @DisplayName("calculateTotal returns zero for an empty or null cart")
    void calculateTotal_emptyCart_isZero() {
        assertThat(orderService.calculateTotal(null)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(orderService.calculateTotal(List.of())).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("calculateTotal skips items with a missing product or price")
    void calculateTotal_skipsIncompleteItems() {
        CartItem noProduct = new CartItem.Builder()
                .id(UUID.randomUUID())
                .user(buildBuyer())
                .quantity(2)
                .build();
        CartItem noPrice = buildCartItem(buildProduct(null), 1);

        BigDecimal total = orderService.calculateTotal(List.of(
                noProduct,
                noPrice,
                buildCartItem(buildProduct(new BigDecimal("20.00")), 1)
        ));

        assertThat(total).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("a saved order keeps the buyer the service resolved, not the request")
    void checkout_setsBuyerFromRepository() {
        User buyer = buildBuyer();
        Product product = buildProduct(new BigDecimal("5.00"));
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUser_Id(buyer.getId()))
                .thenReturn(List.of(buildCartItem(product, 1)));
        when(productRepository.decrementStock(product.getId(), 1)).thenReturn(1);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        givenAPaymentIsAccepted();

        Order order = orderService.checkout(buyer.getId(), null, null);

        assertThat(order.getBuyer().getId()).isEqualTo(buyer.getId());
        verify(orderRepository).save(eq(order));
    }

    // ---------- payment filed at checkout ----------

    @Test
    @DisplayName("checkout files a payment against the order for the buyer")
    void checkout_filesAPayment() {
        User buyer = buildBuyer();
        Product product = buildProduct(new BigDecimal("40.00"));
        UUID orderId = UUID.randomUUID();
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUser_Id(buyer.getId()))
                .thenReturn(List.of(buildCartItem(product, 2)));
        when(productRepository.decrementStock(product.getId(), 2)).thenReturn(1);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        // Hibernate assigns the id during save(), so the stub has to as well for the payment to
        // have an order to point at.
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation ->
                new Order.Builder().copy(invocation.getArgument(0)).setId(orderId).build());
        givenAPaymentIsAccepted();

        orderService.checkout(buyer.getId(), null, PaymentMethod.EFT);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentService).create(captor.capture(), eq(buyer.getId()));
        Payment filed = captor.getValue();
        assertThat(filed.getOrderId()).isEqualTo(orderId);
        assertThat(filed.getMethod()).isEqualTo(PaymentMethod.EFT);
    }

    @Test
    @DisplayName("checkout falls back to CARD when no payment method is named")
    void checkout_defaultsToCard() {
        User buyer = buildBuyer();
        Product product = buildProduct(new BigDecimal("15.00"));
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUser_Id(buyer.getId()))
                .thenReturn(List.of(buildCartItem(product, 1)));
        when(productRepository.decrementStock(product.getId(), 1)).thenReturn(1);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        givenAPaymentIsAccepted();

        orderService.checkout(buyer.getId(), null, null);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentService).create(captor.capture(), eq(buyer.getId()));
        assertThat(captor.getValue().getMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    @DisplayName("the amount on the filed payment is the order total, never client-supplied")
    void checkout_leavesTheAmountToTheServer() {
        User buyer = buildBuyer();
        Product product = buildProduct(new BigDecimal("12.50"));
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUser_Id(buyer.getId()))
                .thenReturn(List.of(buildCartItem(product, 2)));
        when(productRepository.decrementStock(product.getId(), 2)).thenReturn(1);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        givenAPaymentIsAccepted();

        orderService.checkout(buyer.getId(), null, PaymentMethod.CARD);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentService).create(captor.capture(), eq(buyer.getId()));
        // Null is the point: IPaymentService resolves it to the order total, so the client has no
        // way to state what it is paying.
        assertThat(captor.getValue().getAmount()).isNull();
        assertThat(captor.getValue().getStatus()).isNull();
    }

    @Test
    @DisplayName("checkout aborts when the payment cannot be filed")
    void checkout_abortsWhenPaymentFails() {
        User buyer = buildBuyer();
        Product product = buildProduct(new BigDecimal("9.00"));
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUser_Id(buyer.getId()))
                .thenReturn(List.of(buildCartItem(product, 1)));
        when(productRepository.decrementStock(product.getId(), 1)).thenReturn(1);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentService.create(any(Payment.class), any(UUID.class))).thenReturn(null);

        assertThat(orderService.checkout(buyer.getId(), null, PaymentMethod.CARD)).isNull();
        // The cart must survive a checkout that did not complete.
        verify(cartItemRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("a payment is never filed for a checkout that was refused earlier")
    void checkout_filesNoPaymentWhenRefused() {
        User buyer = buildBuyer();
        Product product = buildProduct(new BigDecimal("100.00"));
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(cartItemRepository.findByUser_Id(buyer.getId()))
                .thenReturn(List.of(buildCartItem(product, 5)));
        when(productRepository.decrementStock(product.getId(), 5)).thenReturn(0);

        assertThat(orderService.checkout(buyer.getId(), null, PaymentMethod.CARD)).isNull();
        verify(paymentService, never()).create(any(), any());
    }
}