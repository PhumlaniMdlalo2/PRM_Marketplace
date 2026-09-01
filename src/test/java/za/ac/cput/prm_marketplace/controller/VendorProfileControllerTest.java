package za.ac.cput.prm_marketplace.controller;

import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.service.IVendorProfileService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VendorProfileController.class)
class VendorProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IVendorProfileService vendorProfileService;

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
    void createReturnsCreatedWhenServiceSucceeds() throws Exception {
        VendorProfile profile = buildProfile(null);
        VendorProfile saved = buildProfile(UUID.randomUUID());
        when(vendorProfileService.create(any(VendorProfile.class))).thenReturn(saved);

        mockMvc.perform(post("/vendor-profiles")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businessName").value("Acme Repairs"));
    }

    @Test
    void createReturnsBadRequestWhenServiceRejects() throws Exception {
        VendorProfile profile = buildProfile(null);
        when(vendorProfileService.create(any(VendorProfile.class))).thenReturn(null);

        mockMvc.perform(post("/vendor-profiles")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readReturnsProfileWhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(vendorProfileService.read(id)).thenReturn(buildProfile(id));

        mockMvc.perform(get("/vendor-profiles/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Acme Repairs"));
    }

    @Test
    void readReturnsNotFoundWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(vendorProfileService.read(id)).thenReturn(null);

        mockMvc.perform(get("/vendor-profiles/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturnsOkWhenServiceSucceeds() throws Exception {
        VendorProfile profile = buildProfile(UUID.randomUUID());
        when(vendorProfileService.update(any(VendorProfile.class))).thenReturn(profile);

        mockMvc.perform(put("/vendor-profiles")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsNotFoundWhenServiceRejects() throws Exception {
        VendorProfile profile = buildProfile(UUID.randomUUID());
        when(vendorProfileService.update(any(VendorProfile.class))).thenReturn(null);

        mockMvc.perform(put("/vendor-profiles")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturnsNoContentWhenDeleted() throws Exception {
        UUID id = UUID.randomUUID();
        when(vendorProfileService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/vendor-profiles/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturnsNotFoundWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(vendorProfileService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/vendor-profiles/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllReturnsListOfProfiles() throws Exception {
        when(vendorProfileService.getAll()).thenReturn(List.of(buildProfile(UUID.randomUUID())));

        mockMvc.perform(get("/vendor-profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].businessName").value("Acme Repairs"));
    }

    @Test
    void findByUserIdReturnsProfileWhenFound() throws Exception {
        UUID userId = UUID.randomUUID();
        VendorProfile profile = buildProfile(UUID.randomUUID());
        when(vendorProfileService.findByUserId(userId)).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/vendor-profiles/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Acme Repairs"));
    }

    @Test
    void findByUserIdReturnsNotFoundWhenMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        when(vendorProfileService.findByUserId(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/vendor-profiles/user/{userId}", userId))
                .andExpect(status().isNotFound());
    }
}