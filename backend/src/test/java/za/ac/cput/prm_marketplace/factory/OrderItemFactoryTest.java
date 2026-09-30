package za.ac.cput.prm_marketplace.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.OrderItem;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemFactoryTest {

    private Order buildOrder() {
        return new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(new User.Builder()
                        .setId(UUID.randomUUID())
                        .setName("Buyer")
                        .setEmail("buyer@example.com")
                        .setPasswordHash("hash")
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

    @Test
    @DisplayName("builds an order item wired to the order and product")
    void createOrderItem_wiresAssociations() {
        Order order = buildOrder();
        Product product = buildProduct(new BigDecimal("100.00"));

        OrderItem item = OrderItemFactory.createOrderItem(order, product, 3, new BigDecimal("95.00"));

        assertThat(item).isNotNull();
        assertThat(item.getOrder()).isSameAs(order);
        assertThat(item.getProduct()).isSameAs(product);
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getPriceAtPurchase()).isEqualByComparingTo("95.00");
    }

    @Test
    @DisplayName("requires both an order and a product")
    void createOrderItem_requiresOrderAndProduct() {
        assertThat(OrderItemFactory.createOrderItem(null, buildProduct(BigDecimal.ONE), 1, BigDecimal.ONE))
                .isNull();
        assertThat(OrderItemFactory.createOrderItem(buildOrder(), null, 1, BigDecimal.ONE))
                .isNull();
    }

    @Test
    @DisplayName("rejects a non-positive quantity")
    void createOrderItem_rejectsBadQuantity() {
        Order order = buildOrder();
        Product product = buildProduct(BigDecimal.ONE);

        assertThat(OrderItemFactory.createOrderItem(order, product, 0, BigDecimal.ONE)).isNull();
        assertThat(OrderItemFactory.createOrderItem(order, product, -2, BigDecimal.ONE)).isNull();
    }

    @Test
    @DisplayName("rejects a null or negative purchase price")
    void createOrderItem_rejectsBadPrice() {
        Order order = buildOrder();
        Product product = buildProduct(BigDecimal.ONE);

        assertThat(OrderItemFactory.createOrderItem(order, product, 1, null)).isNull();
        assertThat(OrderItemFactory.createOrderItem(order, product, 1, new BigDecimal("-1.00"))).isNull();
    }

    @Test
    @DisplayName("a zero price is allowed because it can be a giveaway or promotion")
    void createOrderItem_allowsZeroPrice() {
        assertThat(OrderItemFactory.createOrderItem(
                buildOrder(), buildProduct(BigDecimal.ZERO), 1, BigDecimal.ZERO)).isNotNull();
    }

    @Test
    @DisplayName("createOrderItemForProduct snapshots the product's current price")
    void createOrderItemForProduct_usesProductPrice() {
        Order order = buildOrder();
        Product product = buildProduct(new BigDecimal("250.00"));

        OrderItem item = OrderItemFactory.createOrderItemForProduct(order, product, 2);

        assertThat(item).isNotNull();
        assertThat(item.getPriceAtPurchase()).isEqualByComparingTo("250.00");
        assertThat(item.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("createOrderItemForProduct returns null without a product")
    void createOrderItemForProduct_requiresProduct() {
        assertThat(OrderItemFactory.createOrderItemForProduct(buildOrder(), null, 1)).isNull();
    }

    @Test
    @DisplayName("createOrderItemForProduct inherits the quantity validation")
    void createOrderItemForProduct_validatesQuantity() {
        Order order = buildOrder();
        Product product = buildProduct(new BigDecimal("10.00"));

        assertThat(OrderItemFactory.createOrderItemForProduct(order, product, 0)).isNull();
    }
}
