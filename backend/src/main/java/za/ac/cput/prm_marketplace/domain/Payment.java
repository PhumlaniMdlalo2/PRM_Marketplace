package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method; // enum: CARD, EFT, WALLET

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status; // enum: PENDING, COMPLETED, FAILED, REFUNDED

    @Column(nullable = false, unique = true)
    private String transactionReference;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    protected Payment() {

    }

    private Payment(Builder builder) {
        this.id = builder.id;
        this.orderId = builder.orderId;
        this.userId = builder.userId;
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

