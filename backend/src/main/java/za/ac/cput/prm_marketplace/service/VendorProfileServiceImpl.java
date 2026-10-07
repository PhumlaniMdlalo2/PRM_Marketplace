package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.dto.SellerPayoutDetails;
import za.ac.cput.prm_marketplace.repository.UserRepository;
import za.ac.cput.prm_marketplace.repository.VendorProfileRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class VendorProfileServiceImpl implements IVendorProfileService {

    private final VendorProfileRepository vendorProfileRepository;
    private final UserRepository userRepository;

    public VendorProfileServiceImpl(VendorProfileRepository vendorProfileRepository,
                                    UserRepository userRepository) {
        this.vendorProfileRepository = vendorProfileRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public VendorProfile create(VendorProfile profile, UUID requesterId, Role requesterRole) {
        if (profile == null || requesterId == null) {
            return null;
        }

        // Vendors get a profile during signup; students may apply later without losing their
        // student role. Community and admin accounts cannot create seller profiles.
        if (requesterRole != Role.VENDOR && requesterRole != Role.STUDENT) {
            return null;
        }

        // One profile per account: the column is unique, and failing here gives a clear rejection
        // instead of a constraint violation surfacing as a 500.
        if (vendorProfileRepository.existsByUserId(requesterId)) {
            return null;
        }

        User owner = userRepository.findById(requesterId).orElse(null);
        if (owner == null || profile.getBusinessName() == null || profile.getBusinessName().isBlank()) {
            return null;
        }

        // Everything the caller sent except the business details is discarded. verified and
        // ratingAvg in particular: a seller who could post verified:true would be marking
        // themselves approved without admin review.
        VendorProfile created = new VendorProfile.Builder()
                .setUser(owner)
                .setBusinessName(profile.getBusinessName().trim())
                .setRegistrationNo(profile.getRegistrationNo())
                .setVerified(false)
                .setRatingAvg(null)
                .build();

        return vendorProfileRepository.save(created);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorProfile read(UUID id) {
        if (id == null) {
            return null;
        }
        return vendorProfileRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public VendorProfile update(UUID id, VendorProfile profile, UUID requesterId) {
        if (id == null || profile == null || requesterId == null) {
            return null;
        }

        VendorProfile existing = vendorProfileRepository.findByIdAndUserId(id, requesterId).orElse(null);
        if (existing == null) {
            return null;
        }

        // Rebuilt from the stored row so only the two editable fields can move. The owner's id and
        // the verified flag are re-applied from what is already in the database, which is what
        // stops an edit being used to grant or keep verification.
        String businessName = profile.getBusinessName() != null && !profile.getBusinessName().isBlank()
                ? profile.getBusinessName().trim()
                : existing.getBusinessName();
        String registrationNo = profile.getRegistrationNo() != null
                ? normaliseOptional(profile.getRegistrationNo())
                : existing.getRegistrationNo();
        boolean sellerDetailsChanged = !Objects.equals(existing.getBusinessName(), businessName)
                || !Objects.equals(existing.getRegistrationNo(), registrationNo);

        VendorProfile updated = new VendorProfile.Builder()
                .copy(existing)
                .setBusinessName(businessName)
                .setRegistrationNo(registrationNo)
                .setVerified(existing.isVerified() && !sellerDetailsChanged)
                .build();

        return vendorProfileRepository.save(updated);
    }

    @Override
    @Transactional
    public VendorProfile verify(UUID id, boolean verified, UUID requesterId, Role requesterRole) {
        // Checked before anything is read, so a non-moderator gets the same answer whether or not
        // the profile exists and cannot use this to probe the seller directory.
        if (requesterRole != Role.ADMIN || id == null) {
            return null;
        }

        VendorProfile existing = vendorProfileRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        if (existing.isVerified() == verified) {
            // Nothing to do. Returning the stored row avoids a pointless write, and makes a repeat
            // call idempotent rather than bumping updatedAt on a table that has no such column.
            return existing;
        }

        // The rating is carried over untouched. Verification and reputation are separate signals and
        // conflating them would mean verifying a seller silently rewrote their score.
        return vendorProfileRepository.save(new VendorProfile.Builder()
                .copy(existing)
                .setVerified(verified)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorProfile> getAll() {
        return vendorProfileRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public VendorProfile findMine(UUID requesterId) {
        if (requesterId == null) {
            return null;
        }
        return vendorProfileRepository.findByUserId(requesterId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public SellerPayoutDetails getMyPayoutDetails(UUID requesterId) {
        if (requesterId == null) {
            return null;
        }
        VendorProfile profile = vendorProfileRepository.findByUserId(requesterId).orElse(null);
        return profile == null ? null : payoutDetails(profile);
    }

    @Override
    @Transactional
    public SellerPayoutDetails updateMyPayoutDetails(UUID requesterId, SellerPayoutDetails details) {
        if (requesterId == null || !isValidPayoutDetails(details)) {
            return null;
        }
        VendorProfile profile = vendorProfileRepository.findByUserId(requesterId).orElse(null);
        if (profile == null) {
            return null;
        }
        VendorProfile updated = new VendorProfile.Builder()
                .copy(profile)
                .setPayoutDetails(trim(details.accountHolder()), trim(details.bankName()),
                        trim(details.accountNumber()), trim(details.branchCode()), trim(details.accountType()))
                .build();
        return payoutDetails(vendorProfileRepository.save(updated));
    }

    private static SellerPayoutDetails payoutDetails(VendorProfile profile) {
        return new SellerPayoutDetails(profile.getPayoutAccountHolder(), profile.getPayoutBankName(),
                profile.getPayoutAccountNumber(), profile.getPayoutBranchCode(), profile.getPayoutAccountType());
    }

    private static boolean isValidPayoutDetails(SellerPayoutDetails details) {
        return details != null
                && hasText(details.accountHolder(), 120)
                && hasText(details.bankName(), 120)
                && details.accountNumber() != null
                && details.accountNumber().trim().matches("[0-9]{6,20}")
                && details.branchCode() != null
                && details.branchCode().trim().matches("[0-9]{4,10}")
                && hasText(details.accountType(), 40);
    }

    private static boolean hasText(String value, int maxLength) {
        return value != null && !value.isBlank() && value.trim().length() <= maxLength;
    }

    private static String trim(String value) {
        return value.trim();
    }

    private static String normaliseOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}