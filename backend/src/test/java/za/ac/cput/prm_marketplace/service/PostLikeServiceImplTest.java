package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceImplTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private BulletinPostRepository bulletinPostRepository;

    @Mock
    private UserRepository userRepository;

    private PostLikeServiceImpl service;

    private UUID callerId;
    private UUID postId;
    private UUID likeId;
    private BulletinPost post;
    private User caller;

    @BeforeEach
    void setUp() {
        service = new PostLikeServiceImpl(postLikeRepository, bulletinPostRepository, userRepository);
        callerId = UUID.randomUUID();
        postId = UUID.randomUUID();
        likeId = UUID.randomUUID();
        caller = new User.Builder().setId(callerId).setEmail("c@example.com").build();
        post = new BulletinPost.Builder()
                .setId(postId)
                .setAuthor(new User.Builder().setId(UUID.randomUUID()).setEmail("a@example.com").build())
                .setTitle("Water outage")
                .setBody("Supply is interrupted.")
                .build();
    }

    @Test
    @DisplayName("the first toggle likes the post as the caller and raises the counter")
    void toggle_firstClickLikes() {
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(postLikeRepository.findByPostIdAndUserId(postId, callerId)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        PostLike saved = service.toggle(postId, callerId);

        assertThat(saved).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(callerId);
        assertThat(saved.getPost().getId()).isEqualTo(postId);
        assertThat(post.getLikeCount()).isEqualTo(1);
        verify(bulletinPostRepository).save(post);
    }

    @Test
    @DisplayName("the second toggle removes the caller's like and lowers the counter")
    void toggle_secondClickUnlikes() {
        PostLike existing = new PostLike.Builder().setId(likeId).setPost(post).setUser(caller).build();
        post = postWithLikeCount(1);
        existing = new PostLike.Builder().setId(likeId).setPost(post).setUser(caller).build();
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(postLikeRepository.findByPostIdAndUserId(postId, callerId)).thenReturn(Optional.of(existing));

        PostLike result = service.toggle(postId, callerId);

        // A null result means the like was removed, which is not an error.
        assertThat(result).isNull();
        verify(postLikeRepository).delete(existing);
        assertThat(post.getLikeCount()).isZero();
    }

    @Test
    @DisplayName("toggling a post that does not exist returns null and touches nothing")
    void toggle_unknownPostReturnsNull() {
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.empty());

        assertThat(service.toggle(postId, callerId)).isNull();
        verifyNoInteractions(postLikeRepository);
    }

    @Test
    @DisplayName("toggling as an account with no user row returns null and touches nothing")
    void toggle_unknownUserReturnsNull() {
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(callerId)).thenReturn(Optional.empty());

        assertThat(service.toggle(postId, callerId)).isNull();
        verifyNoInteractions(postLikeRepository);
    }

    @Test
    @DisplayName("toggle rejects nulls without touching the repositories")
    void toggle_rejectsNulls() {
        assertThat(service.toggle(null, callerId)).isNull();
        assertThat(service.toggle(postId, null)).isNull();
        verifyNoInteractions(postLikeRepository);
        verifyNoInteractions(bulletinPostRepository);
    }

    @Test
    @DisplayName("the generic create, update, delete and list-all operations are gone")
    void genericCrud_isNotReachable() {
        // Compile-time evidence lives in IPostLikeService. These reflective assertions pin the
        // contract so a future generic method is not quietly reintroduced.
        assertThat(java.util.Arrays.stream(IPostLikeService.class.getMethods())
                .map(java.lang.reflect.Method::getName))
                .containsExactlyInAnyOrder(
                        "toggle", "hasLiked", "countByPost", "getByUser");
    }

    @Test
    @DisplayName("hasLiked reports the caller's own like and rejects nulls")
    void hasLiked_isScopedToTheCaller() {
        when(postLikeRepository.existsByPostIdAndUserId(postId, callerId)).thenReturn(true);

        assertThat(service.hasLiked(postId, callerId)).isTrue();
        assertThat(service.hasLiked(null, callerId)).isFalse();
        assertThat(service.hasLiked(postId, null)).isFalse();
    }

    @Test
    @DisplayName("countByPost counts the likes on one post and rejects null")
    void countByPost_isScopedToThePost() {
        when(postLikeRepository.countByPostId(postId)).thenReturn(5L);

        assertThat(service.countByPost(postId)).isEqualTo(5L);
        assertThat(service.countByPost(null)).isZero();
    }

    @Test
    @DisplayName("getByUser returns only the caller's likes and rejects null")
    void getByUser_isScopedToTheCaller() {
        PostLike mine = new PostLike.Builder().setId(likeId).setPost(post).setUser(caller).build();
        when(postLikeRepository.findByUserId(callerId)).thenReturn(List.of(mine));

        assertThat(service.getByUser(callerId)).containsExactly(mine);
        assertThat(service.getByUser(null)).isEmpty();
    }

    @Test
    @DisplayName("unliking something the caller never liked cannot drive the counter negative")
    void syncLikeCount_neverGoesNegative() {
        post = postWithLikeCount(0);
        PostLike existing = new PostLike.Builder().setId(likeId).setPost(post).setUser(caller).build();
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(postLikeRepository.findByPostIdAndUserId(postId, callerId)).thenReturn(Optional.of(existing));

        service.toggle(postId, callerId);

        assertThat(post.getLikeCount()).isZero();
    }

@Test
    @DisplayName("the like service never writes or removes another account's like")
    void toggle_onlyEverTouchesTheCallersLike() {
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(postLikeRepository.findByPostIdAndUserId(postId, callerId)).thenReturn(Optional.empty());
        when(postLikeRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        PostLike saved = service.toggle(postId, callerId);

        assertThat(saved.getUser().getId()).isEqualTo(callerId);
        verify(postLikeRepository, never()).delete(any());
        verify(postLikeRepository, never()).deleteById(any());
    }

    /** The entity deliberately exposes no like counter setter, so the count is seeded via the builder. */
    private BulletinPost postWithLikeCount(int likeCount) {
        return new BulletinPost.Builder()
                .copy(post)
                .setLikeCount(likeCount)
                .build();
    }
}