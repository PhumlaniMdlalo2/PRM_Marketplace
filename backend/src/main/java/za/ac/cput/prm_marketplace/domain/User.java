package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    private String avatarUrl;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private VendorProfile vendorProfile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

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
        this.avatarUrl = builder.avatarUrl;
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

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Server-owned. The role is decided by the registration and approval flow, so a request body
     * that carries "role" must not be able to grant itself a faculty or vendor account.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Role getRole() {
        return role;
    }

    public String getPhone() {
        return phone;
    }

    /** Server-owned, and {@code updatable = false}, so it must not be settable from a body either. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Server-owned. Verification happens only through the emailed code. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean isVerified() {
        return verified;
    }

    /**
     * The inverse side of the vendor-profile relationship, and the one half of the pair that is
     * actually reachable from a user request body.
     *
     * <p>{@code VendorProfile.user} is already ignored, so without this the pair could still be
     * assembled from a {@code POST} or {@code PUT} to the users routes: a caller could attach a
     * profile to their own account and skip the ownership and verification rules that
     * VendorProfileService enforces. Both directions are ignored for the same reason — neither is
     * the way a client reads or writes a vendor profile. Profiles are served by
     * VendorProfileController, and a user is created from a body with no profile in it.
     */
    @JsonIgnore
public VendorProfile getVendorProfile() {
        return vendorProfile;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * Lazy collection. {@code spring.jpa.open-in-view} is false, so Jackson would try to
     * initialise this after the transaction has closed and the endpoint would answer 500.
     * Addresses are served by AddressController.
     */
    @JsonIgnore
    public List<Address> getAddresses() {
        return addresses;
    }

    public void addAddress(Address address) {
        addresses.add(address);
        address.setUser(this);
    }

    public void removeAddress(Address address) {
        addresses.remove(address);
        address.setUser(null);
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** Walks the lazy collection, so it cannot be serialised either. */
    @JsonIgnore
    public Address getDefaultAddress() {
        return addresses.stream()
                .filter(Address::isDefaultAddress)
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        // The password hash is deliberately absent. This string reaches log statements and
        // exception messages, and the original form leaked the credential into both.
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
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
        private String avatarUrl;
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

        public Builder setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
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
            this.avatarUrl = user.avatarUrl;
            this.vendorProfile = user.vendorProfile;
            return this;
        }
        public User build(){
            return new User(this);
        }
    }
}
