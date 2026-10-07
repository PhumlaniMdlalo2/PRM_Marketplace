package za.ac.cput.prm_marketplace.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Controllers bind JSON straight onto JPA entities, which makes two classes of bug possible.
 * These tests pin down the behaviour the API is supposed to have:
 *
 * <ul>
 *   <li>A request body must not be able to set fields the server owns, otherwise anyone can
 *       promote themselves to an admin account or mark themselves verified by posting JSON.</li>
 *   <li>A response must not serialise a lazy association. {@code spring.jpa.open-in-view} is
 *       false, so the persistence context is closed by the time Jackson runs; touching a lazy
 *       collection then throws and the endpoint answers 500.</li>
 * </ul>
 */
@SpringBootTest
class EntityExposureTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = new User.Builder()
                .setName("Jane Doe")
                .setEmail("jane-" + UUID.randomUUID() + "@example.com")
                .setPasswordHash("original-hash")
                .setRole(Role.STUDENT)
                .setVerified(false)
                .build();
        user.addAddress(new Address.Builder()
                .setLine1("12 Main Road")
                .setSuburb("Gardens")
                .setCity("Cape Town")
                .setProvince("Western Cape")
                .build());
        userId = userRepository.save(user).getId();
    }

    @Test
    @DisplayName("a request body cannot grant itself a role")
    void requestBody_cannotSetRole() {
        User bound = objectMapper.readValue(
                "{\"name\":\"Jane\",\"email\":\"j@example.com\",\"role\":\"ADMIN\"}", User.class);

        assertThat(bound.getRole())
                .as("role is assigned by the registration flow, never by the request body")
                .isNull();
    }

    @Test
    @DisplayName("a request body cannot mark itself verified")
    void requestBody_cannotSetVerified() {
        User bound = objectMapper.readValue(
                "{\"name\":\"Jane\",\"email\":\"j@example.com\",\"verified\":true}", User.class);

        assertThat(bound.isVerified())
                .as("verification happens through the emailed code only")
                .isFalse();
    }

    @Test
    @DisplayName("a request body cannot supply its own password hash")
    void requestBody_cannotSetPasswordHash() {
        User bound = objectMapper.readValue(
                "{\"name\":\"Jane\",\"email\":\"j@example.com\",\"passwordHash\":\"attacker-chosen\"}",
                User.class);

        assertThat(bound.getPasswordHash())
                .as("passwords only ever arrive as plaintext and get encoded by PasswordEncoder")
                .isNullOrEmpty();
    }

    @Test
    @DisplayName("a request body cannot backdate its own creation timestamp")
    void requestBody_cannotSetCreatedAt() {
        User bound = objectMapper.readValue(
                "{\"name\":\"Jane\",\"email\":\"j@example.com\",\"createdAt\":\"2001-01-01T00:00:00\"}",
                User.class);

        assertThat(bound.getCreatedAt())
                .as("createdAt is populated by the persistence layer")
                .isNull();
    }

    @Test
    @DisplayName("a stored user serialises without touching a lazy association")
    void serialisation_doesNotRequireAnOpenPersistenceContext() {
        // Deliberately no @Transactional: this mirrors a controller, which hands the entity to
        // Jackson long after the service transaction has committed and closed.
        User loaded = userRepository.findById(userId).orElseThrow();

        assertThatCode(() -> objectMapper.writeValueAsString(loaded))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a lazy association is never included in the payload")
    void lazyAssociation_isNotSerialised() {
        User loaded = userRepository.findById(userId).orElseThrow();

        String json = assertDoesNotThrowSerialising(loaded);

        assertThat(json)
                .as("addresses is lazy and must not be walked by Jackson")
                .doesNotContain("\"addresses\"");
    }

    private String assertDoesNotThrowSerialising(User user) {
        try {
            return objectMapper.writeValueAsString(user);
        } catch (Exception e) {
            throw new AssertionError("Serialising a user failed: " + e, e);
        }
    }
}