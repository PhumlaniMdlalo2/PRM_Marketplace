package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class BulletinPostFactoryTest {

    private User buildUser() {
        return UserFactory.createUser("Jane Doe", "jane@example.com", "hashed-password",
                Role.STUDENT, null, false, null);
    }

    @Test
    void createsBulletinPostWithValidFields() {
        User author = buildUser();

        BulletinPost post = BulletinPostFactory.createBulletinPost(author, "Books for sale", "Textbooks");

        assertThat(post).isNotNull();
        assertThat(post.getAuthor()).isEqualTo(author);
        assertThat(post.getTitle()).isEqualTo("Books for sale");
        assertThat(post.getCategory()).isEqualTo("Textbooks");
    }

    @Test
    void returnsNullWhenAuthorIsNull() {
        BulletinPost post = BulletinPostFactory.createBulletinPost(null, "Books for sale", "Textbooks");

        assertThat(post).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void returnsNullWhenTitleIsBlankOrNull(String title) {
        BulletinPost post = BulletinPostFactory.createBulletinPost(buildUser(), title, "Textbooks");

        assertThat(post).isNull();
    }

    @Test
    void createsPostWithNullCategory() {
        BulletinPost post = BulletinPostFactory.createBulletinPost(buildUser(), "Books for sale", null);

        assertThat(post).isNotNull();
        assertThat(post.getCategory()).isNull();
    }
}