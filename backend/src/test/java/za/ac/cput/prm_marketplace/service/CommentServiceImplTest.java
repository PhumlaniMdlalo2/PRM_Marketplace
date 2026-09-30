package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.Comment;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.BulletinPostRepository;
import za.ac.cput.prm_marketplace.repository.CommentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private BulletinPostRepository bulletinPostRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User buildAuthor() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Commenter")
                .setEmail("commenter@example.com")
                .setPasswordHash("hash")
                .build();
    }

    private BulletinPost buildPost(int commentCount) {
        return new BulletinPost.Builder()
                .setId(UUID.randomUUID())
                .setAuthor(buildAuthor())
                .setTitle("Selling a desk")
                .setBody("Desk in good condition")
                .setCommentCount(commentCount)
                .build();
    }

    private Comment buildComment(BulletinPost post, Comment parent) {
        return new Comment.Builder()
                .setId(UUID.randomUUID())
                .setPost(post)
                .setAuthor(buildAuthor())
                .setParent(parent)
                .setBody("Is the desk still available?")
                .build();
    }

    @Test
    @DisplayName("create saves the comment and increments the post's comment count")
    void create_incrementsCommentCount() {
        BulletinPost post = buildPost(2);
        Comment comment = buildComment(post, null);

        when(commentRepository.save(comment)).thenReturn(comment);
        when(bulletinPostRepository.findById(post.getId())).thenReturn(Optional.of(post));

        Comment saved = commentService.create(comment);

        assertThat(saved).isSameAs(comment);
        assertThat(post.getCommentCount()).isEqualTo(3);
        verify(bulletinPostRepository).save(post);
    }

    @Test
    @DisplayName("create requires a post")
    void create_withoutPost_returnsNull() {
        assertThat(commentService.create(null)).isNull();

        Comment orphan = new Comment.Builder()
                .setId(UUID.randomUUID())
                .setAuthor(buildAuthor())
                .setBody("Orphan")
                .build();
        assertThat(commentService.create(orphan)).isNull();
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("create still saves the comment when the post row is gone")
    void create_missingPostRow_stillSaves() {
        BulletinPost post = buildPost(0);
        Comment comment = buildComment(post, null);

        when(commentRepository.save(comment)).thenReturn(comment);
        when(bulletinPostRepository.findById(post.getId())).thenReturn(Optional.empty());

        assertThat(commentService.create(comment)).isSameAs(comment);
        verify(bulletinPostRepository, never()).save(any());
    }

    @Test
    @DisplayName("read missing comment returns null")
    void read_missing_returnsNull() {
        UUID id = UUID.randomUUID();
        when(commentRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(commentService.read(id)).isNull();
    }

    @Test
    @DisplayName("update requires an existing comment")
    void update_missing_returnsNull() {
        Comment comment = buildComment(buildPost(0), null);
        when(commentRepository.existsById(comment.getId())).thenReturn(false);

        assertThat(commentService.update(comment)).isNull();
    }

    @Test
    @DisplayName("update saves an existing comment")
    void update_existing_saves() {
        Comment comment = buildComment(buildPost(0), null);
        when(commentRepository.existsById(comment.getId())).thenReturn(true);
        when(commentRepository.save(comment)).thenReturn(comment);

        assertThat(commentService.update(comment)).isSameAs(comment);
    }

    @Test
    @DisplayName("delete removes the comment and decrements the post's comment count")
    void delete_decrementsCommentCount() {
        BulletinPost post = buildPost(4);
        Comment comment = buildComment(post, null);

        when(commentRepository.existsById(comment.getId())).thenReturn(true);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(bulletinPostRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThat(commentService.delete(comment.getId())).isTrue();
        verify(commentRepository).deleteById(comment.getId());
        assertThat(post.getCommentCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("delete reports false for an unknown comment")
    void delete_missing_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(commentRepository.existsById(id)).thenReturn(false);

        assertThat(commentService.delete(id)).isFalse();
        verify(commentRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("comment count never drops below zero")
    void delete_floorsAtZero() {
        BulletinPost post = buildPost(0);
        Comment comment = buildComment(post, null);

        when(commentRepository.existsById(comment.getId())).thenReturn(true);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(bulletinPostRepository.findById(post.getId())).thenReturn(Optional.of(post));

        commentService.delete(comment.getId());

        assertThat(post.getCommentCount()).isZero();
    }

    @Test
    @DisplayName("getByPost with null id returns empty")
    void getByPost_withNull_returnsEmpty() {
        assertThat(commentService.getByPost(null)).isEmpty();
    }

    @Test
    @DisplayName("getTopLevelByPost with null id returns empty")
    void getTopLevelByPost_withNull_returnsEmpty() {
        assertThat(commentService.getTopLevelByPost(null)).isEmpty();
    }

    @Test
    @DisplayName("getReplies requires both a post and a parent")
    void getReplies_requiresBothArguments() {
        UUID postId = UUID.randomUUID();
        assertThat(commentService.getReplies(postId, null)).isEmpty();
        assertThat(commentService.getReplies(null, UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("getByAuthor with null id returns empty")
    void getByAuthor_withNull_returnsEmpty() {
        assertThat(commentService.getByAuthor(null)).isEmpty();
    }

    @Test
    @DisplayName("countByPost with null id is zero")
    void countByPost_withNull_isZero() {
        assertThat(commentService.countByPost(null)).isZero();
    }

    @Test
    @DisplayName("reply queries delegate to the repository")
    void getReplies_delegates() {
        UUID postId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        List<Comment> replies = List.of(buildComment(buildPost(0), null));
        when(commentRepository.findByPostIdAndParentIdOrderByCreatedAtAsc(postId, parentId))
                .thenReturn(replies);

        assertThat(commentService.getReplies(postId, parentId)).isEqualTo(replies);
    }
}
