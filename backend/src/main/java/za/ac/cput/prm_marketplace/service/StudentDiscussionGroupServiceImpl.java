package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.StudentDiscussionGroup;
import za.ac.cput.prm_marketplace.domain.StudentDiscussionGroupMember;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.dto.StudentDiscussionGroupRequest;
import za.ac.cput.prm_marketplace.dto.StudentDiscussionGroupResponse;
import za.ac.cput.prm_marketplace.repository.BulletinPostRepository;
import za.ac.cput.prm_marketplace.repository.StudentDiscussionGroupMemberRepository;
import za.ac.cput.prm_marketplace.repository.StudentDiscussionGroupRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
public class StudentDiscussionGroupServiceImpl implements IStudentDiscussionGroupService {

    private final StudentDiscussionGroupRepository groupRepository;
    private final StudentDiscussionGroupMemberRepository memberRepository;
    private final BulletinPostRepository postRepository;
    private final UserRepository userRepository;

    public StudentDiscussionGroupServiceImpl(StudentDiscussionGroupRepository groupRepository,
                                             StudentDiscussionGroupMemberRepository memberRepository,
                                             BulletinPostRepository postRepository,
                                             UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentDiscussionGroupResponse> listForStudent(UUID userId, Role role) {
        User student = findStudent(userId, role);
        if (student == null || student.getCampus() == null || student.getCampus().isBlank()) {
            return List.of();
        }
        return groupRepository.findByCampusIgnoreCaseOrderByNameAsc(student.getCampus()).stream()
                .map(group -> response(group, userId))
                .toList();
    }

    @Override
    @Transactional
    public StudentDiscussionGroupResponse create(StudentDiscussionGroupRequest request, UUID userId, Role role) {
        User student = findStudent(userId, role);
        if (request == null || student == null || student.getCampus() == null || student.getCampus().isBlank()) {
            return null;
        }
        String name = request.name().trim();
        String description = request.description().trim();
        if (name.isEmpty() || description.isEmpty()
                || groupRepository.existsByCampusIgnoreCaseAndNameIgnoreCase(student.getCampus(), name)) {
            return null;
        }
        StudentDiscussionGroup group = groupRepository.save(new StudentDiscussionGroup.Builder()
                .setName(name)
                .setDescription(description)
                .setCampus(student.getCampus())
                .setCreator(student)
                .build());
        memberRepository.save(new StudentDiscussionGroupMember(group, student));
        return response(group, userId);
    }

    @Override
    @Transactional
    public boolean join(UUID groupId, UUID userId, Role role) {
        User student = findStudent(userId, role);
        StudentDiscussionGroup group = groupId == null ? null : groupRepository.findById(groupId).orElse(null);
        if (!sameCampus(student, group)) {
            return false;
        }
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            memberRepository.save(new StudentDiscussionGroupMember(group, student));
        }
        return true;
    }

    @Override
    @Transactional
    public boolean leave(UUID groupId, UUID userId, Role role) {
        User student = findStudent(userId, role);
        StudentDiscussionGroup group = groupId == null ? null : groupRepository.findById(groupId).orElse(null);
        if (!sameCampus(student, group)) {
            return false;
        }
        memberRepository.deleteByGroupIdAndUserId(groupId, userId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessGroup(UUID groupId, UUID userId, Role role) {
        return isMember(groupId, userId, role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BulletinPost> listPosts(UUID groupId, UUID userId, Role role) {
        if (!isMember(groupId, userId, role)) {
            return List.of();
        }
        return postRepository.findByStudentGroupIdOrderByCreatedAtDesc(groupId);
    }

    @Override
    @Transactional
    public BulletinPost createPost(UUID groupId, BulletinPost request, UUID userId, Role role) {
        if (request == null || !isMember(groupId, userId, role)
                || request.getTitle() == null || request.getTitle().isBlank()
                || request.getTitle().length() > 200 || request.getBody() == null
                || request.getBody().isBlank() || request.getBody().length() > 5000) {
            return null;
        }
        User student = userRepository.findById(userId).orElse(null);
        StudentDiscussionGroup group = groupRepository.findById(groupId).orElse(null);
        if (student == null || group == null) {
            return null;
        }
        return postRepository.save(new BulletinPost.Builder()
                .setAuthor(student)
                .setStudentGroup(group)
                .setTitle(request.getTitle().trim())
                .setBody(request.getBody().trim())
                .setCategory(request.getCategory())
                .setImageUrl(request.getImageUrl())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canReadPost(UUID postId, UUID userId, Role role) {
        if (postId == null) return false;
        BulletinPost post = postRepository.findById(postId).orElse(null);
        if (post == null) return false;
        return post.getStudentGroup() == null || isMember(post.getStudentGroup().getId(), userId, role);
    }

    private boolean isMember(UUID groupId, UUID userId, Role role) {
        User student = findStudent(userId, role);
        if (student == null || groupId == null
                || !memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            return false;
        }
        StudentDiscussionGroup group = groupRepository.findById(groupId).orElse(null);
        return sameCampus(student, group);
    }

    private static boolean sameCampus(User student, StudentDiscussionGroup group) {
        return student != null && group != null && student.getCampus() != null
                && student.getCampus().equalsIgnoreCase(group.getCampus());
    }

    private User findStudent(UUID userId, Role role) {
        if (userId == null || role != Role.STUDENT) return null;
        return userRepository.findById(userId).filter(user -> user.getRole() == Role.STUDENT).orElse(null);
    }

    private StudentDiscussionGroupResponse response(StudentDiscussionGroup group, UUID userId) {
        return new StudentDiscussionGroupResponse(group.getId(), group.getName(), group.getDescription(),
                group.getCampus(), memberRepository.countByGroupId(group.getId()),
                memberRepository.existsByGroupIdAndUserId(group.getId(), userId), group.getCreatedAt());
    }
}
