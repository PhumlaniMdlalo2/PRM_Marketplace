package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.AddressRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Resident")
                .setEmail("resident@example.com")
                .setPasswordHash("hash")
                .build();
    }

    private Address buildAddress(User owner, boolean isDefault) {
        return new Address.Builder()
                .setId(UUID.randomUUID())
                .setUser(owner)
                .setLine1("12 Main Road")
                .setCity("Cape Town")
                .setDefaultAddress(isDefault)
                .build();
    }

    @Test
    @DisplayName("create persists the address")
    void create_saves() {
        Address address = buildAddress(buildUser(), false);
        when(addressRepository.save(address)).thenReturn(address);

        assertThat(addressService.create(address)).isSameAs(address);
    }

    @Test
    @DisplayName("read missing address returns null")
    void read_missing_returnsNull() {
        UUID id = UUID.randomUUID();
        when(addressRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(addressService.read(id)).isNull();
    }

    @Test
    @DisplayName("update requires an existing address")
    void update_missing_returnsNull() {
        Address address = buildAddress(buildUser(), false);
        when(addressRepository.existsById(address.getId())).thenReturn(false);

        assertThat(addressService.update(address)).isNull();
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete reports false for an unknown address")
    void delete_missing_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(addressRepository.existsById(id)).thenReturn(false);

        assertThat(addressService.delete(id)).isFalse();
    }

    @Test
    @DisplayName("getByUser with null id returns empty")
    void getByUser_withNull_returnsEmpty() {
        assertThat(addressService.getByUser(null)).isEmpty();
    }

    @Test
    @DisplayName("getDefaultForUser returns the first default address")
    void getDefaultForUser_returnsFirstDefault() {
        UUID userId = UUID.randomUUID();
        Address preferred = buildAddress(buildUser(), true);
        when(addressRepository.findByUserIdAndDefaultAddressTrue(userId))
                .thenReturn(List.of(preferred));

        assertThat(addressService.getDefaultForUser(userId)).isSameAs(preferred);
    }

    @Test
    @DisplayName("getDefaultForUser returns null when the user has no default")
    void getDefaultForUser_none_returnsNull() {
        UUID userId = UUID.randomUUID();
        when(addressRepository.findByUserIdAndDefaultAddressTrue(userId)).thenReturn(List.of());

        assertThat(addressService.getDefaultForUser(userId)).isNull();
    }

    @Test
    @DisplayName("setDefault promotes the target and demotes the others")
    void setDefault_promotesTargetAndDemotesOthers() {
        User owner = buildUser();
        Address oldDefault = buildAddress(owner, true);
        Address other = buildAddress(owner, false);
        Address target = buildAddress(owner, false);

        when(addressRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(addressRepository.findByUserId(owner.getId()))
                .thenReturn(List.of(oldDefault, other, target));

        Address result = addressService.setDefault(target.getId());

        assertThat(result).isSameAs(target);
        assertThat(target.isDefaultAddress()).isTrue();
        assertThat(oldDefault.isDefaultAddress()).isFalse();
        assertThat(other.isDefaultAddress()).isFalse();
        verify(addressRepository).save(target);
        verify(addressRepository).save(oldDefault);
    }

    @Test
    @DisplayName("setDefault only writes the addresses that actually changed")
    void setDefault_skipsUnchangedAddresses() {
        User owner = buildUser();
        Address alreadyDefault = buildAddress(owner, true);
        Address target = alreadyDefault;

        when(addressRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(addressRepository.findByUserId(owner.getId())).thenReturn(List.of(target));

        assertThat(addressService.setDefault(target.getId())).isSameAs(target);
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("setDefault returns null for an unknown address")
    void setDefault_missing_returnsNull() {
        UUID id = UUID.randomUUID();
        when(addressRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(addressService.setDefault(id)).isNull();
    }

    @Test
    @DisplayName("setDefault returns null when the address has no owner")
    void setDefault_withoutOwner_returnsNull() {
        Address orphan = new Address.Builder()
                .setId(UUID.randomUUID())
                .setLine1("12 Main Road")
                .build();
        when(addressRepository.findById(orphan.getId())).thenReturn(Optional.of(orphan));

        assertThat(addressService.setDefault(orphan.getId())).isNull();
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteForUser removes an address owned by that user")
    void deleteForUser_owned_deletes() {
        UUID userId = UUID.randomUUID();
        User owner = new User.Builder()
                .setId(userId)
                .setName("Resident")
                .setEmail("resident@example.com")
                .setPasswordHash("hash")
                .build();
        Address address = buildAddress(owner, false);

        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));

        assertThat(addressService.deleteForUser(address.getId(), userId)).isTrue();
        verify(addressRepository).deleteById(address.getId());
    }

    @Test
    @DisplayName("deleteForUser refuses addresses belonging to somebody else")
    void deleteForUser_notOwned_refuses() {
        Address address = buildAddress(buildUser(), false);
        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));

        assertThat(addressService.deleteForUser(address.getId(), UUID.randomUUID())).isFalse();
        verify(addressRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteForUser returns false for an unknown address")
    void deleteForUser_missing_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(addressRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(addressService.deleteForUser(id, UUID.randomUUID())).isFalse();
    }
}
