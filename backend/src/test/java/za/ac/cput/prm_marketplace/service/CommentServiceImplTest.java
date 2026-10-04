package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.Comment;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.BulletinPostRepository;
import za.ac.cput.prm_marketplace.repository.CommentRepository;
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
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private BulletinPostRepository bulletinPostRepository;

    @Mock
    private UserRepository userRepository;

    private CommentServiceImpl service;

    private UUID authorId;
    private UUID intruderId;
    private UUID postId;
    private UUID commentId;
    private UUID parentId;
    private BulletinPost post;

    @BeforeEach
    void setUp() {
        service = new CommentServiceImpl(commentRepository, bulletinPostRepository, userRepository);
        authorId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        postId = UUID.randomUUID();
        commentId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        post = buildPost();
    }

    @Test
    @DisplayName("create assigns the caller as author and ignores any author in the body")
    void create_takesAuthorFromTheRequester() {
        when(userRepository.findById(authorId)).thenReturn(Optional.of(buildUser(authorId)));
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Comment submitted = new Comment.Builder()
                .setId(UUID.randomUUID())
                .setPost(post)
                .setAuthor(buildUser(intruderId))
                .setBody("Any update on this?")
                .build();

        Comment created = service.create(submitted, authorId);

        assertThat(created).isNotNull();
        assertThat(created.getAuthor().getId()).isEqualTo(authorId);
        // A client-supplied id must not turn the insert into an overwrite.
        assertThat(created.getId()).isNull();
    }

    @Test
    @DisplayName("create rejects a comment aimed at a post that does not exist")
    void create_unknownPostReturnsNull() {
        Comment submitted = new Comment.Builder().setPost(post).setBody("Hello").build();
        when(userRepository.findById(authorId)).thenReturn(Optional.of(buildUser(authorId)));
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.empty());

        assertThat(service.create(submitted, authorId)).isNull();
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("create bumps the post's comment counter")
    void create_incrementsTheCommentCount() {
        when(userRepository.findById(authorId)).thenReturn(Optional.of(buildUser(authorId)));
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        service.create(new Comment.Builder().setPost(post).setBody("Hello").build(), authorId);

        assertThat(post.getCommentCount()).isEqualTo(1);
        verify(bulletinPostRepository).save(post);
    }

    @Test
    @DisplayName("create allows a reply whose parent sits on the same post")
    void create_replyToTheSamePostIsAllowed() {
        Comment parent = buildComment(parentId, authorId, null);
        Comment submitted = new Comment.Builder()
                .setPost(post)
                .setParent(parent)
                .setBody("Agreed.")
                .build();
        when(userRepository.findById(authorId)).thenReturn(Optional.of(buildUser(authorId)));
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Comment created = service.create(submitted, authorId);

        assertThat(created).isNotNull();
        assertThat(created.getParent().getId()).isEqualTo(parentId);
    }

    @Test
    @DisplayName("create rejects a reply whose parent belongs to another post")
    void create_replyToAnotherPostsCommentIsRefused() {
        BulletinPost otherPost = new BulletinPost.Builder()
                .setId(UUID.randomUUID())
                .setAuthor(buildUser(intruderId))
                .setTitle("Another thread")
                .setBody("Unrelated.")
                .build();
        Comment foreignParent = buildComment(parentId, intruderId, otherPost);
        Comment submitted = new Comment.Builder()
                .setPost(post)
                .setParent(foreignParent)
                .setBody("Agreed.")
                .build();
        when(userRepository.findById(authorId)).thenReturn(Optional.of(buildUser(authorId)));
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findById(parentId)).thenReturn(Optional.of(foreignParent));

        assertThat(service.create(submitted, authorId)).isNull();
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("create rejects a reply whose parent has been deleted")
    void create_replyToAMissingCommentIsRefused() {
        Comment submitted = new Comment.Builder()
                .setPost(post)
                .setParent(buildComment(parentId, authorId, post))
                .setBody("Agreed.")
                .build();
        when(userRepository.findById(authorId)).thenReturn(Optional.of(buildUser(authorId)));
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThat(service.create(submitted, authorId)).isNull();
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("create returns null when the caller has no user row")
    void create_unknownAuthorReturnsNull() {
        when(userRepository.findById(authorId)).thenReturn(Optional.empty());

        Comment submitted = new Comment.Builder().setPost(post).setBody("Hello").build();
        assertThat(service.create(submitted, authorId)).isNull();
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("create returns null for a null payload, missing post or null author")
    void create_rejectsNulls() {
        assertThat(service.create(null, authorId)).isNull();
        assertThat(service.create(new Comment.Builder().setBody("No post").build(), authorId)).isNull();
        assertThat(service.create(new Comment.Builder().setPost(post).build(), null)).isNull();
        verifyNoInteractions(commentRepository);
    }

    @Test
    @DisplayName("update saves the caller's comment but keeps the stored post, author and parent")
    void update_preservesStoredOwnership() {
        Comment stored = buildComment(commentId, authorId, null);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(stored));
        when(commentRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Comment body = new Comment.Builder()
                .copy(buildComment(commentId, intruderId, null))
                .setBody("Edited body")
                .build();

        Comment updated = service.update(body, authorId);

        assertThat(updated).isNotNull();
        assertThat(updated.getAuthor().getId()).isEqualTo(authorId);
        assertThat(updated.getBody()).isEqualTo("Edited body");
    }

    @Test
    @DisplayName("update cannot move a comment onto another post or under another parent")
    void update_cannotReparentOrReown() {
        Comment stored = buildComment(commentId, authorId, null);
        BulletinPost otherPost = new BulletinPost.Builder()
                .setId(UUID.randomUUID())
                .setAuthor(buildUser(intruderId))
                .setTitle("Another thread")
                .setBody("Unrelated.")
                .build();
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(stored));
        when(commentRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Comment body = new Comment.Builder()
                .copy(stored)
                .setPost(otherPost)
                .setParent(buildComment(parentId, intruderId, otherPost))
                .setBody("Edited body")
                .build();

        Comment updated = service.update(body, authorId);

        assertThat(updated.getPost().getId()).isEqualTo(postId);
        assertThat(updated.getParent()).isNull();
    }

    @Test
    @DisplayName("update refuses to touch another account's comment")
    void update_foreignCommentIsRefused() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(buildComment(commentId, intruderId, null)));

        assertThat(service.update(buildComment(commentId, intruderId, null), authorId)).isNull();
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("update returns null for a missing comment or a body without an id")
    void update_invalidInputReturnsNull() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThat(service.update(buildComment(commentId, authorId, null), authorId)).isNull();
        assertThat(service.update(new Comment.Builder().setBody("No id").build(), authorId)).isNull();
        assertThat(service.update(null, authorId)).isNull();
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete removes the caller's own comment and lowers the post's counter")
    void delete_ownedCommentIsRemoved() {
        Comment stored = buildComment(commentId, authorId, null);
        // The service mutates whatever the repository hands back, so the stub and the assertion
        // must observe the same instance.
        post = postWithCounts(1, 0);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(stored));
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThat(service.delete(commentId, authorId)).isTrue();

        verify(commentRepository).delete(stored);
        assertThat(post.getCommentCount()).isZero();
    }

    @Test
    @DisplayName("delete drops the whole reply subtree so the parent foreign key is satisfied")
    void delete_removesTheReplySubtree() {
        Comment root = buildComment(commentId, authorId, null);
        Comment reply = buildComment(parentId, intruderId, null);
        Comment nested = buildComment(UUID.randomUUID(), intruderId, null);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(root));
        when(commentRepository.findByParentId(commentId)).thenReturn(List.of(reply));
        when(commentRepository.findByParentId(parentId)).thenReturn(List.of(nested));
        post = postWithCounts(3, 0);
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThat(service.delete(commentId, authorId)).isTrue();

        // Children must go before their parents: nested, then reply, then the root.
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(commentRepository);
        order.verify(commentRepository).delete(nested);
        order.verify(commentRepository).delete(reply);
        order.verify(commentRepository).delete(root);
        assertThat(post.getCommentCount()).isZero();
    }

    @Test
    @DisplayName("delete refuses to remove another account's comment")
    void delete_foreignCommentIsRefused() {
        when(commentRepository.findById(commentId))
                .thenReturn(Optional.of(buildComment(commentId, intruderId, null)));

        assertThat(service.delete(commentId, authorId)).isFalse();
        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete returns false for a missing comment or a null id")
    void delete_missingReturnsFalse() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThat(service.delete(commentId, authorId)).isFalse();
        assertThat(service.delete(null, authorId)).isFalse();
        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("the post comment count never drops below zero")
    void delete_counterDoesNotGoNegative() {
        Comment stored = buildComment(commentId, authorId, null);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(stored));
        post = postWithCounts(0, 0);
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThat(service.delete(commentId, authorId)).isTrue();

        assertThat(post.getCommentCount()).isZero();
    }

    @Test
    @DisplayName("thread listings are scoped by post and reject a null id")
    void threadListings_areScoped() {
        Comment stored = buildComment(commentId, authorId, null);
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(postId)).thenReturn(List.of(stored));
        when(commentRepository.findByPostIdAndParentIsNullOrderByCreatedAtAsc(postId)).thenReturn(List.of(stored));
        when(commentRepository.findByPostIdAndParentIdOrderByCreatedAtAsc(postId, commentId)).thenReturn(List.of());

        assertThat(service.getByPost(postId)).containsExactly(stored);
        assertThat(service.getTopLevelByPost(postId)).containsExactly(stored);
        assertThat(service.getReplies(postId, commentId)).isEmpty();
        assertThat(service.getByPost(null)).isEmpty();
        assertThat(service.getTopLevelByPost(null)).isEmpty();
        assertThat(service.getReplies(null, commentId)).isEmpty();
        assertThat(service.getReplies(postId, null)).isEmpty();
    }

    @Test
    @DisplayName("getByAuthor and countByPost scope to their argument and reject null")
    void authorAndCountViews_areScoped() {
        Comment stored = buildComment(commentId, authorId, null);
        when(commentRepository.findByAuthorIdOrderByCreatedAtDesc(authorId)).thenReturn(List.of(stored));
        when(commentRepository.countByPostId(postId)).thenReturn(4L);

        assertThat(service.getByAuthor(authorId)).containsExactly(stored);
        assertThat(service.getByAuthor(null)).isEmpty();
        assertThat(service.countByPost(postId)).isEqualTo(4L);
        assertThat(service.countByPost(null)).isZero();
    }

    @Test
    @DisplayName("read returns the stored comment or null when it is missing")
    void read_returnsStoredComment() {
        Comment stored = buildComment(commentId, authorId, null);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(stored));

        assertThat(service.read(commentId)).isSameAs(stored);
        assertThat(service.read(null)).isNull();
    }

    private User buildUser(UUID id) {
        return new User.Builder().setId(id).setEmail(id + "@example.com").build();
    }

    private BulletinPost buildPost() {
        return postWithCounts(0, 0);
    }

    /** The entity deliberately exposes no counter setters, so counters are seeded via the builder. */
    private BulletinPost postWithCounts(int commentCount, int likeCount) {
        return new BulletinPost.Builder()
                .setId(postId)
                .setAuthor(buildUser(intruderId))
                .setTitle("Water outage")
                .setBody("Supply is interrupted until Friday.")
                .setCommentCount(commentCount)
                .setLikeCount(likeCount)
                .build();
    }

    private Comment buildComment(UUID id, UUID author, BulletinPost parentPost) {
        return new Comment.Builder()
                .setId(id)
                .setPost(parentPost == null ? post : parentPost)
                .setAuthor(buildUser(author))
                .setBody("Any update on this?")
                .build();
    }
}