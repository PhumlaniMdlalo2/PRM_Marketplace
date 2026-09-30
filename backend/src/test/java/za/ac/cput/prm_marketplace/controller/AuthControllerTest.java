package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.dto.AuthResponse;
import za.ac.cput.prm_marketplace.dto.UserResponse;
import za.ac.cput.prm_marketplace.exception.BadRequestException;
import za.ac.cput.prm_marketplace.exception.ConflictException;
import za.ac.cput.prm_marketplace.exception.UnauthorizedException;
import za.ac.cput.prm_marketplace.service.IAuthService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAuthService authService;

    private UserResponse buildUser(boolean verified) {
        return new UserResponse(UUID.randomUUID(), "Jane Doe", "jane@example.com", Role.STUDENT,
                null, null, verified, null);
    }

    private AuthResponse buildAuthResponse() {
        return new AuthResponse("jwt-token", "Bearer", 3600L, buildUser(true));
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
    @DisplayName("changePassword: returns 200 and delegates")
    void changePassword_returnsOk() throws Exception {
        doNothing().when(authService).changePassword(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/auth/change-password")
                        .param("email", "jane@example.com")
                        .param("currentPassword", "old")
                        .param("newPassword", "brandNew123"))
                .andExpect(status().isOk());

        verify(authService).changePassword("jane@example.com", "old", "brandNew123");
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