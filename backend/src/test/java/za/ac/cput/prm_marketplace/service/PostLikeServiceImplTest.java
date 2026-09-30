package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.PostLike;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.BulletinPostRepository;
import za.ac.cput.prm_marketplace.repository.PostLikeRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceImplTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private BulletinPostRepository bulletinPostRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostLikeServiceImpl postLikeService;

    private User buildUser(UUID id) {
        return new User.Builder()
                .setId(id)
                .setName("Reader")
                .setEmail("reader@example.com")
                .setPasswordHash("hash")
                .build();
    }

    private BulletinPost buildPost() {
        return buildPost(0);
    }

    private BulletinPost buildPost(int likeCount) {
        return new BulletinPost.Builder()
                .setId(UUID.randomUUID())
                .setAuthor(buildUser(UUID.randomUUID()))
                .setTitle("Selling a desk")
                .setBody("Desk in good condition")
                .setLikeCount(likeCount)
                .build();
    }

    @Test
    @DisplayName("create persists the like")
    void create_saves() {
        PostLike like = new PostLike.Builder()
                .setId(UUID.randomUUID())
                .setPost(buildPost())
                .setUser(buildUser(UUID.randomUUID()))
                .build();
        when(postLikeRepository.save(like)).thenReturn(like);

        assertThat(postLikeService.create(like)).isSameAs(like);
    }

    @Test
    @DisplayName("read missing like returns null")
    void read_missing_returnsNull() {
        UUID id = UUID.randomUUID();
        when(postLikeRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(postLikeService.read(id)).isNull();
    }

    @Test
    @DisplayName("update requires an existing like")
    void update_missing_returnsNull() {
        PostLike like = new PostLike.Builder()
                .setId(UUID.randomUUID())
                .setPost(buildPost())
                .setUser(buildUser(UUID.randomUUID()))
                .build();
        when(postLikeRepository.existsById(like.getId())).thenReturn(false);

        assertThat(postLikeService.update(like)).isNull();
    }

    @Test
    @DisplayName("delete reports false for an unknown like")
    void delete_missing_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(postLikeRepository.existsById(id)).thenReturn(false);

        assertThat(postLikeService.delete(id)).isFalse();
    }

    @Test
    @DisplayName("toggle removes an existing like and decrements the counter")
    void toggle_existingLike_removes() {
        BulletinPost post = buildPost(3);
        User user = buildUser(UUID.randomUUID());
        PostLike existing = new PostLike.Builder()
                .setId(UUID.randomUUID())
                .setPost(post)
                .setUser(user)
                .build();

        when(postLikeRepository.findByPostIdAndUserId(post.getId(), user.getId()))
                .thenReturn(Optional.of(existing));
        when(bulletinPostRepository.findById(post.getId())).thenReturn(Optional.of(post));

        int before = post.getLikeCount();

        assertThat(postLikeService.toggle(post.getId(), user.getId())).isNull();
        verify(postLikeRepository).delete(existing);
        assertThat(post.getLikeCount()).isEqualTo(before - 1);
    }

    @Test
    @DisplayName("the like counter never drops below zero")
    void toggle_decrement_floorsAtZero() {
        BulletinPost post = buildPost(0);
        User user = buildUser(UUID.randomUUID());
        PostLike existing = new PostLike.Builder()
                .setId(UUID.randomUUID())
                .setPost(post)
                .setUser(user)
                .build();

        when(postLikeRepository.findByPostIdAndUserId(post.getId(), user.getId()))
                .thenReturn(Optional.of(existing));
        when(bulletinPostRepository.findById(post.getId())).thenReturn(Optional.of(post));

        postLikeService.toggle(post.getId(), user.getId());

        assertThat(post.getLikeCount()).isZero();
    }

    @Test
    @DisplayName("toggle creates a like and increments the counter")
    void toggle_newLike_creates() {
        BulletinPost post = buildPost();
        User user = buildUser(UUID.randomUUID());

        when(postLikeRepository.findByPostIdAndUserId(post.getId(), user.getId()))
                .thenReturn(Optional.empty());
        when(bulletinPostRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(postLikeRepository.save(any(PostLike.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int before = post.getLikeCount();

        PostLike saved = postLikeService.toggle(post.getId(), user.getId());

        assertThat(saved).isNotNull();
        assertThat(saved.getPost()).isSameAs(post);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(post.getLikeCount()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("toggle returns null when the post does not exist")
    void toggle_missingPost_returnsNull() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(postLikeRepository.findByPostIdAndUserId(postId, userId)).thenReturn(Optional.empty());
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.empty());

        assertThat(postLikeService.toggle(postId, userId)).isNull();
        verify(postLikeRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggle returns null when the user does not exist")
    void toggle_missingUser_returnsNull() {
        BulletinPost post = buildPost();
        UUID userId = UUID.randomUUID();

        when(postLikeRepository.findByPostIdAndUserId(post.getId(), userId)).thenReturn(Optional.empty());
        when(bulletinPostRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(postLikeService.toggle(post.getId(), userId)).isNull();
        verify(postLikeRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggle rejects null arguments")
    void toggle_rejectsNulls() {
        assertThat(postLikeService.toggle(null, UUID.randomUUID())).isNull();
        assertThat(postLikeService.toggle(UUID.randomUUID(), null)).isNull();
    }

    @Test
    @DisplayName("hasLiked rejects null arguments")
    void hasLiked_rejectsNulls() {
        assertThat(postLikeService.hasLiked(null, UUID.randomUUID())).isFalse();
        assertThat(postLikeService.hasLiked(UUID.randomUUID(), null)).isFalse();
    }

    @Test
    @DisplayName("countByPost with null post id is zero")
    void countByPost_withNull_isZero() {
        assertThat(postLikeService.countByPost(null)).isZero();
    }

    @Test
    @DisplayName("getByUser with null user id returns empty")
    void getByUser_withNull_returnsEmpty() {
        assertThat(postLikeService.getByUser(null)).isEmpty();
    }
}
