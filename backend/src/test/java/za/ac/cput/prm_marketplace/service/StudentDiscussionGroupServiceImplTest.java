package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.StudentDiscussionGroup;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.BulletinPostRepository;
import za.ac.cput.prm_marketplace.repository.StudentDiscussionGroupMemberRepository;
import za.ac.cput.prm_marketplace.repository.StudentDiscussionGroupRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentDiscussionGroupServiceImplTest {

    @Mock private StudentDiscussionGroupRepository groupRepository;
    @Mock private StudentDiscussionGroupMemberRepository memberRepository;
    @Mock private BulletinPostRepository postRepository;
    @Mock private UserRepository userRepository;

    private StudentDiscussionGroupServiceImpl service;
    private UUID studentId;
    private UUID groupId;
    private UUID postId;
    private User student;
    private StudentDiscussionGroup group;

    @BeforeEach
    void setUp() {
        service = new StudentDiscussionGroupServiceImpl(
                groupRepository, memberRepository, postRepository, userRepository);
        studentId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        postId = UUID.randomUUID();
        student = new User.Builder().setId(studentId).setName("Student")
                .setEmail("student@campus.ac.za").setRole(Role.STUDENT)
                .setCampus("Cape Town").build();
        group = new StudentDiscussionGroup.Builder().setId(groupId).setName("Computing")
                .setDescription("Computing students").setCampus("Cape Town").setCreator(student).build();
    }

    @Test
    void publicPostsRemainReadableWithoutAnAccount() {
        BulletinPost publicPost = new BulletinPost.Builder().setId(postId).setTitle("Public")
                .setBody("Anyone can read this").build();
        when(postRepository.findById(postId)).thenReturn(Optional.of(publicPost));

        assertThat(service.canReadPost(postId, null, null)).isTrue();
    }

    @Test
    void aJoinedStudentCanReadGroupPostsOnTheirCampus() {
        BulletinPost groupPost = new BulletinPost.Builder().setId(postId).setTitle("Group")
                .setBody("Members only").setStudentGroup(group).build();
        when(postRepository.findById(postId)).thenReturn(Optional.of(groupPost));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(memberRepository.existsByGroupIdAndUserId(groupId, studentId)).thenReturn(true);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThat(service.canReadPost(postId, studentId, Role.STUDENT)).isTrue();
    }

    @Test
    void aStudentCannotReadGroupPostsWithoutMembership() {
        BulletinPost groupPost = new BulletinPost.Builder().setId(postId).setTitle("Group")
                .setBody("Members only").setStudentGroup(group).build();
        when(postRepository.findById(postId)).thenReturn(Optional.of(groupPost));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(memberRepository.existsByGroupIdAndUserId(groupId, studentId)).thenReturn(false);

        assertThat(service.canReadPost(postId, studentId, Role.STUDENT)).isFalse();
    }

    @Test
    void aNonStudentCannotReadGroupPostsEvenWithMembership() {
        BulletinPost groupPost = new BulletinPost.Builder().setId(postId).setTitle("Group")
                .setBody("Members only").setStudentGroup(group).build();
        when(postRepository.findById(postId)).thenReturn(Optional.of(groupPost));

        assertThat(service.canReadPost(postId, studentId, Role.VENDOR)).isFalse();
        verifyNoInteractions(userRepository, memberRepository, groupRepository);
    }

    @Test
    void membershipDoesNotGrantAccessAfterTheStudentChangesCampus() {
        User movedStudent = new User.Builder().copy(student).setCampus("Johannesburg").build();
        BulletinPost groupPost = new BulletinPost.Builder().setId(postId).setTitle("Group")
                .setBody("Members only").setStudentGroup(group).build();
        when(postRepository.findById(postId)).thenReturn(Optional.of(groupPost));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(movedStudent));
        when(memberRepository.existsByGroupIdAndUserId(groupId, studentId)).thenReturn(true);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThat(service.canReadPost(postId, studentId, Role.STUDENT)).isFalse();
    }
}
