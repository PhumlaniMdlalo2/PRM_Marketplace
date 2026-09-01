package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VendorProfileFactoryTest {

    private User buildUser() {
        return UserFactory.createUser("Vendor Owner", "owner@example.com", "hashed-password",
                Role.VENDOR, null, false, null);
    }

    @Test
    void createsVendorProfileWithValidFields() {
        User user = buildUser();

        VendorProfile profile = VendorProfileFactory.createVendorProfile(user, "Acme Repairs", "REG123",
                true, BigDecimal.valueOf(4.5));

        assertThat(profile).isNotNull();
        assertThat(profile.getUser()).isEqualTo(user);
        assertThat(profile.getBusinessName()).isEqualTo("Acme Repairs");
        assertThat(profile.getRegistrationNo()).isEqualTo("REG123");
        assertThat(profile.isVerified()).isTrue();
        assertThat(profile.getRatingAvg()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
        assertThat(profile.getCreatedAt()).isNotNull();
    }

    @Test
    void returnsNullWhenUserIsNull() {
        VendorProfile profile = VendorProfileFactory.createVendorProfile(null, "Acme Repairs", "REG123",
                false, null);

        assertThat(profile).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void returnsNullWhenBusinessNameIsBlankOrNull(String businessName) {
        VendorProfile profile = VendorProfileFactory.createVendorProfile(buildUser(), businessName, "REG123",
                false, null);

        assertThat(profile).isNull();
    }
}