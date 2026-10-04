package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.AddressRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    private AddressServiceImpl service;

    private UUID ownerId;
    private UUID intruderId;
    private UUID addressId;

    @BeforeEach
    void setUp() {
        service = new AddressServiceImpl(addressRepository, userRepository);
        ownerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        addressId = UUID.randomUUID();
    }

    @Test
    @DisplayName("create assigns the caller as owner and ignores any owner in the body")
    void create_takesOwnerFromTheRequester() {
        User owner = buildUser(ownerId);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(addressRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Address submitted = new Address.Builder()
                .setId(UUID.randomUUID())
                .setUser(buildUser(intruderId))
                .setLine1("1 Main Road")
                .setSuburb("Rondebosch")
                .setCity("Cape Town")
                .setProvince("Western Cape")
                .build();

        Address created = service.create(submitted, ownerId);

        assertThat(created).isNotNull();
        assertThat(created.getUser().getId()).isEqualTo(ownerId);
        // A client-supplied id must not turn the insert into an overwrite.
        assertThat(created.getId()).isNull();
    }

    @Test
    @DisplayName("create returns null when the caller has no user row")
    void create_unknownRequesterReturnsNull() {
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThat(service.create(buildAddress(ownerId), ownerId)).isNull();
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("create returns null for a null payload or requester")
    void create_rejectsNulls() {
        assertThat(service.create(null, ownerId)).isNull();
        assertThat(service.create(buildAddress(ownerId), null)).isNull();
        verifyNoInteractions(addressRepository);
    }

    @Test
    @DisplayName("create demotes the caller's existing default when a new default is added")
    void create_newDefaultDemotesTheOldOne() {
        Address existingDefault = buildAddress(ownerId);
        existingDefault.setDefaultAddress(true);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(buildUser(ownerId)));
        when(addressRepository.findByUserId(ownerId)).thenReturn(List.of(existingDefault));
        when(addressRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Address submitted = new Address.Builder()
                .copy(buildAddress(ownerId))
                .setDefaultAddress(true)
                .build();

        service.create(submitted, ownerId);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .filteredOn(Address::isDefaultAddress)
                .hasSize(1);
    }

    @Test
    @DisplayName("read returns the caller's own address")
    void read_ownedAddressIsReturned() {
        Address address = buildAddress(ownerId);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        assertThat(service.read(addressId, ownerId)).isSameAs(address);
    }

    @Test
    @DisplayName("read hides an address that belongs to somebody else")
    void read_foreignAddressIsHidden() {
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(buildAddress(intruderId)));

        assertThat(service.read(addressId, ownerId)).isNull();
    }

    @Test
    @DisplayName("read hides an address that has no owner recorded")
    void read_ownerlessAddressIsHidden() {
        Address ownerless = new Address.Builder().setId(addressId).build();
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(ownerless));

        assertThat(service.read(addressId, ownerId)).isNull();
    }

    @Test
    @DisplayName("read returns null for a missing address or a null id")
    void read_missingReturnsNull() {
        when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

        assertThat(service.read(addressId, ownerId)).isNull();
        assertThat(service.read(null, ownerId)).isNull();
    }

    @Test
    @DisplayName("update saves the caller's address but keeps the stored owner and timestamp")
    void update_preservesStoredOwnership() {
        Address stored = buildAddress(ownerId);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(stored));
        when(addressRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Address body = new Address.Builder()
                .copy(buildAddress(intruderId))
                .setLine1("99 Attacker Way")
                .build();

        Address updated = service.update(body, ownerId);

        assertThat(updated).isNotNull();
        assertThat(updated.getUser().getId()).isEqualTo(ownerId);
        assertThat(updated.getLine1()).isEqualTo("99 Attacker Way");
    }

    @Test
    @DisplayName("update refuses to touch another account's address")
    void update_foreignAddressIsRefused() {
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(buildAddress(intruderId)));

        assertThat(service.update(buildAddress(intruderId), ownerId)).isNull();
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("update returns null without an id")
    void update_withoutIdReturnsNull() {
        assertThat(service.update(buildAddress(ownerId), ownerId)).isNull();
        assertThat(service.update(null, ownerId)).isNull();
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete removes the caller's own address")
    void delete_ownedAddressIsRemoved() {
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(buildAddress(ownerId)));

        assertThat(service.delete(addressId, ownerId)).isTrue();
        verify(addressRepository).deleteById(addressId);
    }

    @Test
    @DisplayName("delete refuses to remove another account's address")
    void delete_foreignAddressIsRefused() {
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(buildAddress(intruderId)));

        assertThat(service.delete(addressId, ownerId)).isFalse();
        verify(addressRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete returns false for a missing address")
    void delete_missingReturnsFalse() {
        when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

        assertThat(service.delete(addressId, ownerId)).isFalse();
        verify(addressRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("getByUser returns only the rows for the given account")
    void getByUser_scopesToTheRequester() {
        Address address = buildAddress(ownerId);
        when(addressRepository.findByUserId(ownerId)).thenReturn(List.of(address));

        assertThat(service.getByUser(ownerId)).containsExactly(address);
        assertThat(service.getByUser(null)).isEmpty();
    }

    @Test
    @DisplayName("getDefaultForUser picks the caller's default address")
    void getDefaultForUser_scopesToTheRequester() {
        Address address = buildAddress(ownerId);
        when(addressRepository.findByUserIdAndDefaultAddressTrue(ownerId))
                .thenReturn(List.of(address));

        assertThat(service.getDefaultForUser(ownerId)).isSameAs(address);
        assertThat(service.getDefaultForUser(null)).isNull();
    }

    @Test
    @DisplayName("getDefaultForUser returns null when the caller has no default")
    void getDefaultForUser_noneReturnsNull() {
        when(addressRepository.findByUserIdAndDefaultAddressTrue(ownerId)).thenReturn(List.of());

        assertThat(service.getDefaultForUser(ownerId)).isNull();
    }

    @Test
    @DisplayName("setDefault promotes the target and demotes the caller's other addresses")
    void setDefault_promotesAndDemotes() {
        Address target = buildAddress(ownerId);
        Address other = buildAddress(ownerId);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(target));
        when(addressRepository.findByUserId(ownerId)).thenReturn(List.of(target, other));
        when(addressRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Address result = service.setDefault(addressId, ownerId);

        assertThat(result).isSameAs(target);
        assertThat(target.isDefaultAddress()).isTrue();
        assertThat(other.isDefaultAddress()).isFalse();
    }

    @Test
    @DisplayName("setDefault refuses to promote another account's address")
    void setDefault_foreignAddressIsRefused() {
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(buildAddress(intruderId)));

        assertThat(service.setDefault(addressId, ownerId)).isNull();
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("setDefault does not rewrite an address that is already the default")
    void setDefault_alreadyDefaultDoesNotResave() {
        Address target = buildAddress(ownerId);
        target.setDefaultAddress(true);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(target));

        assertThat(service.setDefault(addressId, ownerId)).isSameAs(target);
        verify(addressRepository, never()).save(any());
    }

    private User buildUser(UUID id) {
        return new User.Builder().setId(id).setEmail(id + "@example.com").build();
    }

    private Address buildAddress(UUID ownerId) {
        return new Address.Builder()
                .setId(addressId)
                .setUser(buildUser(ownerId))
                .setLine1("1 Main Road")
                .setSuburb("Rondebosch")
                .setCity("Cape Town")
                .setProvince("Western Cape")
                .setCountry("South Africa")
                .build();
    }
}