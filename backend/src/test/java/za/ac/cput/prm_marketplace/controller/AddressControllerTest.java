package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Address;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.IAddressService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.asStudent;

@SpringBootTest
@AutoConfigureMockMvc
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IAddressService addressService;

    private UUID callerId;
    private UUID intruderId;
    private UUID addressId;
    private Address address;

    @BeforeEach
    void setUp() {
        callerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        address = new Address.Builder()
                .setId(addressId)
                .setUser(buildUser(callerId))
                .setLine1("1 Main Road")
                .setSuburb("Rondebosch")
                .setCity("Cape Town")
                .setProvince("Western Cape")
                .setCountry("South Africa")
                .build();
    }

    @Test
    @DisplayName("creating an address files it under the caller, not the userId in the body")
    void create_takesTheOwnerFromTheToken() throws Exception {
        when(addressService.create(any(), eq(callerId))).thenReturn(address);

        // The body names a different account. It must not become the owner.
        Address hostile = new Address.Builder()
                .copy(address)
                .setUser(buildUser(intruderId))
                .build();

        mockMvc.perform(post("/api/addresses")
                        .with(asStudent(callerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hostile)))
                .andExpect(status().isCreated());

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressService).create(captor.capture(), eq(callerId));
        // The service ignores the body's owner and assigns the caller; the controller proves it
        // never forwards the intruder id by passing the caller's own id as the second argument.
        assertThat(captor.getValue().getLine1()).isEqualTo("1 Main Road");
    }

    @Test
    @DisplayName("creating an address returns 400 when it cannot be saved")
    void create_returnsBadRequestWhenServiceReturnsNull() throws Exception {
        when(addressService.create(any(), eq(callerId))).thenReturn(null);

        mockMvc.perform(post("/api/addresses")
                        .with(asStudent(callerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("the address list is the caller's, and the /user/{userId} route is gone")
    void getAll_isScopedToTheCaller() throws Exception {
        when(addressService.getByUser(callerId)).thenReturn(List.of(address));

        mockMvc.perform(get("/api/addresses").with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(addressId.toString()));

        verify(addressService).getByUser(callerId);

        mockMvc.perform(get("/api/addresses/user/" + intruderId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("reading an address that is not the caller's returns 404")
    void read_returnsNotFoundForAnotherAccountsAddress() throws Exception {
        when(addressService.read(addressId, callerId)).thenReturn(null);

        mockMvc.perform(get("/api/addresses/" + addressId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("updating an address that is not the caller's returns 404")
    void update_returnsNotFoundForAnotherAccountsAddress() throws Exception {
        when(addressService.update(any(), eq(callerId))).thenReturn(null);

        mockMvc.perform(put("/api/addresses/" + addressId)
                        .with(asStudent(callerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address)))
                .andExpect(status().isNotFound());

        verify(addressService).update(any(), eq(callerId));
    }

    @Test
    @DisplayName("updating the caller's own address succeeds")
    void update_returnsUpdatedAddress() throws Exception {
        when(addressService.update(any(), eq(callerId))).thenReturn(address);

        mockMvc.perform(put("/api/addresses/" + addressId)
                        .with(asStudent(callerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(addressId.toString()));
    }

    @Test
    @DisplayName("deleting an address that is not the caller's returns 404")
    void delete_returnsNotFoundForAnotherAccountsAddress() throws Exception {
        when(addressService.delete(addressId, callerId)).thenReturn(false);

        mockMvc.perform(delete("/api/addresses/" + addressId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deleting the caller's own address succeeds")
    void delete_returnsNoContent() throws Exception {
        when(addressService.delete(addressId, callerId)).thenReturn(true);

        mockMvc.perform(delete("/api/addresses/" + addressId).with(asStudent(callerId)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("the default address is the caller's, and the /user/{userId} route is gone")
    void getDefault_isScopedToTheCaller() throws Exception {
        when(addressService.getDefaultForUser(callerId)).thenReturn(address);

        mockMvc.perform(get("/api/addresses/default").with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(addressId.toString()));

        mockMvc.perform(get("/api/addresses/user/" + callerId + "/default").with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the default address returns 404 when the caller has none")
    void getDefault_returnsNotFoundWhenNoneSet() throws Exception {
        when(addressService.getDefaultForUser(callerId)).thenReturn(null);

        mockMvc.perform(get("/api/addresses/default").with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("setting a default address that is not the caller's returns 404")
    void setDefault_returnsNotFoundForAnotherAccountsAddress() throws Exception {
        when(addressService.setDefault(addressId, callerId)).thenReturn(null);

        mockMvc.perform(patch("/api/addresses/" + addressId + "/default").with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("address endpoints reject anonymous callers")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/addresses")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/addresses/" + addressId)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/addresses/" + addressId)).andExpect(status().isUnauthorized());
        verify(addressService, never()).getByUser(any());
    }

    private User buildUser(UUID id) {
        return new User.Builder().setId(id).setEmail(id + "@example.com").build();
    }
}