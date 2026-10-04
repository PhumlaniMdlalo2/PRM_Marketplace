package za.ac.cput.prm_marketplace.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.Comment;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the comment thread queries against a real database, for the same reason
 * {@link MessageRepositoryTest} exists: a mocked repository proves nothing about whether the JPQL is
 * valid, and a derived query that does not parse fails at runtime rather than at compile time.
 *
 * <p>Concretely, this is the test that would have caught the trap documented on
 * {@link CommentRepository}. Adding {@code getParentId()} to {@code Comment} for the response body was
 * enough to make {@code findByParentId} resolve against a bean property instead of the association,
 * so every comment delete returned a 500 while all 936 mocked tests stayed green.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BulletinPostRepository bulletinPostRepository;

    @Autowired
    private UserRepository userRepository;

    private User jane;
    private User john;
    private BulletinPost post;

    @BeforeEach
    void setUp() {
        jane = userRepository.save(buildUser("jane"));
        john = userRepository.save(buildUser("john"));

        post = bulletinPostRepository.save(new BulletinPost.Builder()
                .setAuthor(jane)
                .setTitle("Water outage in Block C")
                .setBody("Supply is interrupted until Friday.")
                .build());
    }

    @Test
    @DisplayName("a comment's replies are found through the parent association")
    void findByParent_Id_returnsOnlyRepliesToThatComment() {
        Comment root = saveComment("Any update on this?", null);
        Comment otherRoot = saveComment("Different question entirely", null);
        Comment reply = saveComment("Same here", root);
        saveComment("Replying to the other one", otherRoot);

        List<Comment> replies = commentRepository.findByParent_Id(root.getId());

        assertThat(replies).extracting(Comment::getBody).containsExactly("Same here");
    }

    @Test
    @DisplayName("a comment with no replies has none")
    void findByParent_Id_returnsNothingForATopLevelComment() {
        Comment root = saveComment("Any update on this?", null);

        assertThat(commentRepository.findByParent_Id(root.getId())).isEmpty();
    }

    @Test
    @DisplayName("replies are listed oldest first, and only for the requested parent")
    void findByPost_IdAndParent_IdOrderByCreatedAtAsc_scopesToOneParentOnOnePost() {
        Comment firstRoot = saveComment("First root", null);
        saveComment("Second root", null);
        saveComment("Older reply", firstRoot);
        saveComment("Newer reply", firstRoot);

        List<Comment> replies = commentRepository.findByPost_IdAndParent_IdOrderByCreatedAtAsc(
                post.getId(), firstRoot.getId());

        assertThat(replies).extracting(Comment::getBody)
                .containsExactly("Older reply", "Newer reply");
    }

    @Test
    @DisplayName("the top-level view leaves replies out")
    void findByPost_IdAndParentIsNullOrderByCreatedAtAsc_excludesReplies() {
        Comment root = saveComment("A root comment", null);
        saveComment("A reply", root);

        List<Comment> topLevel = commentRepository.findByPost_IdAndParentIsNullOrderByCreatedAtAsc(post.getId());

        assertThat(topLevel).extracting(Comment::getBody).containsExactly("A root comment");
    }

    @Test
    @DisplayName("the whole thread for a post comes back oldest first")
    void findByPost_IdOrderByCreatedAtAsc_includesRepliesInOrder() {
        Comment root = saveComment("A root comment", null);
        saveComment("A reply", root);

        List<Comment> thread = commentRepository.findByPost_IdOrderByCreatedAtAsc(post.getId());

        assertThat(thread).extracting(Comment::getBody)
                .containsExactly("A root comment", "A reply");
    }

    @Test
    @DisplayName("a comment knows the id of the comment it replies to")
    void aReply_exposesItsParentId() {
        Comment root = saveComment("A root comment", null);
        Comment reply = saveComment("A reply", root);

        assertThat(reply.getParentId()).isEqualTo(root.getId());
        assertThat(reply.isReply()).isTrue();
        assertThat(root.getParentId()).isNull();
        assertThat(root.isReply()).isFalse();
    }

    private Comment saveComment(String body, Comment parent) {
        return commentRepository.save(new Comment.Builder()
                .setPost(post)
                .setAuthor(john)
                .setParent(parent)
                .setBody(body)
                .build());
    }

    private User buildUser(String name) {
        return new User.Builder()
                .setName(name)
                .setEmail(name + "-" + UUID.randomUUID() + "@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .setVerified(true)
                .build();
    }
}