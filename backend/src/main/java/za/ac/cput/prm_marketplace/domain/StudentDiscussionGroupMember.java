package za.ac.cput.prm_marketplace.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_discussion_group_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_student_group_member", columnNames = {"group_id", "user_id"}))
public class StudentDiscussionGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentDiscussionGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    protected StudentDiscussionGroupMember() {
    }

    public StudentDiscussionGroupMember(StudentDiscussionGroup group, User user) {
        this.group = group;
        this.user = user;
    }

    public UUID getId() { return id; }
    public StudentDiscussionGroup getGroup() { return group; }
    public User getUser() { return user; }

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) joinedAt = LocalDateTime.now();
    }
}
