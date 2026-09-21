package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BulletinPostTest {

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .build();
    }

    @Test
    void builderSetsAllFields() {
        UUID id = UUID.randomUUID();
        User author = buildUser();

        BulletinPost post = new BulletinPost.Builder()
                .setId(id)
                .setAuthor(author)
                .setTitle("Books for sale")
                .setCategory("Textbooks")
                .build();

        assertThat(post.getId()).isEqualTo(id);
        assertThat(post.getAuthor()).isEqualTo(author);
        assertThat(post.getTitle()).isEqualTo("Books for sale");
        assertThat(post.getCategory()).isEqualTo("Textbooks");
    }

    @Test
    void copyPreservesAllFields() {
        BulletinPost original = new BulletinPost.Builder()
                .setId(UUID.randomUUID())
                .setAuthor(buildUser())
                .setTitle("Books for sale")
                .setCategory("Textbooks")
                .build();

        BulletinPost copy = new BulletinPost.Builder().copy(original).build();

        assertThat(copy.getId()).isEqualTo(original.getId());
        assertThat(copy.getAuthor()).isEqualTo(original.getAuthor());
        assertThat(copy.getTitle()).isEqualTo(original.getTitle());
        assertThat(copy.getCategory()).isEqualTo(original.getCategory());
    }

    @Test
    void copyAllowsOverridingIndividualFields() {
        BulletinPost original = new BulletinPost.Builder()
                .setId(UUID.randomUUID())
                .setAuthor(buildUser())
                .setTitle("Books for sale")
                .setCategory("Textbooks")
                .build();

        BulletinPost updated = new BulletinPost.Builder().copy(original).setTitle("Laptop for sale").build();

        assertThat(updated.getId()).isEqualTo(original.getId());
        assertThat(updated.getAuthor()).isEqualTo(original.getAuthor());
        assertThat(updated.getTitle()).isEqualTo("Laptop for sale");
    }
}