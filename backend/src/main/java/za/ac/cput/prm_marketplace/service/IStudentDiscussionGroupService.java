package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.dto.StudentDiscussionGroupRequest;
import za.ac.cput.prm_marketplace.dto.StudentDiscussionGroupResponse;

import java.util.List;
import java.util.UUID;

public interface IStudentDiscussionGroupService {
    List<StudentDiscussionGroupResponse> listForStudent(UUID userId, Role role);
    StudentDiscussionGroupResponse create(StudentDiscussionGroupRequest request, UUID userId, Role role);
    boolean join(UUID groupId, UUID userId, Role role);
    boolean leave(UUID groupId, UUID userId, Role role);
    boolean canAccessGroup(UUID groupId, UUID userId, Role role);
    List<BulletinPost> listPosts(UUID groupId, UUID userId, Role role);
    BulletinPost createPost(UUID groupId, BulletinPost request, UUID userId, Role role);
    boolean canReadPost(UUID postId, UUID userId, Role role);
}
