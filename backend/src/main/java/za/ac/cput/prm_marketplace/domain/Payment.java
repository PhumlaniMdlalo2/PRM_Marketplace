package za.ac.cput.prm_marketplace.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A payment record. It is a financial record, so the client supplies only what describes the
 * attempt: which order is being paid, how much, and by which method. Everything else is decided
 * by the server.
 *
 * <p>The reason each field is read-only matters as much as the annotation:
 * <ul>
 *   <li>{@code userId} - a payment must belong to the caller who made it. It used to come from the
 *       body, so a caller could file a payment against someone else's account.</li>
 *   <li>{@code status} and {@code paidAt} - the payment lifecycle moves through
 *       {@code updateStatus}, which validates the transition. Binding them let a caller write
 *       COMPLETED directly and skip the state machine.</li>
 *   <li>{@code transactionReference} - generated per payment and used to look the payment up.
 *       Accepting one from the client risks colliding with or overwriting a real reference.</li>
 *   <li>{@code id} - generated on save.</li>
 * </ul>
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID userId;

    @Column(name = "seller_user_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID sellerUserId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private PaymentStatus status; // enum: PENDING, COMPLETED, FAILED, REFUNDED

    @Column(nullable = false, unique = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String transactionReference;

    @Column(updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime paidAt;

    protected Payment() {

    }

    private Payment(Builder builder) {
        this.id = builder.id;
        this.orderId = builder.orderId;
        this.userId = builder.userId;
        this.sellerUserId = builder.sellerUserId;
        this.amount = builder.amount;
        this.method = builder.method;
        this.status = builder.status;
        this.transactionReference = builder.transactionReference;
        this.createdAt = builder.createdAt;
        this.paidAt = builder.paidAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getSellerUserId() {
        return sellerUserId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", userId=" + userId +
                ", amount=" + amount +
                ", method=" + method +
                ", status=" + status +
                ", transactionReference='" + transactionReference + '\'' +
                ", createdAt=" + createdAt +
                ", paidAt=" + paidAt +
                '}';
    }

    public static class Builder {

        private UUID id;
        private UUID orderId;
        private UUID userId;
        private UUID sellerUserId;
        private BigDecimal amount;
        private PaymentMethod method;
        private PaymentStatus status;
        private String transactionReference;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setOrderId(UUID orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder setUserId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder setSellerUserId(UUID sellerUserId) {
            this.sellerUserId = sellerUserId;
            return this;
        }

        public Builder setAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder setMethod(PaymentMethod method) {
            this.method = method;
            return this;
        }

        public Builder setStatus(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public Builder setTransactionReference(String transactionReference) {
            this.transactionReference = transactionReference;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder setPaidAt(LocalDateTime paidAt) {
            this.paidAt = paidAt;
            return this;
        }

        public Builder copy(Payment payment) {
            this.id = payment.id;
            this.orderId = payment.orderId;
            this.userId = payment.userId;
            this.sellerUserId = payment.sellerUserId;
            this.amount = payment.amount;
            this.method = payment.method;
            this.status = payment.status;
            this.transactionReference = payment.transactionReference;
            this.createdAt = payment.createdAt;
            this.paidAt = payment.paidAt;
            return this;
        }

        public Payment build() {
            return new Payment(this);
        }
    }
}
