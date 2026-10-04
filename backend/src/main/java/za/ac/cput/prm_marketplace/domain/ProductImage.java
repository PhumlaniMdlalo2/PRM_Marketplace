package za.ac.cput.prm_marketplace.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One of a product's photographs.
 *
 * <p>The product is named by the request path, not by the body, so {@code product} is hidden from
 * output and ignored on input: the service loads the seller's own product instead. It stays
 * WRITE_ONLY rather than READ_ONLY because writing it out would walk Product to its images and back
 * to this object, and that lazy collection cannot be walked here.
 */
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

    /** At most one image per product is primary; the service clears the previous one. */
    @Column(name = "is_primary")
    private boolean primary;

    @Column(updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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

    /**
     * Moves the primary flag on an image already in the database.
     *
     * <p>The service uses this to demote the previous primary when a new one arrives. A product
     * must only ever have one cover image, and there is no unique constraint on the column that
     * could enforce it for us.
     */
    public void setPrimary(boolean primary) {
        this.primary = primary;
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
        return id != null && id.equals(productImage.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
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
