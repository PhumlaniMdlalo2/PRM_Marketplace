package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.security.UserPrincipal;
import za.ac.cput.prm_marketplace.service.IUserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real filter chain so each request carries a real principal — that is the only way to
 * prove the acting account comes from the token rather than from the request.
 *
 * <p>The tests below the {@code me} ones exist to pin down the shape of the old surface. Two of
 * them write an account by posting the entity itself, which is what allowed any authenticated
 * caller to rewrite any other account; asserting those routes answer 405 keeps them from coming
 * back.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUserService userService;

    private UUID callerId;

    /** A second account, used to prove one caller cannot reach the other's. */
    private UUID intruderId;

    private Role callerRole = Role.STUDENT;

    @BeforeEach
    void setUp() {
        callerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
    }

    private RequestPostProcessor as(UUID userId, Role role) {
        UserPrincipal principal =
                new UserPrincipal(userId, userId + "@example.com", "hash", role, true);
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    }

    private RequestPostProcessor asCaller() {
        return as(callerId, callerRole);
    }

    private void actAs(UUID userId, Role role) {
        this.callerId = userId;
        this.callerRole = role;
    }

    private User buildUser(UUID id, Role role) {
        return new User.Builder()
                .setId(id)
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hash")
                .setRole(role)
                .setPhone("0710000000")
                .build();
    }

    // ---- the caller's own account ----------------------------------------------------------------

    @Test
    @DisplayName("GET /api/users/me reads the token's account, not one named in the request")
    void meReturnsTheCallersOwnAccount() throws Exception {
        when(userService.read(callerId)).thenReturn(buildUser(callerId, Role.STUDENT));

        mockMvc.perform(get("/api/users/me").with(asCaller()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"));

        verify(userService).read(callerId);
    }

    @Test
    @DisplayName("GET /api/users/me is 404 when the account behind the token no longer exists")
    void meReturnsNotFoundWhenAccountIsGone() throws Exception {
        when(userService.read(callerId)).thenReturn(null);

        mockMvc.perform(get("/api/users/me").with(asCaller()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/users/me never returns the password hash")
    void meOmitsThePasswordHash() throws Exception {
        when(userService.read(callerId)).thenReturn(buildUser(callerId, Role.STUDENT));

        mockMvc.perform(get("/api/users/me").with(asCaller()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("PUT /api/users/me applies only the three editable fields")
    void updateMePassesOnlyEditableFields() throws Exception {
        User saved = new User.Builder()
                .setId(callerId).setName("Jane Roe").setEmail("jane@example.com")
                .setPasswordHash("hash").setRole(Role.STUDENT)
                .setPhone("0722222222").setAvatarUrl("https://cdn.example.com/a.png")
                .build();
        when(userService.updateProfile(any(), any(), any(), any())).thenReturn(saved);

        mockMvc.perform(put("/api/users/me").with(asCaller())
                        .contentType("application/json")
                        .content("""
                                {"name":"Jane Roe","phone":"0722222222",
                                 "avatarUrl":"https://cdn.example.com/a.png"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Roe"));

        verify(userService).updateProfile(
                eq(callerId), eq("Jane Roe"), eq("0722222222"), eq("https://cdn.example.com/a.png"));
    }

    @Test
    @DisplayName("PUT /api/users/me ignores a role or password smuggled into the body")
    void updateMeCannotEscalateThroughTheBody() throws Exception {
        // The stored account stays a student, and the response must not claim otherwise.
        when(userService.updateProfile(any(), any(), any(), any()))
                .thenReturn(buildUser(callerId, Role.STUDENT));

        mockMvc.perform(put("/api/users/me").with(asCaller())
                        .contentType("application/json")
                        .content("""
                                {"id":"%s","name":"Jane Doe","role":"FACULTY",
                                 "password":"hunter2","email":"attacker@evil.example",
                                 "verified":true}""".formatted(intruderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.verified").value(false));

        // The escalation never reaches the service: only the allowlisted values are passed, and the
        // id it updates is the caller's own rather than the one in the body.
        verify(userService).updateProfile(eq(callerId), eq("Jane Doe"), eq(null), eq(null));
        verify(userService, never()).updateProfile(eq(intruderId), any(), any(), any());
    }

    @Test
    @DisplayName("PUT /api/users/me is 404 when the account behind the token no longer exists")
    void updateMeReturnsNotFoundWhenAccountIsGone() throws Exception {
        when(userService.updateProfile(any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(put("/api/users/me").with(asCaller())
                        .contentType("application/json")
                        .content("""
                                {"name":"Jane Doe"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/users/me rejects a blank name")
    void updateMeRejectsBlankName() throws Exception {
        mockMvc.perform(put("/api/users/me").with(asCaller())
                        .contentType("application/json")
                        .content("""
                                {"name":"   "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());

        verify(userService, never()).updateProfile(any(), any(), any(), any());
    }

    @Test
    @DisplayName("PUT /api/users/me rejects an avatar URL that is not http(s)")
    void updateMeRejectsNonHttpAvatarUrl() throws Exception {
        mockMvc.perform(put("/api/users/me").with(asCaller())
                        .contentType("application/json")
                        .content("""
                                {"name":"Jane Doe","avatarUrl":"javascript:alert(1)"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.avatarUrl").exists());

        verify(userService, never()).updateProfile(any(), any(), any(), any());
    }

    // ---- the retired write endpoints ---------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/users no longer exists: an account cannot be written by posting the entity")
    void legacyEntityUpdateIsGone() throws Exception {
        mockMvc.perform(put("/api/users").with(asCaller())
                        .contentType("application/json")
                        .content("""
                                {"id":"%s","role":"FACULTY"}""".formatted(intruderId)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("POST /api/users no longer exists: accounts are created by the register flow")
    void legacyEntityCreateIsGone() throws Exception {
        mockMvc.perform(post("/api/users").with(asCaller())
                        .contentType("application/json")
                        .content("""
                                {"email":"attacker@evil.example","role":"FACULTY",
                                 "passwordHash":"x"}"""))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---- directory reads --------------------------------------------------------------------------

    @Test
    @DisplayName("a single account stays readable so listings can show a seller's name")
    void readReturnsUserWhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.read(id)).thenReturn(buildUser(id, Role.VENDOR));

        mockMvc.perform(get("/api/users/{id}", id).with(asCaller()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("a missing account is 404")
    void readReturnsNotFoundWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.read(id)).thenReturn(null);

        mockMvc.perform(get("/api/users/{id}", id).with(asCaller()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("listing every account is faculty-only")
    void getAllIsFacultyOnly() throws Exception {
        when(userService.getAll()).thenReturn(List.of(buildUser(callerId, Role.FACULTY)));

        mockMvc.perform(get("/api/users").with(asCaller()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users").with(as(callerId, Role.FACULTY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("jane@example.com"));
    }

    @Test
    @DisplayName("lookup by email is faculty-only, so it cannot be used to test for membership")
    void findByEmailIsFacultyOnly() throws Exception {
        when(userService.findByEmail("jane@example.com"))
                .thenReturn(Optional.of(buildUser(callerId, Role.STUDENT)));

        mockMvc.perform(get("/api/users/email/{email}", "jane@example.com").with(asCaller()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/email/{email}", "jane@example.com")
                        .with(as(callerId, Role.FACULTY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    @DisplayName("a missing email is 404 for faculty")
    void findByEmailReturnsNotFoundWhenMissing() throws Exception {
        when(userService.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/email/{email}", "missing@example.com")
                        .with(as(callerId, Role.FACULTY)))
                .andExpect(status().isNotFound());
    }

    // ---- deletion ---------------------------------------------------------------------------------

    @Test
    @DisplayName("a user can close their own account")
    void deleteOwnAccountSucceeds() throws Exception {
        when(userService.delete(callerId)).thenReturn(true);

        mockMvc.perform(delete("/api/users/{id}", callerId).with(asCaller()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("a user cannot delete someone else's account")
    void deleteAnotherAccountIsForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", intruderId).with(asCaller()))
                .andExpect(status().isForbidden());

        verify(userService, never()).delete(intruderId);
    }

    @Test
    @DisplayName("faculty can delete an account")
    void facultyCanDeleteAnyAccount() throws Exception {
        when(userService.delete(intruderId)).thenReturn(true);

        mockMvc.perform(delete("/api/users/{id}", intruderId).with(as(callerId, Role.FACULTY)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("deleting a missing account is 404")
    void deleteReturnsNotFoundWhenMissing() throws Exception {
        when(userService.delete(callerId)).thenReturn(false);

        mockMvc.perform(delete("/api/users/{id}", callerId).with(asCaller()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("acting as the intruder still cannot reach the original caller")
    void switchingCallerDoesNotWidenAccess() throws Exception {
        // Captured locally, because actAs reassigns the field the path is built from.
        UUID victimId = callerId;
        when(userService.delete(victimId)).thenReturn(true);

        mockMvc.perform(delete("/api/users/{id}", victimId).with(asCaller()))
                .andExpect(status().isNoContent());

        actAs(intruderId, Role.STUDENT);
        mockMvc.perform(delete("/api/users/{id}", victimId).with(asCaller()))
                .andExpect(status().isForbidden());
    }
}