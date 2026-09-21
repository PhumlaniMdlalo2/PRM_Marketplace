package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;

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

    @Column(nullable = false)
    private String targetType;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String status;

    protected Report() {

    }

    private Report(Builder builder) {
        this.id = builder.id;
        this.reporter = builder.reporter;
        this.targetType = builder.targetType;
        this.reason = builder.reason;
        this.status = builder.status;
    }

    public UUID getId() {
        return id;
    }

    public User getReporter() {
        return reporter;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "Report{" +
                "id=" + id +
                ", reporter=" + reporter +
                ", targetType='" + targetType + '\'' +
                ", reason='" + reason + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    public static class Builder {
        private UUID id;
        private User reporter;
        private String targetType;
        private String reason;
        private String status;

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder setReporter(User reporter) {
            this.reporter = reporter;
            return this;
        }

        public Builder setTargetType(String targetType) {
            this.targetType = targetType;
            return this;
        }

        public Builder setReason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder setStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder copy(Report report) {
            this.id = report.id;
            this.reporter = report.reporter;
            this.targetType = report.targetType;
            this.reason = report.reason;
            this.status = report.status;
            return this;
        }

        public Report build() {
            return new Report(this);
        }
    }
}