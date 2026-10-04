package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;
import tools.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import za.ac.cput.prm_marketplace.dto.AuthorSummary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne
    @JoinColumn(name = "shipping_address_id")
    private Address shippingAddress;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    private Order(Builder builder) {
        this.id = builder.id;
        this.buyer = builder.buyer;
        this.status = builder.status;
        this.totalAmount = builder.totalAmount;
        this.shippingAddress = builder.shippingAddress;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public UUID getId() {
        return id;
    }

    /**
 * Server-owned. The buyer is taken from the authenticated caller, so accepting it from a request
 * body would let anyone place an order in somebody else's name.
 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    /** Serialised through {@link AuthorSummary}: see {@code AuthorSummary} for why. */
    @Schema(implementation = AuthorSummary.class)
    @JsonSerialize(using = AuthorSummary.Serializer.class)
    public User getBuyer() {
        return buyer;
    }

    /** Server-owned. A request body must not be able to declare an order already delivered. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Server-owned, recomputed from the catalogue at checkout. A body-supplied total would let a
     * client pay whatever it likes.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Lazy collection. {@code open-in-view} is false, so serialising it from a controller would
     * throw once the transaction is closed.
     */
    @JsonIgnore
    public List<OrderItem> getItems() {
        return items;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    /**
     * Delivery address only, and only while the order is still PENDING. Used by the checkout
     * flow and by a buyer correcting where an order is going.
     */
    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    /**
     * Server-owned like the rest: set once, from the order's own line items, and never taken from
     * a request body.
     */
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return id != null && id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", buyer=" + (buyer == null ? null : buyer.getId()) +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", shippingAddress=" + (shippingAddress == null ? null : shippingAddress.getId()) +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public static class Builder {
        private UUID id;
        private User buyer;
        private OrderStatus status = OrderStatus.PENDING;
        private BigDecimal totalAmount;
        private Address shippingAddress;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setBuyer(User buyer) {
            this.buyer = buyer;
            return this;
        }

        public Builder setStatus(OrderStatus status) {
            this.status = status;
            return this;
        }

        public Builder setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder setShippingAddress(Address shippingAddress) {
            this.shippingAddress = shippingAddress;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder copy(Order order) {
            this.id = order.id;
            this.buyer = order.buyer;
            this.status = order.status;
            this.totalAmount = order.totalAmount;
            this.shippingAddress = order.shippingAddress;
            this.createdAt = order.createdAt;
            this.updatedAt = order.updatedAt;
            return this;
        }

        public Order build() {
            return new Order(this);
        }
    }
}
