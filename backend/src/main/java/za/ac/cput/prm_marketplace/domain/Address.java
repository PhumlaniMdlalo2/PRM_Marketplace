package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String line1;

    private String line2;

    @Column(nullable = false)
    private String suburb;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String province;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(nullable = false)
    private String country = "South Africa";

    @Column(name = "is_default")
    private boolean defaultAddress;

    @Column(updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    protected Address() {
    }

    private Address(Builder builder) {
        this.id = builder.id;
        this.user = builder.user;
        this.line1 = builder.line1;
        this.line2 = builder.line2;
        this.suburb = builder.suburb;
        this.city = builder.city;
        this.province = builder.province;
        this.postalCode = builder.postalCode;
        this.country = builder.country;
        this.defaultAddress = builder.defaultAddress;
        this.createdAt = builder.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getSuburb() {
        return suburb;
    }

    public String getCity() {
        return city;
    }

    public String getProvince() {
        return province;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }

    public String getSingleLine() {
        StringBuilder sb = new StringBuilder();
        if (line1 != null) {
            sb.append(line1);
        }
        if (line2 != null && !line2.isBlank()) {
            sb.append(", ").append(line2);
        }
        if (suburb != null) {
            sb.append(", ").append(suburb);
        }
        if (city != null) {
            sb.append(", ").append(city);
        }
        if (postalCode != null) {
            sb.append(", ").append(postalCode);
        }
        if (province != null) {
            sb.append(", ").append(province);
        }
        return sb.toString();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address)) return false;
        Address address = (Address) o;
        return id != null && id.equals(address.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "Address{" +
                "id=" + id +
                ", user=" + (user == null ? null : user.getId()) +
                ", line1='" + line1 + '\'' +
                ", line2='" + line2 + '\'' +
                ", suburb='" + suburb + '\'' +
                ", city='" + city + '\'' +
                ", province='" + province + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", country='" + country + '\'' +
                ", defaultAddress=" + defaultAddress +
                ", createdAt=" + createdAt +
                '}';
    }

    public static class Builder {
        private UUID id;
        private User user;
        private String line1;
        private String line2;
        private String suburb;
        private String city;
        private String province;
        private String postalCode;
        private String country = "South Africa";
        private boolean defaultAddress;
        private LocalDateTime createdAt;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setUser(User user) {
            this.user = user;
            return this;
        }

        public Builder setLine1(String line1) {
            this.line1 = line1;
            return this;
        }

        public Builder setLine2(String line2) {
            this.line2 = line2;
            return this;
        }

        public Builder setSuburb(String suburb) {
            this.suburb = suburb;
            return this;
        }

        public Builder setCity(String city) {
            this.city = city;
            return this;
        }

        public Builder setProvince(String province) {
            this.province = province;
            return this;
        }

        public Builder setPostalCode(String postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public Builder setCountry(String country) {
            this.country = country;
            return this;
        }

        public Builder setDefaultAddress(boolean defaultAddress) {
            this.defaultAddress = defaultAddress;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(Address address) {
            this.id = address.id;
            this.user = address.user;
            this.line1 = address.line1;
            this.line2 = address.line2;
            this.suburb = address.suburb;
            this.city = address.city;
            this.province = address.province;
            this.postalCode = address.postalCode;
            this.country = address.country;
            this.defaultAddress = address.defaultAddress;
            this.createdAt = address.createdAt;
            return this;
        }

        public Address build() {
            return new Address(this);
        }
    }
}
