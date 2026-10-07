package za.ac.cput.prm_marketplace.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_discussion_groups",
        uniqueConstraints = @UniqueConstraint(name = "uk_student_group_campus_name", columnNames = {"campus", "name"}))
public class StudentDiscussionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 160)
    private String campus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @JsonIgnore
    private User creator;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected StudentDiscussionGroup() {
    }

    private StudentDiscussionGroup(Builder builder) {
        id = builder.id;
        name = builder.name;
        description = builder.description;
        campus = builder.campus;
        creator = builder.creator;
        createdAt = builder.createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCampus() { return campus; }
    public User getCreator() { return creator; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public static class Builder {
        private UUID id;
        private String name;
        private String description;
        private String campus;
        private User creator;
        private LocalDateTime createdAt;

        public Builder setId(UUID id) { this.id = id; return this; }
        public Builder setName(String name) { this.name = name; return this; }
        public Builder setDescription(String description) { this.description = description; return this; }
        public Builder setCampus(String campus) { this.campus = campus; return this; }
        public Builder setCreator(User creator) { this.creator = creator; return this; }
        public Builder setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public StudentDiscussionGroup build() { return new StudentDiscussionGroup(this); }
    }
}
