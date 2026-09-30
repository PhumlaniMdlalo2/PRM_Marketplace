package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PostLikeTest {

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
        User user = buildUser();

        PostLike like = new PostLike.Builder()
                .setId(id)
                .setPost(post)
                .setUser(user)
                .build();

        assertThat(like.getId()).isEqualTo(id);
        assertThat(like.getPost()).isEqualTo(post);
        assertThat(like.getUser()).isEqualTo(user);
    }

    @Test
    void copyPreservesPostAndUser() {
        PostLike original = new PostLike.Builder()
                .setId(UUID.randomUUID())
                .setPost(buildPost())
                .setUser(buildUser())
                .build();

        PostLike copy = new PostLike.Builder().copy(original).build();

        assertThat(copy.getPost()).isEqualTo(original.getPost());
        assertThat(copy.getUser()).isEqualTo(original.getUser());
    }
}
