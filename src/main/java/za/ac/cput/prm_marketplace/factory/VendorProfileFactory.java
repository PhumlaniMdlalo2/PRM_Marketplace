package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VendorProfileFactory {

    public static VendorProfile createVendorProfile(User user, String businessName, String registrationNo,
                                                      boolean verified, BigDecimal ratingAvg) {
        if (user == null) {
            return null;
        }
        if (businessName == null || businessName.isBlank()) {
            return null;
        }

        return new VendorProfile.Builder()
                .setUser(user)
                .setBusinessName(businessName)
                .setRegistrationNo(registrationNo)
                .setVerified(verified)
                .setRatingAvg(ratingAvg)
                .setCreatedAt(LocalDateTime.now())
                .build();
    }
}
