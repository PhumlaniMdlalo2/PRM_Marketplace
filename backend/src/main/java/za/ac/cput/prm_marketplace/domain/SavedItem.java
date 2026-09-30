package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "saved_items", uniqueConstraints = @UniqueConstraint(name = "uk_saved_user_product", columnNames = {"user_id", "product_id"}))
public class SavedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(updatable = false)
    private LocalDateTime savedAt;

    protected SavedItem() {
    }

    private SavedItem(Builder builder) {
        this.id = builder.id;
        this.user = builder.user;
        this.product = builder.product;
        this.savedAt = builder.savedAt;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Product getProduct() {
        return product;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.savedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavedItem)) return false;
        SavedItem savedItem = (SavedItem) o;
        return Objects.equals(id, savedItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SavedItem{" +
                "id=" + id +
                ", user=" + (user == null ? null : user.getId()) +
                ", product=" + (product == null ? null : product.getId()) +
                ", savedAt=" + savedAt +
                '}';
    }

    public static class Builder {
        private UUID id;
        private User user;
        private Product product;
        private LocalDateTime savedAt;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setUser(User user) {
            this.user = user;
            return this;
        }

        public Builder setProduct(Product product) {
            this.product = product;
            return this;
        }

        public Builder setSavedAt(LocalDateTime savedAt) {
            this.savedAt = savedAt;
            return this;
        }

        public Builder copy(SavedItem savedItem) {
            this.id = savedItem.id;
            this.user = savedItem.user;
            this.product = savedItem.product;
            this.savedAt = savedItem.savedAt;
            return this;
        }

        public SavedItem build() {
            return new SavedItem(this);
        }
    }
}
