package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void builderSetsAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        User user = new User.Builder()
                .setId(id)
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hashed-password")
                .setRole(Role.STUDENT)
                .setPhone("0821234567")
                .setCreatedAt(createdAt)
                .setVerified(true)
                .setVendorProfile(null)
                .build();

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getName()).isEqualTo("Jane Doe");
        assertThat(user.getEmail()).isEqualTo("jane@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
        assertThat(user.getPhone()).isEqualTo("0821234567");
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        assertThat(user.isVerified()).isTrue();
        assertThat(user.getVendorProfile()).isNull();
    }

    @Test
    void copyPreservesAllFields() {
        User original = new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hashed-password")
                .setRole(Role.VENDOR)
                .setPhone("0821234567")
                .setCreatedAt(LocalDateTime.now())
                .setVerified(true)
                .build();

        User copy = new User.Builder().copy(original).build();

        assertThat(copy.getId()).isEqualTo(original.getId());
        assertThat(copy.getName()).isEqualTo(original.getName());
        assertThat(copy.getEmail()).isEqualTo(original.getEmail());
        assertThat(copy.getPasswordHash()).isEqualTo(original.getPasswordHash());
        assertThat(copy.getRole()).isEqualTo(original.getRole());
        assertThat(copy.getPhone()).isEqualTo(original.getPhone());
        assertThat(copy.getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(copy.isVerified()).isEqualTo(original.isVerified());
    }

    @Test
    void copyAllowsOverridingIndividualFields() {
        User original = new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hashed-password")
                .setRole(Role.STUDENT)
                .setVerified(false)
                .build();

        User updated = new User.Builder().copy(original).setVerified(true).build();

        assertThat(updated.getId()).isEqualTo(original.getId());
        assertThat(updated.getName()).isEqualTo(original.getName());
        assertThat(updated.isVerified()).isTrue();
    }
}