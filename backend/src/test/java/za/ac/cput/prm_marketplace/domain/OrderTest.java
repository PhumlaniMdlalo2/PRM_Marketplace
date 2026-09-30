package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .build();
    }

    private Product buildProduct() {
        return new Product.Builder()
                .id(UUID.randomUUID())
                .name("Textbook")
                .price(new BigDecimal("250.00"))
                .build();
    }

    private Address buildAddress() {
        return new Address.Builder()
                .setId(UUID.randomUUID())
                .setLine1("12 Main Road")
                .setCity("Cape Town")
                .setProvince("Western Cape")
                .setPostalCode("8001")
                .setCountry("South Africa")
                .build();
    }

    private OrderItem buildItem(Order order) {
        return new OrderItem.Builder()
                .setProduct(buildProduct())
                .setQuantity(2)
                .setPriceAtPurchase(new BigDecimal("250.00"))
                .setOrder(order)
                .build();
    }

    @Test
    void builderSetsAllFields() {
        UUID id = UUID.randomUUID();
        User buyer = buildUser();
        Address address = buildAddress();

        Order order = new Order.Builder()
                .setId(id)
                .setBuyer(buyer)
                .setStatus(OrderStatus.PENDING)
                .setTotalAmount(new BigDecimal("500.00"))
                .setShippingAddress(address)
                .build();

        assertThat(order.getId()).isEqualTo(id);
        assertThat(order.getBuyer()).isEqualTo(buyer);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("500.00");
        assertThat(order.getShippingAddress()).isEqualTo(address);
    }

    @Test
    void defaultsStatusToPending() {
        Order order = new Order.Builder().setBuyer(buildUser()).build();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void addItemLinksTheItemBackToTheOrder() {
        Order order = new Order.Builder().setBuyer(buildUser()).build();
        OrderItem item = new OrderItem.Builder()
                .setProduct(buildProduct())
                .setQuantity(1)
                .setPriceAtPurchase(new BigDecimal("10.00"))
                .build();

        order.addItem(item);

        assertThat(order.getItems()).containsExactly(item);
        assertThat(item.getOrder()).isEqualTo(order);
    }

    @Test
    void removeItemDetachesTheItem() {
        Order order = new Order.Builder().setBuyer(buildUser()).build();
        OrderItem item = buildItem(order);
        order.addItem(item);

        order.removeItem(item);

        assertThat(order.getItems()).doesNotContain(item);
    }

    @Test
    void setStatusUpdatesTheOrderStatus() {
        Order order = new Order.Builder().setBuyer(buildUser()).build();

        order.setStatus(OrderStatus.SHIPPED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void copyPreservesAllFieldsAndAllowsOverrides() {
        Order original = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(buildUser())
                .setStatus(OrderStatus.CONFIRMED)
                .setTotalAmount(new BigDecimal("75.50"))
                .setShippingAddress(buildAddress())
                .build();

        Order copy = new Order.Builder().copy(original).build();
        assertThat(copy.getId()).isEqualTo(original.getId());
        assertThat(copy.getStatus()).isEqualTo(original.getStatus());
        assertThat(copy.getTotalAmount()).isEqualByComparingTo("75.50");

        Order updated = new Order.Builder().copy(original).setStatus(OrderStatus.CANCELLED).build();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(updated.getBuyer()).isEqualTo(original.getBuyer());
    }

    @Test
    void toStringDoesNotRecurseThroughItems() {
        Order order = new Order.Builder().setBuyer(buildUser()).build();
        order.addItem(buildItem(order));

        assertThat(order.toString()).doesNotContain("StackOverflow");
    }
}
