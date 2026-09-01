package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendor_profiles")
public class VendorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String businessName;

    private String registrationNo;

    private boolean verified;

    private BigDecimal ratingAvg;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    protected VendorProfile() {

    }

    private VendorProfile(Builder builder) {
        this.id = builder.id;
        this.user = builder.user;
        this.businessName = builder.businessName;
        this.registrationNo = builder.registrationNo;
        this.verified = builder.verified;
        this.ratingAvg = builder.ratingAvg;
        this.createdAt = builder.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public boolean isVerified() {
        return verified;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "VendorProfile{" +
                "id=" + id +
                ", user=" + user +
                ", businessName='" + businessName + '\'' +
                ", registrationNo='" + registrationNo + '\'' +
                ", verified=" + verified +
                ", ratingAvg=" + ratingAvg +
                ", createdAt=" + createdAt +
                '}';
    }

    public static class Builder {
        private UUID id;
        private User user;
        private String businessName;
        private String registrationNo;
        private boolean verified;
        private BigDecimal ratingAvg;
        private LocalDateTime createdAt;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setUser(User user) {
            this.user = user;
            return this;
        }

        public Builder setBusinessName(String businessName) {
            this.businessName = businessName;
            return this;
        }

        public Builder setRegistrationNo(String registrationNo) {
            this.registrationNo = registrationNo;
            return this;
        }

        public Builder setVerified(boolean verified) {
            this.verified = verified;
            return this;
        }

        public Builder setRatingAvg(BigDecimal ratingAvg) {
            this.ratingAvg = ratingAvg;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder copy(VendorProfile vendorProfile) {
            this.id = vendorProfile.id;
            this.user = vendorProfile.user;
            this.businessName = vendorProfile.businessName;
            this.registrationNo = vendorProfile.registrationNo;
            this.verified = vendorProfile.verified;
            this.ratingAvg = vendorProfile.ratingAvg;
            this.createdAt = vendorProfile.createdAt;
            return this;
        }

        public VendorProfile build() {
            return new VendorProfile(this);
        }
    }
}