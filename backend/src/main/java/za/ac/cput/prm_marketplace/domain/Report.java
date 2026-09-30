package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReportTargetType targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(length = 1000)
    private String resolutionNotes;

    protected Report() {
    }

    private Report(Builder builder) {
        this.id = builder.id;
        this.reporter = builder.reporter;
        this.targetType = builder.targetType;
        this.targetId = builder.targetId;
        this.reason = builder.reason;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.resolvedAt = builder.resolvedAt;
        this.resolutionNotes = builder.resolutionNotes;
    }

    public UUID getId() {
        return id;
    }

    public User getReporter() {
        return reporter;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getReason() {
        return reason;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public boolean isResolved() {
        return status == ReportStatus.RESOLVED || status == ReportStatus.DISMISSED;
    }

    public void resolve(ReportStatus newStatus, String notes) {
        this.status = newStatus;
        this.resolutionNotes = notes;
        this.resolvedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Report)) return false;
        Report report = (Report) o;
        return Objects.equals(id, report.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Report{" +
                "id=" + id +
                ", reporter=" + reporter +
                ", targetType=" + targetType +
                ", targetId=" + targetId +
                ", reason='" + reason + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", resolvedAt=" + resolvedAt +
                '}';
    }

    public static class Builder {
        private UUID id;
        private User reporter;
        private ReportTargetType targetType;
        private UUID targetId;
        private String reason;
        private ReportStatus status = ReportStatus.OPEN;
        private LocalDateTime createdAt;
        private LocalDateTime resolvedAt;
        private String resolutionNotes;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setReporter(User reporter) {
            this.reporter = reporter;
            return this;
        }

        public Builder setTargetType(ReportTargetType targetType) {
            this.targetType = targetType;
            return this;
        }

        public Builder setTargetId(UUID targetId) {
            this.targetId = targetId;
            return this;
        }

        public Builder setReason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder setStatus(ReportStatus status) {
            this.status = status;
            return this;
        }

        public Builder setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder setResolvedAt(LocalDateTime resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public Builder setResolutionNotes(String resolutionNotes) {
            this.resolutionNotes = resolutionNotes;
            return this;
        }

        public Builder copy(Report report) {
            this.id = report.id;
            this.reporter = report.reporter;
            this.targetType = report.targetType;
            this.targetId = report.targetId;
            this.reason = report.reason;
            this.status = report.status;
            this.createdAt = report.createdAt;
            this.resolvedAt = report.resolvedAt;
            this.resolutionNotes = report.resolutionNotes;
            return this;
        }

        public Report build() {
            return new Report(this);
        }
    }
}
