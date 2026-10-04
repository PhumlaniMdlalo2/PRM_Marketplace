package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.repository.UserRepository;
import za.ac.cput.prm_marketplace.repository.VendorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorProfileServiceImplTest {

    @Mock
    private VendorProfileRepository vendorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VendorProfileServiceImpl vendorProfileService;

    private UUID sellerId;
    private User seller;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        seller = new User.Builder()
                .setId(sellerId)
                .setName("Vendor Owner")
                .setEmail("owner@example.com")
                .setPasswordHash("hash")
                .setRole(Role.VENDOR)
                .build();
    }

    private VendorProfile submitted() {
        return new VendorProfile.Builder()
                .setBusinessName("Acme Repairs")
                .setRegistrationNo("REG-1")
                .build();
    }

    private VendorProfile stored() {
        return new VendorProfile.Builder()
                .setId(UUID.randomUUID())
                .setUser(seller)
                .setBusinessName("Acme Repairs")
                .setRegistrationNo("REG-1")
                .setVerified(true)
                .setRatingAvg(new BigDecimal("4.50"))
                .build();
    }

    private void givenSellerExists() {
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));
    }

    // create

    @Test
    void createReturnsNullWhenProfileIsNull() {
        assertThat(vendorProfileService.create(null, sellerId, Role.VENDOR)).isNull();

        verifyNoInteractions(vendorProfileRepository, userRepository);
    }

    @Test
    @DisplayName("create refuses a caller whose id is missing")
    void createReturnsNullWhenCallerIsNull() {
        assertThat(vendorProfileService.create(submitted(), null, Role.VENDOR)).isNull();

        verifyNoInteractions(vendorProfileRepository, userRepository);
    }

    @Test
    @DisplayName("create saves a profile owned by the caller")
    void createSavesProfileForTheCaller() {
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(vendorProfileRepository.existsByUserId(sellerId)).thenReturn(false);
        when(vendorProfileRepository.save(any(VendorProfile.class)))
                .thenAnswer(call -> call.getArgument(0));

        VendorProfile result = vendorProfileService.create(submitted(), sellerId, Role.VENDOR);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isSameAs(seller);
        assertThat(result.getBusinessName()).isEqualTo("Acme Repairs");
    }

    @Test
    @DisplayName("create ignores a user supplied in the body")
    void createBodyUserIsIgnored() {
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(vendorProfileRepository.existsByUserId(sellerId)).thenReturn(false);
        when(vendorProfileRepository.save(any(VendorProfile.class)))
                .thenAnswer(call -> call.getArgument(0));

        User impostor = new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Someone Else")
                .setEmail("else@example.com")
                .setPasswordHash("hash")
                .setRole(Role.VENDOR)
                .build();
        VendorProfile hostile = new VendorProfile.Builder()
                .setUser(impostor)
                .setBusinessName("Acme Repairs")
                .build();

        VendorProfile result = vendorProfileService.create(hostile, sellerId, Role.VENDOR);

        assertThat(result.getUser()).isSameAs(seller);
    }

    @Test
    @DisplayName("create ignores a verified flag supplied in the body")
    void createBodyVerifiedIsIgnored() {
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(vendorProfileRepository.existsByUserId(sellerId)).thenReturn(false);
        when(vendorProfileRepository.save(any(VendorProfile.class)))
                .thenAnswer(call -> call.getArgument(0));

        VendorProfile hostile = new VendorProfile.Builder()
                .setUser(seller)
                .setBusinessName("Acme Repairs")
                .setVerified(true)
                .setRatingAvg(new BigDecimal("5.00"))
                .build();

        VendorProfile result = vendorProfileService.create(hostile, sellerId, Role.VENDOR);

        assertThat(result.isVerified()).isFalse();
        assertThat(result.getRatingAvg()).isNull();
    }

    @Test
    @DisplayName("create refuses an account that is not a vendor")
    void createNonVendorIsRefused() {
        assertThat(vendorProfileService.create(submitted(), sellerId, Role.STUDENT)).isNull();

        verifyNoInteractions(vendorProfileRepository, userRepository);
    }

    @Test
    @DisplayName("create refuses a second profile for the same account")
    void createSecondProfileIsRefused() {
        when(vendorProfileRepository.existsByUserId(sellerId)).thenReturn(true);

        assertThat(vendorProfileService.create(submitted(), sellerId, Role.VENDOR)).isNull();

        verify(vendorProfileRepository, never()).save(any(VendorProfile.class));
    }

    @Test
    @DisplayName("create refuses a blank business name")
    void createBlankBusinessNameIsRefused() {
        when(vendorProfileRepository.existsByUserId(sellerId)).thenReturn(false);
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));

        VendorProfile blank = new VendorProfile.Builder().setBusinessName("   ").build();

        assertThat(vendorProfileService.create(blank, sellerId, Role.VENDOR)).isNull();

        verify(vendorProfileRepository, never()).save(any(VendorProfile.class));
    }

    @Test
    @DisplayName("create returns null when the account behind the token is gone")
    void createMissingUserIsRefused() {
        when(vendorProfileRepository.existsByUserId(sellerId)).thenReturn(false);
        when(userRepository.findById(sellerId)).thenReturn(Optional.empty());

        assertThat(vendorProfileService.create(submitted(), sellerId, Role.VENDOR)).isNull();

        verify(vendorProfileRepository, never()).save(any(VendorProfile.class));
    }

    // read

    @Test
    void readReturnsNullWhenIdIsNull() {
        assertThat(vendorProfileService.read(null)).isNull();

        verifyNoInteractions(vendorProfileRepository);
    }

    @Test
    void readReturnsProfileWhenFound() {
        UUID id = UUID.randomUUID();
        VendorProfile profile = stored();
        when(vendorProfileRepository.findById(id)).thenReturn(Optional.of(profile));

        assertThat(vendorProfileService.read(id)).isEqualTo(profile);
    }

    @Test
    void readReturnsNullWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(vendorProfileRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(vendorProfileService.read(id)).isNull();
    }

    // update

    @Test
    void updateReturnsNullWhenProfileIsNull() {
        assertThat(vendorProfileService.update(UUID.randomUUID(), null, sellerId)).isNull();

        verifyNoInteractions(vendorProfileRepository);
    }

    @Test
    void updateReturnsNullWhenIdIsNull() {
        assertThat(vendorProfileService.update(null, submitted(), sellerId)).isNull();

        verifyNoInteractions(vendorProfileRepository);
    }

    @Test
    void updateReturnsNullWhenCallerIsNull() {
        assertThat(vendorProfileService.update(UUID.randomUUID(), submitted(), null)).isNull();

        verifyNoInteractions(vendorProfileRepository);
    }

    @Test
    @DisplayName("update refuses a profile belonging to another account")
    void updateSomebodyElsesProfileIsRefused() {
        UUID id = UUID.randomUUID();
        when(vendorProfileRepository.findByIdAndUserId(id, sellerId)).thenReturn(Optional.empty());

        assertThat(vendorProfileService.update(id, submitted(), sellerId)).isNull();

        verify(vendorProfileRepository, never()).save(any(VendorProfile.class));
    }

    @Test
    @DisplayName("update moves the two business fields on the caller's own profile")
    void updateAppliesTheEdit() {
        UUID id = UUID.randomUUID();
        VendorProfile existing = stored();
        when(vendorProfileRepository.findByIdAndUserId(id, sellerId)).thenReturn(Optional.of(existing));
        when(vendorProfileRepository.save(any(VendorProfile.class)))
                .thenAnswer(call -> call.getArgument(0));

        VendorProfile edited = new VendorProfile.Builder()
                .setBusinessName("Acme Repairs and Parts")
                .setRegistrationNo("REG-2")
                .build();

        VendorProfile result = vendorProfileService.update(id, edited, sellerId);

        assertThat(result.getBusinessName()).isEqualTo("Acme Repairs and Parts");
        assertThat(result.getRegistrationNo()).isEqualTo("REG-2");
    }

    @Test
    @DisplayName("update cannot grant or keep verification")
    void updateCannotChangeVerified() {
        UUID id = UUID.randomUUID();
        VendorProfile unverified = new VendorProfile.Builder()
                .setId(id)
                .setUser(seller)
                .setBusinessName("Acme Repairs")
                .setVerified(false)
                .build();
        when(vendorProfileRepository.findByIdAndUserId(id, sellerId))
                .thenReturn(Optional.of(unverified));
        when(vendorProfileRepository.save(any(VendorProfile.class)))
                .thenAnswer(call -> call.getArgument(0));

        VendorProfile hostile = new VendorProfile.Builder()
                .setBusinessName("Acme Repairs")
                .setVerified(true)
                .build();

        assertThat(vendorProfileService.update(id, hostile, sellerId).isVerified()).isFalse();
    }

    @Test
    @DisplayName("update cannot re-point the profile at a different owner")
    void updateCannotChangeOwner() {
        UUID id = UUID.randomUUID();
        VendorProfile existing = stored();
        when(vendorProfileRepository.findByIdAndUserId(id, sellerId)).thenReturn(Optional.of(existing));
        when(vendorProfileRepository.save(any(VendorProfile.class)))
                .thenAnswer(call -> call.getArgument(0));

        User impostor = new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Someone Else")
                .setEmail("else@example.com")
                .setPasswordHash("hash")
                .setRole(Role.VENDOR)
                .build();
        VendorProfile hostile = new VendorProfile.Builder()
                .setUser(impostor)
                .setBusinessName("Acme Repairs")
                .build();

        assertThat(vendorProfileService.update(id, hostile, sellerId).getUser()).isSameAs(seller);
    }

    @Test
    @DisplayName("update keeps the existing name when the body leaves it blank")
    void updateKeepsNameWhenBlank() {
        UUID id = UUID.randomUUID();
        VendorProfile existing = stored();
        when(vendorProfileRepository.findByIdAndUserId(id, sellerId)).thenReturn(Optional.of(existing));
        when(vendorProfileRepository.save(any(VendorProfile.class)))
                .thenAnswer(call -> call.getArgument(0));

        VendorProfile blank = new VendorProfile.Builder().setBusinessName("  ").build();

        assertThat(vendorProfileService.update(id, blank, sellerId).getBusinessName())
                .isEqualTo("Acme Repairs");
    }

    // getAll / findMine

    @Test
    void getAllReturnsAllProfiles() {
        List<VendorProfile> profiles = List.of(stored(), stored());
        when(vendorProfileRepository.findAll()).thenReturn(profiles);

        assertThat(vendorProfileService.getAll()).isEqualTo(profiles);
    }

    @Test
    void findMineReturnsTheCallersProfile() {
        VendorProfile profile = stored();
        when(vendorProfileRepository.findByUserId(sellerId)).thenReturn(Optional.of(profile));

        assertThat(vendorProfileService.findMine(sellerId)).isEqualTo(profile);
    }

    @Test
    void findMineReturnsNullWhenTheCallerHasNoProfile() {
        when(vendorProfileRepository.findByUserId(sellerId)).thenReturn(Optional.empty());

        assertThat(vendorProfileService.findMine(sellerId)).isNull();
    }

    @Test
    @DisplayName("findMine with a null caller returns null instead of throwing")
    void findMineNullCallerReturnsNull() {
        assertThat(vendorProfileService.findMine(null)).isNull();

        verifyNoInteractions(vendorProfileRepository);
    }
}