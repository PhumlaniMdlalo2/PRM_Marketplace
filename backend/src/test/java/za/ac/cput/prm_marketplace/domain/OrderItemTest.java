package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemTest {

    private Product buildProduct() {
        return new Product.Builder()
                .id(UUID.randomUUID())
                .name("Textbook")
                .price(new BigDecimal("250.00"))
                .build();
    }

    @Test
    void builderSetsAllFields() {
        UUID id = UUID.randomUUID();
        Product product = buildProduct();
        Order order = new Order.Builder()
                .setId(UUID.randomUUID())
                .setBuyer(new User.Builder()
                        .setId(UUID.randomUUID())
                        .setName("Jane")
                        .setEmail("jane@example.com")
                        .setPasswordHash("hash")
                        .setRole(Role.STUDENT)
                        .build())
                .build();

        OrderItem item = new OrderItem.Builder()
                .setId(id)
                .setOrder(order)
                .setProduct(product)
                .setQuantity(3)
                .setPriceAtPurchase(new BigDecimal("250.00"))
                .build();

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getOrder()).isEqualTo(order);
        assertThat(item.getProduct()).isEqualTo(product);
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getPriceAtPurchase()).isEqualByComparingTo("250.00");
    }

    @Test
    void lineTotalMultipliesQuantityByUnitPrice() {
        OrderItem item = new OrderItem.Builder()
                .setProduct(buildProduct())
                .setQuantity(3)
                .setPriceAtPurchase(new BigDecimal("250.00"))
                .build();

        assertThat(item.getLineTotal()).isEqualByComparingTo("750.00");
    }

    @Test
    void lineTotalOfASingleUnitIsTheUnitPrice() {
        OrderItem item = new OrderItem.Builder()
                .setProduct(buildProduct())
                .setQuantity(1)
                .setPriceAtPurchase(new BigDecimal("99.99"))
                .build();

        assertThat(item.getLineTotal()).isEqualByComparingTo("99.99");
    }

    @Test
    void orderReferencesTheItemBack() {
        Order order = new Order.Builder()
                .setBuyer(new User.Builder()
                        .setId(UUID.randomUUID())
                        .setName("Jane")
                        .setEmail("jane@example.com")
                        .setPasswordHash("hash")
                        .setRole(Role.STUDENT)
                        .build())
                .build();
        OrderItem item = new OrderItem.Builder()
                .setProduct(buildProduct())
                .setQuantity(1)
                .setPriceAtPurchase(new BigDecimal("10.00"))
                .setOrder(order)
                .build();

        order.addItem(item);

        assertThat(order.getItems()).hasSize(1);
        assertThat(item.getOrder()).isEqualTo(order);
    }
}
