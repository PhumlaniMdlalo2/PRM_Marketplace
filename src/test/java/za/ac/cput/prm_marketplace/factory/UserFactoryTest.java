package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserFactoryTest {

    @Test
    void createsUserWithValidFields() {
        User user = UserFactory.createUser("Jane Doe", "jane@example.com", "hashed-password",
                Role.STUDENT, "0821234567", false, null);

        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("Jane Doe");
        assertThat(user.getEmail()).isEqualTo("jane@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
        assertThat(user.getPhone()).isEqualTo("0821234567");
        assertThat(user.isVerified()).isFalse();
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void returnsNullWhenNameIsBlankOrNull(String name) {
        User user = UserFactory.createUser(name, "jane@example.com", "hashed-password",
                Role.STUDENT, null, false, null);

        assertThat(user).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"not-an-email", "missing-domain@", "@missing-local.com", "no-at-sign.com"})
    void returnsNullWhenEmailIsInvalidOrNull(String email) {
        User user = UserFactory.createUser("Jane Doe", email, "hashed-password",
                Role.STUDENT, null, false, null);

        assertThat(user).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void returnsNullWhenPasswordHashIsBlankOrNull(String passwordHash) {
        User user = UserFactory.createUser("Jane Doe", "jane@example.com", passwordHash,
                Role.STUDENT, null, false, null);

        assertThat(user).isNull();
    }

    @Test
    void returnsNullWhenRoleIsNull() {
        User user = UserFactory.createUser("Jane Doe", "jane@example.com", "hashed-password",
                null, null, false, null);

        assertThat(user).isNull();
    }
}