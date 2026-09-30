package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

@WebMvcTest(AddressController.class)
@AutoConfigureMockMvc(addFilters = false)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IAddressService addressService;

    private UUID id;
    private UUID userId;
    private Address address;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        userId = UUID.randomUUID();
        address = buildAddress();
    }

    private Address buildAddress() {
        return new Address.Builder()
                .setId(id)
                .setUser(new User.Builder()
                        .setId(userId)
                        .setName("Resident")
                        .setEmail("resident@example.com")
                        .setPasswordHash("hash")
                        .build())
                .setLine1("12 Main Road")
                .setCity("Cape Town")
                .setProvince("Western Cape")
                .setPostalCode("8001")
                .setCountry("South Africa")
                .setSuburb("Observatory")
                .build();
    }

    @Test
    @DisplayName("create returns 201 with the created address")
    void create_returnsCreated() throws Exception {
        when(addressService.create(any(Address.class))).thenReturn(address);

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.line1").value("12 Main Road"))
                .andExpect(jsonPath("$.city").value("Cape Town"));
    }

    @Test
    @DisplayName("create returns 400 when the service refuses")
    void create_returnsBadRequest() throws Exception {
        when(addressService.create(any(Address.class))).thenReturn(null);

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create never leaks the owner back to the client")
    void create_doesNotEchoOwner() throws Exception {
        when(addressService.create(any(Address.class))).thenReturn(address);

        String body = mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("passwordHash").doesNotContain("\"user\"");
    }

    @Test
    @DisplayName("read returns the address")
    void read_returnsAddress() throws Exception {
        when(addressService.read(id)).thenReturn(address);

        mockMvc.perform(get("/api/addresses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("read returns 404 for an unknown address")
    void read_returnsNotFound() throws Exception {
        when(addressService.read(id)).thenReturn(null);

        mockMvc.perform(get("/api/addresses/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("read with a non-UUID id returns 400")
    void read_withMalformedId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/addresses/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("update forces the path id onto the entity")
    void update_usesPathId() throws Exception {
        when(addressService.read(id)).thenReturn(address);
        when(addressService.update(any(Address.class))).thenReturn(address);

        mockMvc.perform(put("/api/addresses/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address)))
                .andExpect(status().isOk());

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressService).update(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("update returns 404 when the address is unknown")
    void update_returnsNotFound() throws Exception {
        when(addressService.read(id)).thenReturn(null);

        mockMvc.perform(put("/api/addresses/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address)))
                .andExpect(status().isNotFound());

        verify(addressService, never()).update(any());
    }

    @Test
    @DisplayName("delete returns 204 on success")
    void delete_returnsNoContent() throws Exception {
        when(addressService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/api/addresses/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete returns 404 when the address is unknown")
    void delete_returnsNotFound() throws Exception {
        when(addressService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/api/addresses/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getAll returns every address")
    void getAll_returnsList() throws Exception {
        when(addressService.getAll()).thenReturn(List.of(address));

        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("getByUser returns the user's addresses")
    void getByUser_returnsList() throws Exception {
        when(addressService.getByUser(userId)).thenReturn(List.of(address));

        mockMvc.perform(get("/api/addresses/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("getDefault returns the default address")
    void getDefault_returnsAddress() throws Exception {
        when(addressService.getDefaultForUser(userId)).thenReturn(address);

        mockMvc.perform(get("/api/addresses/user/{userId}/default", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("getDefault returns 404 when the user has no default")
    void getDefault_returnsNotFound() throws Exception {
        when(addressService.getDefaultForUser(userId)).thenReturn(null);

        mockMvc.perform(get("/api/addresses/user/{userId}/default", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("setDefault promotes an address")
    void setDefault_returnsAddress() throws Exception {
        when(addressService.setDefault(id)).thenReturn(address);

        mockMvc.perform(patch("/api/addresses/{id}/default", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("setDefault returns 404 for an unknown address")
    void setDefault_returnsNotFound() throws Exception {
        when(addressService.setDefault(id)).thenReturn(null);

        mockMvc.perform(patch("/api/addresses/{id}/default", id))
                .andExpect(status().isNotFound());
    }
}
