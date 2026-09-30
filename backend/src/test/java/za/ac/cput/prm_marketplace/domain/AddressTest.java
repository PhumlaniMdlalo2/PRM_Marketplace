package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AddressTest {

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .build();
    }

    private Address.Builder builder() {
        return new Address.Builder()
                .setLine1("12 Main Road")
                .setSuburb("Observatory")
                .setCity("Cape Town")
                .setProvince("Western Cape")
                .setPostalCode("8001")
                .setCountry("South Africa");
    }

    @Test
    void builderSetsAllFields() {
        UUID id = UUID.randomUUID();
        User user = buildUser();

        Address address = builder()
                .setId(id)
                .setUser(user)
                .setLine2("Flat 3")
                .setDefaultAddress(true)
                .build();

        assertThat(address.getId()).isEqualTo(id);
        assertThat(address.getUser()).isEqualTo(user);
        assertThat(address.getLine1()).isEqualTo("12 Main Road");
        assertThat(address.getLine2()).isEqualTo("Flat 3");
        assertThat(address.getSuburb()).isEqualTo("Observatory");
        assertThat(address.getCity()).isEqualTo("Cape Town");
        assertThat(address.getProvince()).isEqualTo("Western Cape");
        assertThat(address.getPostalCode()).isEqualTo("8001");
        assertThat(address.getCountry()).isEqualTo("South Africa");
        assertThat(address.isDefaultAddress()).isTrue();
    }

    @Test
    void defaultsToNotBeingTheDefaultAddress() {
        Address address = builder().build();

        assertThat(address.isDefaultAddress()).isFalse();
    }

    @Test
    void getSingleLineJoinsThePresentParts() {
        Address address = builder().setLine2("Flat 3").build();

        assertThat(address.getSingleLine())
                .isEqualTo("12 Main Road, Flat 3, Observatory, Cape Town, 8001, Western Cape");
    }

    @Test
    void getSingleLineOmitsMissingLine2() {
        Address address = builder().build();

        assertThat(address.getSingleLine())
                .isEqualTo("12 Main Road, Observatory, Cape Town, 8001, Western Cape")
                .doesNotContain("null");
    }

    @Test
    void setDefaultAddressFlipsTheFlag() {
        Address address = builder().build();

        address.setDefaultAddress(true);
        assertThat(address.isDefaultAddress()).isTrue();

        address.setDefaultAddress(false);
        assertThat(address.isDefaultAddress()).isFalse();
    }

    @Test
    void copyPreservesAllFieldsAndAllowsOverrides() {
        Address original = builder()
                .setId(UUID.randomUUID())
                .setUser(buildUser())
                .setLine2("Flat 3")
                .setDefaultAddress(true)
                .build();

        Address copy = new Address.Builder().copy(original).build();
        assertThat(copy.getId()).isEqualTo(original.getId());
        assertThat(copy.getUser()).isEqualTo(original.getUser());
        assertThat(copy.getLine2()).isEqualTo("Flat 3");
        assertThat(copy.isDefaultAddress()).isTrue();

        Address updated = new Address.Builder().copy(original).setCity("Durban").build();
        assertThat(updated.getCity()).isEqualTo("Durban");
        assertThat(updated.getProvince()).isEqualTo(original.getProvince());
    }

    @Test
    void belongsToTheUserAndAppearsInTheirAddressList() {
        User user = buildUser();
        Address address = builder().setUser(user).build();
        user.addAddress(address);

        assertThat(address.getUser()).isEqualTo(user);
        assertThat(user.getAddresses()).contains(address);

        user.removeAddress(address);
        assertThat(user.getAddresses()).doesNotContain(address);
    }
}
