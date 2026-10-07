package za.ac.cput.prm_marketplace.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.CartItem;
import za.ac.cput.prm_marketplace.domain.Comment;
import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.Message;
import za.ac.cput.prm_marketplace.domain.Order;
import za.ac.cput.prm_marketplace.domain.PostLike;
import za.ac.cput.prm_marketplace.domain.Report;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.SavedItem;
import za.ac.cput.prm_marketplace.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sweeps every entity that carries a {@link User} and pins one rule for all of them: a user embedded
 * in someone else's content serialises as an {@code AuthorSummary}, so a reader gets a name and an
 * avatar and never a contact detail.
 *
 * <p>This is one test over the whole set rather than an assertion per controller because the failure
 * mode is quiet. A new {@code private User something} field with no serializer on its getter compiles,
 * passes every service test, and starts handing out email addresses. Keeping the list here makes that
 * omission a failing test instead of a leak somebody notices later.
 *
 * <p>The full user record has exactly one home: {@code GET /api/users/me}, which answers with a
 * {@code UserResponse} to its owner.
 */
@SpringBootTest
class NestedUserExposureTest {

    @Autowired
    private ObjectMapper objectMapper;

    private String secretEmail;
    private String secretPhone;
    private String secretHash;
    private User author;

    @BeforeEach
    void setUp() {
        secretEmail = "leak-" + UUID.randomUUID() + "@example.com";
        secretPhone = "+27825550199";
        secretHash = "hash-that-must-never-be-serialised";

        author = new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail(secretEmail)
                .setPhone(secretPhone)
                .setPasswordHash(secretHash)
                .setRole(Role.ADMIN)
                .setVerified(true)
                .setCreatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("no entity leaks a nested user's email, phone, role or password hash")
    void nestedUsers_neverCarryContactDetails() {
        for (Carrier carrier : carriers()) {
            String json = serialise(carrier.entity());

            assertThat(json)
                    .as("%s must not expose the user's email", carrier.label())
                    .doesNotContain(secretEmail)
                    .as("%s must not expose the user's phone", carrier.label())
                    .doesNotContain(secretPhone)
                    .as("%s must not expose the password hash", carrier.label())
                    .doesNotContain(secretHash)
                    .as("%s must not expose the role", carrier.label())
                    .doesNotContain("ADMIN")
                    .as("%s must not nest the whole user object", carrier.label())
                    .doesNotContain("\"passwordHash\"");
        }
    }

    @Test
    @DisplayName("a nested user keeps the fields a reader is allowed to see")
    void nestedUsers_keepThePublicFields() {
        for (Carrier carrier : carriers()) {
            String json = serialise(carrier.entity());

            assertThat(json)
                    .as("%s should still name its user", carrier.label())
                    .contains("\"name\":\"Jane Doe\"")
                    .as("%s should still carry an avatar field", carrier.label())
                    .contains("\"avatarUrl\"");
        }
    }

    private List<Carrier> carriers() {
        BulletinPost post = new BulletinPost.Builder()
                .setId(UUID.randomUUID())
                .setAuthor(author)
                .setTitle("Water outage")
                .setBody("Until Friday.")
                .build();

        return List.of(
                new Carrier("BulletinPost.author", post),
                new Carrier("Comment.author", new Comment.Builder()
                        .setId(UUID.randomUUID())
                        .setPost(post)
                        .setAuthor(author)
                        .setBody("Any update on this?")
                        .build()),
                new Carrier("PostLike.user", new PostLike.Builder()
                        .setId(UUID.randomUUID())
                        .setPost(post)
                        .setUser(author)
                        .build()),
                new Carrier("CartItem.user", CartItem.builder().user(author).build()),
                new Carrier("SavedItem.user", new SavedItem.Builder()
                        .setId(UUID.randomUUID())
                        .setUser(author)
                        .build()),
                new Carrier("Conversation.buyer", new Conversation.Builder()
                        .setBuyer(author)
                        .setSeller(author)
                        .build()),
                new Carrier("Message.sender", new Message.Builder()
                        .setSender(author)
                        .setBody("Is it still available?")
                        .build()),
                new Carrier("Order.buyer", new Order.Builder()
                        .setBuyer(author)
                        .build()),
                new Carrier("Report.reporter", new Report.Builder()
                        .setReporter(author)
                        .build()));
    }

    private String serialise(Object entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (Exception e) {
            throw new AssertionError(
                    "Serialising " + entity.getClass().getSimpleName() + " failed: " + e, e);
        }
    }

    /** An entity plus the relation it was built to exercise. */
    private record Carrier(String label, Object entity) {
    }
}