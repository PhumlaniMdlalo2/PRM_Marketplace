package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommentTest {

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .build();
    }

    private BulletinPost buildPost() {
        return new BulletinPost.Builder()
                .setId(UUID.randomUUID())
                .setAuthor(buildUser())
                .setTitle("Selling a desk")
                .setBody("Desk in good condition")
                .build();
    }

    @Test
    void builderSetsAllFields() {
        UUID id = UUID.randomUUID();
        BulletinPost post = buildPost();
        User author = buildUser();

        Comment comment = new Comment.Builder()
                .setId(id)
                .setPost(post)
                .setAuthor(author)
                .setBody("Is the desk still available?")
                .build();

        assertThat(comment.getId()).isEqualTo(id);
        assertThat(comment.getPost()).isEqualTo(post);
        assertThat(comment.getAuthor()).isEqualTo(author);
        assertThat(comment.getBody()).isEqualTo("Is the desk still available?");
    }

    @Test
    void topLevelCommentHasNoParent() {
        Comment comment = new Comment.Builder()
                .setPost(buildPost())
                .setAuthor(buildUser())
                .setBody("Top level")
                .build();

        assertThat(comment.getParent()).isNull();
        assertThat(comment.isReply()).isFalse();
    }

    @Test
    void replyKnowsItsParent() {
        Comment parent = new Comment.Builder()
                .setPost(buildPost())
                .setAuthor(buildUser())
                .setBody("Parent")
                .build();

        Comment reply = new Comment.Builder()
                .setPost(parent.getPost())
                .setAuthor(buildUser())
                .setBody("Reply")
                .setParent(parent)
                .build();

        assertThat(reply.getParent()).isEqualTo(parent);
        assertThat(reply.isReply()).isTrue();
    }

    @Test
    void copyPreservesBodyAndParent() {
        Comment parent = new Comment.Builder()
                .setPost(buildPost())
                .setAuthor(buildUser())
                .setBody("Parent")
                .build();
        Comment original = new Comment.Builder()
                .setId(UUID.randomUUID())
                .setPost(parent.getPost())
                .setAuthor(buildUser())
                .setBody("Reply")
                .setParent(parent)
                .build();

        Comment copy = new Comment.Builder().copy(original).build();

        assertThat(copy.getBody()).isEqualTo("Reply");
        assertThat(copy.getParent()).isEqualTo(parent);

        Comment updated = new Comment.Builder().copy(original).setBody("Edited reply").build();
        assertThat(updated.getBody()).isEqualTo("Edited reply");
    }
}
