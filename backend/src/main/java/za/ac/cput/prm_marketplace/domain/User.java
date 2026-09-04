package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role; // enum: STUDENT, FACULTY, VENDOR, RESIDENT

    private String phone;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private boolean verified;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private VendorProfile vendorProfile;

    protected User() {

    }

    private User(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.email = builder.email;
        this.passwordHash = builder.passwordHash;
        this.role = builder.role;
        this.phone = builder.phone;
        this.createdAt = builder.createdAt;
        this.verified = builder.verified;
        this.vendorProfile = builder.vendorProfile;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public VendorProfile getVendorProfile() {
        return vendorProfile;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", passwordHash='" + passwordHash + '\'' +
                ", role=" + role +
                ", phone='" + phone + '\'' +
                ", createdAt=" + createdAt +
                ", verified=" + verified +
                ", vendorProfile=" + vendorProfile +
                '}';
    }

    public static class Builder{
        private UUID id;
        private String name;
        private String email;
        private String passwordHash;
        private Role role; // enum: STUDENT, FACULTY, VENDOR, RESIDENT
        private String phone;
        private LocalDateTime createdAt;
        private boolean verified;
        private VendorProfile vendorProfile;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder setRole(Role role) {
            this.role = role;
            return this;
        }

        public Builder setPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder setVerified(boolean verified) {
            this.verified = verified;
            return this;
        }

        public Builder setVendorProfile(VendorProfile vendorProfile) {
            this.vendorProfile = vendorProfile;
            return this;
        }

        public Builder copy(User user ){
            this.id = user.id;
            this.name = user.name;
            this.email = user.email;
            this.passwordHash = user.passwordHash;
            this.role = user.role;
            this.phone = user.phone;
            this.createdAt = user.createdAt;
            this.verified = user.verified;
            this.vendorProfile = user.vendorProfile;
            return this;
        }
        public User build(){
            return new User(this);
        }
    }
}
