package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single line on an order.
 *
 * <p>Line items are built by {@code OrderServiceImpl} during checkout, from the cart and the
 * current catalogue price, and the order total is then derived from them. They are not editable
 * over the API at all, which is why every field here is read-only or write-only.
 *
 * <p>{@code order} stays WRITE_ONLY rather than READ_ONLY. Its accessor is only reached to read the
 * id, and letting Jackson write it out would walk Order to its items and back to this object
 * again, so every read of a line item would try to serialise the whole order graph.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Order order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Set from the cart at checkout. The previous controller accepted a quantity in the body,
     * which is how a caller could have bought three of something for the price of one.
     */
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int quantity;

    /**
     * Captured at checkout so a later catalogue price change cannot alter what was already
     * ordered. It must never come from a client: a body-supplied price is a direct route to paying
     * less than the product costs.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal priceAtPurchase;

    @Column(updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    protected OrderItem() {
    }

    private OrderItem(Builder builder) {
        this.id = builder.id;
        this.order = builder.order;
        this.product = builder.product;
        this.quantity = builder.quantity;
        this.priceAtPurchase = builder.priceAtPurchase;
        this.createdAt = builder.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPriceAtPurchase() {
        return priceAtPurchase;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public BigDecimal getLineTotal() {
        if (priceAtPurchase == null) {
            return BigDecimal.ZERO;
        }
        return priceAtPurchase.multiply(BigDecimal.valueOf(quantity));
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem)) return false;
        OrderItem orderItem = (OrderItem) o;
        return id != null && id.equals(orderItem.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", order=" + (order == null ? null : order.getId()) +
                ", product=" + (product == null ? null : product.getId()) +
                ", quantity=" + quantity +
                ", priceAtPurchase=" + priceAtPurchase +
                ", createdAt=" + createdAt +
                '}';
    }

    public static class Builder {
        private UUID id;
        private Order order;
        private Product product;
        private int quantity;
        private BigDecimal priceAtPurchase;
        private LocalDateTime createdAt;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setOrder(Order order) {
            this.order = order;
            return this;
        }

        public Builder setProduct(Product product) {
            this.product = product;
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

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(OrderItem orderItem) {
            this.id = orderItem.id;
            this.order = orderItem.order;
            this.product = orderItem.product;
            this.quantity = orderItem.quantity;
            this.priceAtPurchase = orderItem.priceAtPurchase;
            this.createdAt = orderItem.createdAt;
            return this;
        }

        public OrderItem build() {
            return new OrderItem(this);
        }
    }
}
