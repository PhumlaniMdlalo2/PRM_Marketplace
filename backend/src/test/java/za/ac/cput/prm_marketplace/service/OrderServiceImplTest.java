package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderStatus;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

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

    @Test
    @DisplayName("create persists the order")
    void create_saves() {
        Order order = new Order.Builder().setId(UUID.randomUUID()).setBuyer(buildBuyer()).build();
        when(orderRepository.save(order)).thenReturn(order);

        assertThat(orderService.create(order)).isSameAs(order);
    }

    @Test
    @DisplayName("create with null returns null")
    void create_withNull_returnsNull() {
        assertThat(orderService.create(null)).isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("read missing order returns null")
    void read_missing_returnsNull() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(orderService.read(id)).isNull();
    }

    @Test
    @DisplayName("update requires an existing order")
    void update_missing_returnsNull() {
        Order order = new Order.Builder().setId(UUID.randomUUID()).setBuyer(buildBuyer()).build();
        when(orderRepository.existsById(order.getId())).thenReturn(false);

        assertThat(orderService.update(order)).isNull();
    }

    @Test
    @DisplayName("delete reports false for an unknown order")
    void delete_missing_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(orderRepository.existsById(id)).thenReturn(false);

        assertThat(orderService.delete(id)).isFalse();
    }

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
    @DisplayName("checkout converts the cart into a pending order with line items")
    void checkout_buildsPendingOrder() {
        Product product = buildProduct(new BigDecimal("100.00"));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.checkout(
                buildBuyer(),
                List.of(buildCartItem(product, 2), buildCartItem(buildProduct(new BigDecimal("5.00")), 1)),
                null);

        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("205.00");
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getItems())
                .allSatisfy(item -> assertThat(item.getOrder()).isSameAs(order));
        assertThat(order.getItems())
                .filteredOn(item -> item.getProduct().equals(product))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getQuantity()).isEqualTo(2);
                    assertThat(item.getPriceAtPurchase()).isEqualByComparingTo("100.00");
                });
    }

    @Test
    @DisplayName("checkout needs a buyer and a non-empty cart")
    void checkout_requiresBuyerAndItems() {
        assertThat(orderService.checkout(null, List.of(buildCartItem(buildProduct(BigDecimal.ONE), 1)), null))
                .isNull();
        assertThat(orderService.checkout(buildBuyer(), List.of(), null)).isNull();
        assertThat(orderService.checkout(buildBuyer(), null, null)).isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkout fails when no cart item carries a product")
    void checkout_withoutProducts_returnsNull() {
        CartItem noProduct = new CartItem.Builder()
                .id(UUID.randomUUID())
                .user(buildBuyer())
                .quantity(1)
                .build();

        assertThat(orderService.checkout(buildBuyer(), List.of(noProduct), null)).isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus changes the status of an existing order")
    void updateStatus_existing_persists() {
        Order order = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buildBuyer())
                .setStatus(OrderStatus.PENDING)
                .build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order updated = orderService.updateStatus(order.getId(), OrderStatus.SHIPPED);

        assertThat(updated).isSameAs(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("updateStatus rejects null arguments and unknown orders")
    void updateStatus_rejectsInvalidInput() {
        UUID id = UUID.randomUUID();
        assertThat(orderService.updateStatus(null, OrderStatus.SHIPPED)).isNull();
        assertThat(orderService.updateStatus(id, null)).isNull();

        when(orderRepository.findById(id)).thenReturn(Optional.empty());
        assertThat(orderService.updateStatus(id, OrderStatus.SHIPPED)).isNull();
    }

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
}
