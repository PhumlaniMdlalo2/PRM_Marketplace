package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.dto.AuthResponse;
import za.ac.cput.prm_marketplace.dto.UserResponse;
import za.ac.cput.prm_marketplace.exception.BadRequestException;
import za.ac.cput.prm_marketplace.exception.ConflictException;
import za.ac.cput.prm_marketplace.exception.UnauthorizedException;
import za.ac.cput.prm_marketplace.security.UserPrincipal;
import za.ac.cput.prm_marketplace.service.IAuthService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A full context, with the filter chain running, unlike the web slices used elsewhere.
 *
 * <p>Most of this controller is public, which is why it used to run with filters disabled. But
 * {@code /change-password} is not public, and its whole point is that the account comes from the
 * token rather than a request parameter. With no filter chain there is no token, so the test could
 * not tell the two designs apart — it would have passed just as happily against the old,
 * email-from-the-query-string version.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAuthService authService;

    private UserResponse buildUser(boolean verified) {
        return new UserResponse(UUID.randomUUID(), "Jane Doe", "jane@example.com", Role.STUDENT, null,
                null, null, verified, null);
    }

    private AuthResponse buildAuthResponse() {
        return new AuthResponse("jwt-token", "refresh-token", "Bearer", 3600L, buildUser(true));
    }

    @Test
    @DisplayName("register: returns 201 with the token and user")
    void register_returnsCreated() throws Exception {
        when(authService.register(any())).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Jane Doe","email":"jane@example.com","password":"password123"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("jane@example.com"));
    }

    @Test
    @DisplayName("register: returns 400 when required fields are missing")
    void register_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email"}"""))
                .andExpect(status().isBadRequest());

        verify(authService, org.mockito.Mockito.never()).register(any());
    }

    @Test
    @DisplayName("register: maps a duplicate-email conflict to 409")
    void register_duplicateEmail_returnsConflict() throws Exception {
        when(authService.register(any())).thenThrow(new ConflictException("An account already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Jane","email":"jane@example.com","password":"password123"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account already exists"));
    }

    @Test
    @DisplayName("login: returns 200 with a token")
    void login_returnsOk() throws Exception {
        when(authService.login(any())).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","password":"password123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    @DisplayName("login: maps bad credentials to 401")
    void login_badCredentials_returnsUnauthorized() throws Exception {
        when(authService.login(any())).thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","password":"wrong"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refresh: returns 200 with a new token and refresh token")
    void refresh_returnsOk() throws Exception {
        when(authService.refresh("refresh-token")).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"refresh-token"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("refresh: a missing token is a bad request rather than a call with a null")
    void refresh_missingToken_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(authService, never()).refresh(any());
    }

    @Test
    @DisplayName("refresh: a token the server will not exchange is 401, so the client signs in again")
    void refresh_expiredSession_returnsUnauthorized() throws Exception {
        when(authService.refresh("spent-token"))
                .thenThrow(new UnauthorizedException("Session expired. Please sign in again"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"spent-token"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Session expired. Please sign in again"));
    }

    @Test
    @DisplayName("verify: returns 200 with the verified user")
    void verify_returnsOk() throws Exception {
        when(authService.verifyCode(anyString(), anyString())).thenReturn(buildUser(true));

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","code":"123456"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    @DisplayName("verify: maps an invalid code to 400")
    void verify_invalidCode_returnsBadRequest() throws Exception {
        when(authService.verifyCode(anyString(), anyString())).thenThrow(new BadRequestException("Verification code is invalid"));

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","code":"000000"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("resendCode: returns 200 and delegates")
    void resendCode_returnsOk() throws Exception {
        doNothing().when(authService).resendCode(anyString());

        mockMvc.perform(post("/api/auth/resend-code").param("email", "jane@example.com"))
                .andExpect(status().isOk());

        verify(authService).resendCode("jane@example.com");
    }

    @Test
    @DisplayName("forgotPassword: always returns 200 to avoid leaking which emails exist")
    void forgotPassword_returnsOk() throws Exception {
        doNothing().when(authService).forgotPassword(anyString());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com"}"""))
                .andExpect(status().isOk());

        verify(authService).forgotPassword("jane@example.com");
    }

    @Test
    @DisplayName("resetPassword: returns 200 and delegates")
    void resetPassword_returnsOk() throws Exception {
        doNothing().when(authService).resetPassword(any());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"token-123","newPassword":"newPassword123"}"""))
                .andExpect(status().isOk());

        verify(authService).resetPassword(any());
    }

    @Test
    @DisplayName("resetPassword: maps an invalid token to 400")
    void resetPassword_invalidToken_returnsBadRequest() throws Exception {
        doThrow(new BadRequestException("Reset token is invalid")).when(authService).resetPassword(any());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"nope","newPassword":"newPassword123"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("changePassword: identifies the account from the token, not a request parameter")
    void changePassword_returnsOk() throws Exception {
        UUID callerId = UUID.randomUUID();
        doNothing().when(authService).changePassword(any(), anyString(), anyString());

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"old","newPassword":"brandNew123"}""")
                        .with(as(callerId)))
                .andExpect(status().isOk());

        // The account came off the token. An email field would have let any authenticated
        // caller point this at a different account.
        verify(authService).changePassword(callerId, "old", "brandNew123");
    }

    @Test
    @DisplayName("changePassword: an email in the body is ignored, not honoured")
    void changePassword_ignoresSuppliedEmail() throws Exception {
        UUID callerId = UUID.randomUUID();
        UUID victimId = UUID.randomUUID();
        doNothing().when(authService).changePassword(any(), anyString(), anyString());

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"victim@example.com","currentPassword":"old",\
                                "newPassword":"brandNew123"}""")
                        .with(as(callerId)))
                .andExpect(status().isOk());

        verify(authService).changePassword(callerId, "old", "brandNew123");
        verify(authService, never()).changePassword(eq(victimId), anyString(), anyString());
    }

    @Test
    @DisplayName("changePassword: the new password travels in the body, never in the URL")
    void changePassword_keepsPasswordOutOfTheRequestLine() throws Exception {
        UUID callerId = UUID.randomUUID();
        doNothing().when(authService).changePassword(any(), anyString(), anyString());

        mockMvc.perform(post("/api/auth/change-password")
                        .param("currentPassword", "old")
                        .param("newPassword", "brandNew123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"old","newPassword":"brandNew123"}""")
                        .with(as(callerId)))
                .andExpect(status().isOk());

        // The body alone is enough to satisfy the route. If either password were still being read
        // from a parameter, this would fail with 400 instead: the params are ignored, and nothing in
        // the URL is left for them to be read from.
    }

    @Test
    @DisplayName("changePassword: a new password below the minimum length is rejected")
    void changePassword_rejectsShortNewPassword() throws Exception {
        UUID callerId = UUID.randomUUID();

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"old","newPassword":"short"}""")
                        .with(as(callerId)))
                .andExpect(status().isBadRequest());

        // Rejected before the service is reached, so a caller cannot set a password that registration
        // would have refused.
        verify(authService, never()).changePassword(any(), anyString(), anyString());
    }

    /** Authenticates the request as the given account. */
    private RequestPostProcessor as(UUID userId) {
        UserPrincipal principal = new UserPrincipal(
                userId, userId + "@example.com", "hash", Role.STUDENT, true);
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    }

    @Test
    @DisplayName("responses never include the password hash")
    void responses_neverExposePasswordHash() throws Exception {
        when(authService.register(any())).thenReturn(buildAuthResponse());

        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Jane Doe","email":"jane@example.com","password":"password123"}"""))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("passwordHash")
                .doesNotContain("password123");
    }

    @Test
    @DisplayName("unhandled failures map to 500 rather than leaking internals")
    void unhandledFailure_returnsInternalServerError() throws Exception {
        when(authService.login(any())).thenThrow(new IllegalStateException("database exploded"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","password":"password123"}"""))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}