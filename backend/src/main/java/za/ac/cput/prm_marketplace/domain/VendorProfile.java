package za.ac.cput.prm_marketplace.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A seller's business profile.
 *
 * <p>The business name and registration number are public: this is a public directory and buyers
 * browse it. {@code verified} was the serious one, because it was bindable: any caller could post a
 * profile for themselves with {@code "verified": true} and then edit it to stay that way, marking
 * themselves a trusted seller with no check at all. It is read-only now, alongside the id, the
 * rating and the creation time.
 */
@Entity
@Table(name = "vendor_profiles")
public class VendorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    /**
     * The owning account.
     *
     * <p>{@code @JsonIgnore} in both directions, not WRITE_ONLY. A profile is always created for
     * whoever the token belongs to, so there is never a legitimate reason to accept the owner from
     * a request, and leaving it bindable only invites a body that names a different account. The
     * old controller passed exactly that field straight into the service, which is how one seller
     * could open or overwrite another's business.
     */
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String businessName;

    private String registrationNo;

    @JsonIgnore
    private String payoutAccountHolder;

    @JsonIgnore
    private String payoutBankName;

    @JsonIgnore
    private String payoutAccountNumber;

    @JsonIgnore
    private String payoutBranchCode;

    @JsonIgnore
    private String payoutAccountType;

    /** Set by admin through moderation; never by the seller. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean verified;

    /** Derived from the seller's reviews; never accepted from a client. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal ratingAvg;

    /**
     * How many reviews {@link #ratingAvg} summarises.
     *
     * <p>Maintained by {@code VendorRatingService}, and read-only over JSON for the same reason
     * {@code ratingAvg} is: a count the client can set is a count that will be wrong.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int ratingCount;

    @Column(updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    protected VendorProfile() {

    }

    private VendorProfile(Builder builder) {
        this.id = builder.id;
        this.user = builder.user;
        this.businessName = builder.businessName;
        this.registrationNo = builder.registrationNo;
        this.payoutAccountHolder = builder.payoutAccountHolder;
        this.payoutBankName = builder.payoutBankName;
        this.payoutAccountNumber = builder.payoutAccountNumber;
        this.payoutBranchCode = builder.payoutBranchCode;
        this.payoutAccountType = builder.payoutAccountType;
        this.verified = builder.verified;
        this.ratingAvg = builder.ratingAvg;
        this.ratingCount = builder.ratingCount;
        this.createdAt = builder.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    /**
     * The id of the account behind this profile, and nothing else about it.
     *
     * <p>{@code user} itself is {@code @JsonIgnore} because a profile read is a public endpoint and
     * serialising the account would publish that person's email and phone to anyone browsing
     * listings. That left the frontend with no way to reach {@code POST /conversations/start}, which
     * names a user id: the Contact button had nothing to send, so no conversation could be started
     * from anywhere in the app.
     *
     * <p>So this exposes the id alone. It is an opaque primary key that the API already hands out
     * publicly in {@code AuthorSummary} on every bulletin post and comment, it names no account, and
     * it is read-only, so it cannot be pointed at an owner by writing it.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public UUID getUserId() {
        return user == null ? null : user.getId();
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    @JsonIgnore
    public String getPayoutAccountHolder() {
        return payoutAccountHolder;
    }

    @JsonIgnore
    public String getPayoutBankName() {
        return payoutBankName;
    }

    @JsonIgnore
    public String getPayoutAccountNumber() {
        return payoutAccountNumber;
    }

    @JsonIgnore
    public String getPayoutBranchCode() {
        return payoutBranchCode;
    }

    @JsonIgnore
    public String getPayoutAccountType() {
        return payoutAccountType;
    }

    public boolean isVerified() {
        return verified;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    /**
     * How many reviews the average is drawn from.
     *
     * <p>Never trust {@code ratingAvg} without it. A single five-star review and fifty of them
     * produce the same number, and the listing page shows both as "4.8" — so this is what tells a
     * reader whether the average has any weight behind it.
     */
    public int getRatingCount() {
        return ratingCount;
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
                ", ratingCount=" + ratingCount +
                ", createdAt=" + createdAt +
                '}';
    }

    public static class Builder {
        private UUID id;
        private User user;
        private String businessName;
        private String registrationNo;
        private String payoutAccountHolder;
        private String payoutBankName;
        private String payoutAccountNumber;
        private String payoutBranchCode;
        private String payoutAccountType;
private boolean verified;
        private BigDecimal ratingAvg;
        private int ratingCount;
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

        public Builder setPayoutDetails(String accountHolder, String bankName, String accountNumber,
                                        String branchCode, String accountType) {
            this.payoutAccountHolder = accountHolder;
            this.payoutBankName = bankName;
            this.payoutAccountNumber = accountNumber;
            this.payoutBranchCode = branchCode;
            this.payoutAccountType = accountType;
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

        public Builder setRatingCount(int ratingCount) {
            this.ratingCount = ratingCount;
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
            this.payoutAccountHolder = vendorProfile.payoutAccountHolder;
            this.payoutBankName = vendorProfile.payoutBankName;
            this.payoutAccountNumber = vendorProfile.payoutAccountNumber;
            this.payoutBranchCode = vendorProfile.payoutBranchCode;
            this.payoutAccountType = vendorProfile.payoutAccountType;
            this.verified = vendorProfile.verified;
            this.ratingAvg = vendorProfile.ratingAvg;
            // Copied rather than reset: this is the one path that must not drop it, since every
            // write to a profile rebuilds the entity from the stored row.
            this.ratingCount = vendorProfile.ratingCount;
            this.createdAt = vendorProfile.createdAt;
            return this;
        }

        public VendorProfile build() {
            return new VendorProfile(this);
        }
    }
}
