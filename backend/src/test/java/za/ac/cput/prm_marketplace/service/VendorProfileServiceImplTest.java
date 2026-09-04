package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.repository.VendorProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private VendorProfileServiceImpl vendorProfileService;

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Vendor Owner")
                .setEmail("owner@example.com")
                .setPasswordHash("hash")
                .setRole(Role.VENDOR)
                .build();
    }

    private VendorProfile buildProfile(UUID id) {
        return new VendorProfile.Builder()
                .setId(id)
                .setUser(buildUser())
                .setBusinessName("Acme Repairs")
                .build();
    }

    @Test
    void createReturnsNullWhenProfileIsNull() {
        VendorProfile result = vendorProfileService.create(null);

        assertThat(result).isNull();
        verifyNoInteractions(vendorProfileRepository);
    }

    @Test
    void createSavesAndReturnsProfile() {
        VendorProfile profile = buildProfile(null);
        VendorProfile saved = buildProfile(UUID.randomUUID());
        when(vendorProfileRepository.save(profile)).thenReturn(saved);

        VendorProfile result = vendorProfileService.create(profile);

        assertThat(result).isEqualTo(saved);
        verify(vendorProfileRepository).save(profile);
    }

    @Test
    void readReturnsNullWhenIdIsNull() {
        VendorProfile result = vendorProfileService.read(null);

        assertThat(result).isNull();
        verifyNoInteractions(vendorProfileRepository);
    }

    @Test
    void readReturnsProfileWhenFound() {
        UUID id = UUID.randomUUID();
        VendorProfile profile = buildProfile(id);
        when(vendorProfileRepository.findById(id)).thenReturn(Optional.of(profile));

        VendorProfile result = vendorProfileService.read(id);

        assertThat(result).isEqualTo(profile);
    }

    @Test
    void readReturnsNullWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(vendorProfileRepository.findById(id)).thenReturn(Optional.empty());

        VendorProfile result = vendorProfileService.read(id);

        assertThat(result).isNull();
    }

    @Test
    void updateReturnsNullWhenProfileIsNull() {
        VendorProfile result = vendorProfileService.update(null);

        assertThat(result).isNull();
        verifyNoInteractions(vendorProfileRepository);
    }

    @Test
    void updateReturnsNullWhenIdIsNull() {
        VendorProfile profile = buildProfile(null);

        VendorProfile result = vendorProfileService.update(profile);

        assertThat(result).isNull();
        verifyNoInteractions(vendorProfileRepository);
    }

    @Test
    void updateReturnsNullWhenProfileDoesNotExist() {
        UUID id = UUID.randomUUID();
        VendorProfile profile = buildProfile(id);
        when(vendorProfileRepository.existsById(id)).thenReturn(false);

        VendorProfile result = vendorProfileService.update(profile);

        assertThat(result).isNull();
        verify(vendorProfileRepository, never()).save(any());
    }

    @Test
    void updateSavesAndReturnsProfileWhenExists() {
        UUID id = UUID.randomUUID();
        VendorProfile profile = buildProfile(id);
        when(vendorProfileRepository.existsById(id)).thenReturn(true);
        when(vendorProfileRepository.save(profile)).thenReturn(profile);

        VendorProfile result = vendorProfileService.update(profile);

        assertThat(result).isEqualTo(profile);
        verify(vendorProfileRepository).save(profile);
    }

    @Test
    void deleteReturnsFalseWhenIdIsNull() {
        boolean result = vendorProfileService.delete(null);

        assertThat(result).isFalse();
        verifyNoInteractions(vendorProfileRepository);
    }

    @Test
    void deleteReturnsFalseWhenProfileDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(vendorProfileRepository.existsById(id)).thenReturn(false);

        boolean result = vendorProfileService.delete(id);

        assertThat(result).isFalse();
        verify(vendorProfileRepository, never()).deleteById(any());
    }

    @Test
    void deleteReturnsTrueAndDeletesWhenExists() {
        UUID id = UUID.randomUUID();
        when(vendorProfileRepository.existsById(id)).thenReturn(true);

        boolean result = vendorProfileService.delete(id);

        assertThat(result).isTrue();
        verify(vendorProfileRepository).deleteById(id);
    }

    @Test
    void getAllReturnsAllProfiles() {
        List<VendorProfile> profiles = List.of(buildProfile(UUID.randomUUID()), buildProfile(UUID.randomUUID()));
        when(vendorProfileRepository.findAll()).thenReturn(profiles);

        List<VendorProfile> result = vendorProfileService.getAll();

        assertThat(result).isEqualTo(profiles);
    }

    @Test
    void findByUserIdDelegatesToRepository() {
        UUID userId = UUID.randomUUID();
        VendorProfile profile = buildProfile(UUID.randomUUID());
        when(vendorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        Optional<VendorProfile> result = vendorProfileService.findByUserId(userId);

        assertThat(result).contains(profile);
    }
}