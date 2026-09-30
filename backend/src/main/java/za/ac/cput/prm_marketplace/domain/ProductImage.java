package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Product product;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "is_primary")
    private boolean primary;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    protected ProductImage() {
    }

    private ProductImage(Builder builder) {
        this.id = builder.id;
        this.product = builder.product;
        this.imageUrl = builder.imageUrl;
        this.sortOrder = builder.sortOrder;
        this.primary = builder.primary;
        this.createdAt = builder.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isPrimary() {
        return primary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductImage)) return false;
        ProductImage productImage = (ProductImage) o;
        return Objects.equals(id, productImage.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ProductImage{" +
                "id=" + id +
                ", product=" + (product == null ? null : product.getId()) +
                ", imageUrl='" + imageUrl + '\'' +
                ", sortOrder=" + sortOrder +
                ", primary=" + primary +
                ", createdAt=" + createdAt +
                '}';
    }

    public static class Builder {
        private UUID id;
        private Product product;
        private String imageUrl;
        private int sortOrder;
        private boolean primary;
        private LocalDateTime createdAt;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setProduct(Product product) {
            this.product = product;
            return this;
        }

        public Builder setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder setSortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder setPrimary(boolean primary) {
            this.primary = primary;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(ProductImage productImage) {
            this.id = productImage.id;
            this.product = productImage.product;
            this.imageUrl = productImage.imageUrl;
            this.sortOrder = productImage.sortOrder;
            this.primary = productImage.primary;
            this.createdAt = productImage.createdAt;
            return this;
        }

        public ProductImage build() {
            return new ProductImage(this);
        }
    }
}
