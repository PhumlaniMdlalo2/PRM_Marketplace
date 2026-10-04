package za.ac.cput.prm_marketplace.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A listing in the marketplace catalogue.
 *
 * <p>Reads are public: anyone can browse and search products. Writes belong to the vendor who owns
 * the listing, and the vendor is resolved from the caller's token, which is why {@code vendor} is
 * read-only here. It used to be bindable, which meant any caller could post a product carrying
 * somebody else's vendor and take credit for their listings.
 *
 * <p>{@code active} is writable on purpose: it is how a seller retires a listing without deleting
 * a row that an order line still points at.
 */
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stockQuantity;

    private String category;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_condition")
    private ProductCondition condition;

    private String city;

    private String province;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private VendorProfile vendor;

    @Column(updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    /**
     * Whether the listing is visible. This is how a vendor retires a listing: products are
     * referenced by order lines, so a hard delete would fail on the foreign key for anything
     * already sold.
     */
    private boolean active;

    /**
     * Hidden in both directions with {@code @JsonIgnore}.
     *
     * <p>It must not be written out: {@code spring.jpa.open-in-view} is false, so serialising this
     * lazy collection from a controller threw LazyInitializationException and turned every product
     * response into a 500.
     *
     * <p>It must also not be read in, and this is the part that is easy to get wrong. The obvious
     * choice of WRITE_ONLY would still let a client send the collection, and because the association
     * is cascaded with orphan removal, binding it would let a request attach or delete image rows
     * straight through the product, entirely around the ownership checks the product-image routes
     * perform. Ignoring both ways closes that.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @JsonIgnore
    private List<ProductImage> images = new ArrayList<>();

    protected Product() {
        // required by JPA
    }

    private Product(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.price = builder.price;
        this.stockQuantity = builder.stockQuantity;
        this.category = builder.category;
        this.imageUrl = builder.imageUrl;
        this.condition = builder.condition;
        this.city = builder.city;
        this.province = builder.province;
        this.vendor = builder.vendor;
        this.createdAt = builder.createdAt;
        this.active = builder.active;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public ProductCondition getCondition() {
        return condition;
    }

    public String getCity() {
        return city;
    }

    public String getProvince() {
        return province;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
    }

    public VendorProfile getVendor() {
        return vendor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", stockQuantity=" + stockQuantity +
                ", category='" + category + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", condition=" + condition +
                ", city='" + city + '\'' +
                ", province='" + province + '\'' +
                ", vendor=" + vendor +
                ", createdAt=" + createdAt +
                ", active=" + active +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String name;
        private String description;
        private BigDecimal price;
        private int stockQuantity;
        private String category;
        private String imageUrl;
        private ProductCondition condition;
        private String city;
        private String province;
        private VendorProfile vendor;
        private LocalDateTime createdAt;
        private boolean active = true;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder stockQuantity(int stockQuantity) {
            this.stockQuantity = stockQuantity;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder condition(ProductCondition condition) {
            this.condition = condition;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder province(String province) {
            this.province = province;
            return this;
        }

        public Builder vendor(VendorProfile vendor) {
            this.vendor = vendor;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder copy(Product product) {
            this.id = product.id;
            this.name = product.name;
            this.description = product.description;
            this.price = product.price;
            this.stockQuantity = product.stockQuantity;
            this.category = product.category;
            this.imageUrl = product.imageUrl;
            this.condition = product.condition;
            this.city = product.city;
            this.province = product.province;
            this.vendor = product.vendor;
            this.createdAt = product.createdAt;
            this.active = product.active;
            return this;
        }

        public Product build() {
            return new Product(this);
        }
    }
}
