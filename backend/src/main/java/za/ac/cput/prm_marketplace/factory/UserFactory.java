package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class UserFactory {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public static User createUser(String name, String email, String passwordHash, Role role,
                                   String phone, boolean verified, VendorProfile vendorProfile) {
        if (name == null || name.isBlank()) {
            return null;
        }
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            return null;
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            return null;
        }
        if (role == null) {
            return null;
        }

        return new User.Builder()
                .setName(name)
                .setEmail(email)
                .setPasswordHash(passwordHash)
                .setRole(role)
                .setPhone(phone)
                .setCreatedAt(LocalDateTime.now())
                .setVerified(verified)
                .setVendorProfile(vendorProfile)
                .build();
    }
}
