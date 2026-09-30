package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "conversations", uniqueConstraints = @UniqueConstraint(
        name = "uk_conversation_participants",
        columnNames = {"buyer_id", "seller_id", "product_id"}))
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    protected Conversation() {
    }

    private Conversation(Builder builder) {
        this.id = builder.id;
        this.buyer = builder.buyer;
        this.seller = builder.seller;
        this.product = builder.product;
        this.lastMessageAt = builder.lastMessageAt;
        this.createdAt = builder.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public User getBuyer() {
        return buyer;
    }

    public User getSeller() {
        return seller;
    }

    public Product getProduct() {
        return product;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean involves(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        UUID userId = user.getId();
        return userId.equals(buyer.getId()) || userId.equals(seller.getId());
    }

    public User counterpartOf(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        if (user.getId().equals(buyer.getId())) {
            return seller;
        }
        if (user.getId().equals(seller.getId())) {
            return buyer;
        }
        return null;
    }

    public void touch() {
        this.lastMessageAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.lastMessageAt == null) {
            this.lastMessageAt = this.createdAt;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conversation)) return false;
        Conversation that = (Conversation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "id=" + id +
                ", buyer=" + (buyer == null ? null : buyer.getId()) +
                ", seller=" + (seller == null ? null : seller.getId()) +
                ", product=" + (product == null ? null : product.getId()) +
                ", lastMessageAt=" + lastMessageAt +
                ", createdAt=" + createdAt +
                '}';
    }

    public static class Builder {
        private UUID id;
        private User buyer;
        private User seller;
        private Product product;
        private LocalDateTime lastMessageAt;
        private LocalDateTime createdAt;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setBuyer(User buyer) {
            this.buyer = buyer;
            return this;
        }

        public Builder setSeller(User seller) {
            this.seller = seller;
            return this;
        }

        public Builder setProduct(Product product) {
            this.product = product;
            return this;
        }

        public Builder setLastMessageAt(LocalDateTime lastMessageAt) {
            this.lastMessageAt = lastMessageAt;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(Conversation conversation) {
            this.id = conversation.id;
            this.buyer = conversation.buyer;
            this.seller = conversation.seller;
            this.product = conversation.product;
            this.lastMessageAt = conversation.lastMessageAt;
            this.createdAt = conversation.createdAt;
            return this;
        }

        public Conversation build() {
            return new Conversation(this);
        }
    }
}
