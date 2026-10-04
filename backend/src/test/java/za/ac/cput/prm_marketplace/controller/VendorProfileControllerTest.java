package za.ac.cput.prm_marketplace.controller;

import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.service.IVendorProfileService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.as;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.asStudent;

/**
 * The seller directory stays public, but the owner of a profile is now taken from the token and the
 * profile id comes from the path. The regression tests here cover what a client can no longer do:
 * mark itself verified, edit another seller's business details, or look up the business behind an
 * arbitrary user id.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VendorProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IVendorProfileService vendorProfileService;

    private UUID profileId;
    private UUID sellerId;
    private UUID intruderId;
    private User seller;

    @BeforeEach
    void setUp() {
        profileId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        seller = new User.Builder()
                .setId(sellerId)
                .setName("Vendor Owner")
                .setEmail("owner@example.com")
                .setPasswordHash("hash")
                .setRole(Role.VENDOR)
                .build();
    }

    private VendorProfile buildProfile(UUID id) {
        return new VendorProfile.Builder()
                .setId(id)
                .setUser(seller)
                .setBusinessName("Acme Repairs")
                .setRegistrationNo("REG-1")
                .build();
    }

    // create

    @Test
    @DisplayName("a seller can open a profile and the service is given their id and role")
    void create_returnsCreated() throws Exception {
        when(vendorProfileService.create(any(VendorProfile.class), eq(sellerId), eq(Role.VENDOR)))
                .thenReturn(buildProfile(profileId));

        mockMvc.perform(post("/api/vendor-profiles")
                        .with(as(sellerId, Role.VENDOR))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(buildProfile(null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businessName").value("Acme Repairs"));
    }

    @Test
    @DisplayName("a self-declared verified flag never reaches the service")
    void create_verifiedInBodyIsStripped() throws Exception {
        when(vendorProfileService.create(any(VendorProfile.class), eq(sellerId), eq(Role.VENDOR)))
                .thenReturn(buildProfile(profileId));

        String hostile = """
                {
                  "businessName": "Acme Repairs",
                  "verified": true,
                  "ratingAvg": 5.00,
                  "createdAt": "2000-01-01T00:00:00",
                  "user": {"id": "%s", "email": "victim@example.com", "passwordHash": "leaked"}
                }
                """.formatted(intruderId);

        mockMvc.perform(post("/api/vendor-profiles")
                        .with(as(sellerId, Role.VENDOR))
                        .contentType("application/json")
                        .content(hostile))
                .andExpect(status().isCreated());

        org.mockito.ArgumentCaptor<VendorProfile> captor =
                org.mockito.ArgumentCaptor.forClass(VendorProfile.class);
        verify(vendorProfileService).create(captor.capture(), eq(sellerId), eq(Role.VENDOR));

        assertFalse(captor.getValue().isVerified(), "a seller cannot verify itself");
        assertNull(captor.getValue().getRatingAvg(), "rating is derived, not accepted");
        assertNull(captor.getValue().getCreatedAt(), "creation time is server-owned");
        assertNull(captor.getValue().getUser(), "the owner comes from the token, not the body");
    }

    @Test
    @DisplayName("a non-vendor is refused")
    void create_nonVendor_returnsBadRequest() throws Exception {
        when(vendorProfileService.create(any(VendorProfile.class), eq(intruderId), eq(Role.STUDENT)))
                .thenReturn(null);

        mockMvc.perform(post("/api/vendor-profiles")
                        .with(asStudent(intruderId))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(buildProfile(null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a second profile for the same account is refused")
    void create_duplicate_returnsBadRequest() throws Exception {
        when(vendorProfileService.create(any(VendorProfile.class), eq(sellerId), eq(Role.VENDOR)))
                .thenReturn(null);

        mockMvc.perform(post("/api/vendor-profiles")
                        .with(as(sellerId, Role.VENDOR))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(buildProfile(null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("an anonymous caller cannot open a profile")
    void create_rejectsAnonymous() throws Exception {
        mockMvc.perform(post("/api/vendor-profiles")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(buildProfile(null))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(vendorProfileService);
    }

    // read

    @Test
    @DisplayName("a profile is readable without logging in, because the directory is public")
    void read_returnsProfileAnonymously() throws Exception {
        when(vendorProfileService.read(profileId)).thenReturn(buildProfile(profileId));

        mockMvc.perform(get("/api/vendor-profiles/{id}", profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Acme Repairs"));
    }

    @Test
    @DisplayName("a profile response does not expose the owner's account")
    void read_doesNotLeakTheOwnerAccount() throws Exception {
        when(vendorProfileService.read(profileId)).thenReturn(buildProfile(profileId));

        String body = mockMvc.perform(get("/api/vendor-profiles/{id}", profileId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("passwordHash")
                .doesNotContain("owner@example.com");
    }

    @Test
    void read_returnsNotFoundWhenMissing() throws Exception {
        when(vendorProfileService.read(profileId)).thenReturn(null);

        mockMvc.perform(get("/api/vendor-profiles/{id}", profileId))
                .andExpect(status().isNotFound());
    }

    // update

    @Test
    @DisplayName("a seller can edit their own profile")
    void update_ownProfile_returnsOk() throws Exception {
        when(vendorProfileService.update(eq(profileId), any(VendorProfile.class), eq(sellerId)))
                .thenReturn(buildProfile(profileId));

        mockMvc.perform(put("/api/vendor-profiles/{id}", profileId)
                        .with(as(sellerId, Role.VENDOR))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(buildProfile(profileId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Acme Repairs"));
    }

    @Test
    @DisplayName("editing another seller's profile reads as not found")
    void update_somebodyElsesProfile_returnsNotFound() throws Exception {
        when(vendorProfileService.update(eq(profileId), any(VendorProfile.class), eq(intruderId)))
                .thenReturn(null);

        mockMvc.perform(put("/api/vendor-profiles/{id}", profileId)
                        .with(as(intruderId, Role.VENDOR))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(buildProfile(profileId))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a verified value in the body cannot survive deserialisation")
    void update_verifiedInBodyIsStripped() throws Exception {
        when(vendorProfileService.update(eq(profileId), any(VendorProfile.class), eq(sellerId)))
                .thenReturn(buildProfile(profileId));

        String hostile = """
                {"businessName": "Acme Repairs", "verified": true, "ratingAvg": 5.00}
                """;

        mockMvc.perform(put("/api/vendor-profiles/{id}", profileId)
                        .with(as(sellerId, Role.VENDOR))
                        .contentType("application/json")
                        .content(hostile))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<VendorProfile> captor =
                org.mockito.ArgumentCaptor.forClass(VendorProfile.class);
        verify(vendorProfileService).update(eq(profileId), captor.capture(), eq(sellerId));

        assertFalse(captor.getValue().isVerified());
        assertNull(captor.getValue().getRatingAvg());
    }

    @Test
    @DisplayName("an anonymous caller cannot edit a profile")
    void update_rejectsAnonymous() throws Exception {
        mockMvc.perform(put("/api/vendor-profiles/{id}", profileId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(buildProfile(profileId))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(vendorProfileService);
    }

    // delete

    @Test
    @DisplayName("there is no delete route: a seller with stock cannot have their profile removed")
    void deleteIsNotAvailable() throws Exception {
        mockMvc.perform(delete("/api/vendor-profiles/{id}", profileId)
                        .with(as(sellerId, Role.VENDOR)))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(vendorProfileService);
    }

    // getAll

    @Test
    @DisplayName("the seller directory is readable without logging in")
    void getAll_returnsListAnonymously() throws Exception {
        when(vendorProfileService.getAll()).thenReturn(List.of(buildProfile(profileId)));

        mockMvc.perform(get("/api/vendor-profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].businessName").value("Acme Repairs"));
    }

    // findMine

    @Test
    @DisplayName("a seller can read their own profile")
    void findMine_returnsOwnProfile() throws Exception {
        when(vendorProfileService.findMine(sellerId)).thenReturn(buildProfile(profileId));

        mockMvc.perform(get("/api/vendor-profiles/me").with(as(sellerId, Role.VENDOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Acme Repairs"));
    }

    @Test
    @DisplayName("a seller with no profile gets not found")
    void findMine_missingProfile_returnsNotFound() throws Exception {
        when(vendorProfileService.findMine(sellerId)).thenReturn(null);

        mockMvc.perform(get("/api/vendor-profiles/me").with(as(sellerId, Role.VENDOR)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the caller id is taken from the token, not from a path parameter")
    void findMine_usesTheToken() throws Exception {
        when(vendorProfileService.findMine(sellerId)).thenReturn(buildProfile(profileId));

        mockMvc.perform(get("/api/vendor-profiles/me").with(as(sellerId, Role.VENDOR)))
                .andExpect(status().isOk());

        verify(vendorProfileService).findMine(sellerId);
    }

    @Test
    @DisplayName("the old user-id lookup is gone, so a business cannot be mapped to any account")
    void findByUserIdRouteIsNotAvailable() throws Exception {
        UUID victimId = UUID.randomUUID();

        // 401 rather than 404 on the legacy path is expected and fine: the point is that no
        // handler exists for it, and nothing was revealed to the caller about the victim.
        mockMvc.perform(get("/vendor-profiles/user/{userId}", victimId)
                        .with(as(intruderId, Role.VENDOR)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/vendor-profiles/user/{userId}", victimId)
                        .with(as(intruderId, Role.VENDOR)))
                .andExpect(status().isNotFound());

        verifyNoInteractions(vendorProfileService);
    }

    @Test
    @DisplayName("an anonymous caller cannot read their own profile")
    void findMine_rejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/vendor-profiles/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(vendorProfileService);
    }

    @Test
    @DisplayName("a verification state that is already stored is still readable")
    void read_returnsTheStoredVerificationFlag() throws Exception {
        VendorProfile verifiedProfile = new VendorProfile.Builder()
                .copy(buildProfile(profileId))
                .setVerified(true)
                .setRatingAvg(new BigDecimal("4.50"))
                .build();
        when(vendorProfileService.read(profileId)).thenReturn(verifiedProfile);

        mockMvc.perform(get("/api/vendor-profiles/{id}", profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.ratingAvg").value(4.5));
    }
}