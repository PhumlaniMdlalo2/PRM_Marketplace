package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.cput.prm_marketplace.domain.StudentDiscussionGroupMember;

import java.util.UUID;

public interface StudentDiscussionGroupMemberRepository extends JpaRepository<StudentDiscussionGroupMember, UUID> {
    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
    long countByGroupId(UUID groupId);
    void deleteByGroupIdAndUserId(UUID groupId, UUID userId);
}
