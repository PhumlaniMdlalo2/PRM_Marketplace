package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VendorProfileTest {

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Vendor Owner")
                .setEmail("owner@example.com")
                .setPasswordHash("hash")
                .setRole(Role.VENDOR)
                .build();
    }

    @Test
    void builderSetsAllFields() {
        UUID id = UUID.randomUUID();
        User user = buildUser();
        LocalDateTime createdAt = LocalDateTime.now();

        VendorProfile profile = new VendorProfile.Builder()
                .setId(id)
                .setUser(user)
                .setBusinessName("Acme Repairs")
                .setRegistrationNo("REG123")
                .setVerified(true)
                .setRatingAvg(BigDecimal.valueOf(4.5))
                .setCreatedAt(createdAt)
                .build();

        assertThat(profile.getId()).isEqualTo(id);
        assertThat(profile.getUser()).isEqualTo(user);
        assertThat(profile.getBusinessName()).isEqualTo("Acme Repairs");
        assertThat(profile.getRegistrationNo()).isEqualTo("REG123");
        assertThat(profile.isVerified()).isTrue();
        assertThat(profile.getRatingAvg()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
        assertThat(profile.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void copyPreservesAllFields() {
        VendorProfile original = new VendorProfile.Builder()
                .setId(UUID.randomUUID())
                .setUser(buildUser())
                .setBusinessName("Acme Repairs")
                .setRegistrationNo("REG123")
                .setVerified(false)
                .setRatingAvg(BigDecimal.valueOf(3.2))
                .setCreatedAt(LocalDateTime.now())
                .build();

        VendorProfile copy = new VendorProfile.Builder().copy(original).build();

        assertThat(copy.getId()).isEqualTo(original.getId());
        assertThat(copy.getUser()).isEqualTo(original.getUser());
        assertThat(copy.getBusinessName()).isEqualTo(original.getBusinessName());
        assertThat(copy.getRegistrationNo()).isEqualTo(original.getRegistrationNo());
        assertThat(copy.isVerified()).isEqualTo(original.isVerified());
        assertThat(copy.getRatingAvg()).isEqualByComparingTo(original.getRatingAvg());
        assertThat(copy.getCreatedAt()).isEqualTo(original.getCreatedAt());
    }

    @Test
    void copyAllowsOverridingIndividualFields() {
        VendorProfile original = new VendorProfile.Builder()
                .setId(UUID.randomUUID())
                .setUser(buildUser())
                .setBusinessName("Acme Repairs")
                .setVerified(false)
                .build();

        VendorProfile updated = new VendorProfile.Builder().copy(original).setVerified(true).build();

        assertThat(updated.getId()).isEqualTo(original.getId());
        assertThat(updated.getBusinessName()).isEqualTo(original.getBusinessName());
        assertThat(updated.isVerified()).isTrue();
    }
}