package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal priceAtPurchase;

    protected OrderItem() {

    }

    private OrderItem(Builder builder) {
        this.id = builder.id;
        this.orderId = builder.orderId;
        this.productId = builder.productId;
        this.quantity = builder.quantity;
        this.priceAtPurchase = builder.priceAtPurchase;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPriceAtPurchase() {
        return priceAtPurchase;
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", priceAtPurchase=" + priceAtPurchase +
                '}';
    }

    public static class Builder {

        private UUID id;
        private UUID orderId;
        private UUID productId;
        private int quantity;
        private BigDecimal priceAtPurchase;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setOrderId(UUID orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder setProductId(UUID productId) {
            this.productId = productId;
            return this;
        }

        public Builder setQuantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder setPriceAtPurchase(BigDecimal priceAtPurchase) {
            this.priceAtPurchase = priceAtPurchase;
            return this;
        }

        public Builder copy(OrderItem orderItem) {
            this.id = orderItem.id;
            this.orderId = orderItem.orderId;
            this.productId = orderItem.productId;
            this.quantity = orderItem.quantity;
            this.priceAtPurchase = orderItem.priceAtPurchase;
            return this;
        }

        public OrderItem build() {
            return new OrderItem(this);
        }
    }
}