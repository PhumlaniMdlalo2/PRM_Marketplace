package za.ac.cput.prm_marketplace.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.UserRepository;
import za.ac.cput.prm_marketplace.security.JwtService;
import za.ac.cput.prm_marketplace.service.IAuthService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real filter chain, unlike the {@code @WebMvcTest} slices which run with
 * filters disabled. Verifies which routes are public and that the JWT filter populates
 * the security context.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private IAuthService authService;

    private String tokenFor(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private User buildUser(UUID id) {
        return new User.Builder()
                .setId(id)
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setRole(za.ac.cput.prm_marketplace.domain.Role.STUDENT)
                .setVerified(true)
                .build();
    }

    @Test
    @DisplayName("protected routes reject anonymous callers with 401")
    void protectedRoutes_rejectAnonymous() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("protected write routes reject anonymous callers with 401")
    void protectedWriteRoutes_rejectAnonymous() throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("auth endpoints are public so registration and login can bootstrap a session")
    void authEndpoints_arePublic() throws Exception {
        when(authService.register(any())).thenReturn(new za.ac.cput.prm_marketplace.dto.AuthResponse(
                "jwt-token", "Bearer", 3600L,
                new za.ac.cput.prm_marketplace.dto.UserResponse(
                        UUID.randomUUID(), "Jane", "jane@example.com",
                        za.ac.cput.prm_marketplace.domain.Role.STUDENT, null, null, false, null)));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"name":"Jane","email":"jane@example.com","password":"password123"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("public catalogue reads are permitted without a token")
    void publicCatalogueReads_arePermitted() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("catalogue search is public so browsing works without a session")
    void catalogueSearch_isPublic() throws Exception {
        mockMvc.perform(get("/api/products/search").param("keyword", "textbook"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("review reads are public so ratings render without a session")
    void reviewReads_arePublic() throws Exception {
        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("review writes still require authentication")
    void reviewWrites_requireAuthentication() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("order item reads require authentication")
    void orderItemReads_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/order-items"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a valid bearer token grants access to protected routes")
    void validToken_grantsAccess() throws Exception {
        User user = buildUser(UUID.randomUUID());
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/me").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .header("Authorization", tokenFor(user)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a malformed bearer token is rejected rather than authenticated")
    void malformedToken_isRejected() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a token signed with a foreign secret is rejected")
    void foreignSignedToken_isRejected() throws Exception {
        String foreign = "Bearer " + new JwtService("a-completely-different-signing-secret-value-x", 3_600_000L)
                .generateToken(buildUser(UUID.randomUUID()));

        mockMvc.perform(get("/api/users/me").header("Authorization", foreign))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CORS preflight for an allowed origin is accepted")
    void corsPreflight_allowedOrigin_isAccepted() throws Exception {
        mockMvc.perform(options("/api/users/me")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("CORS preflight for an unknown origin is rejected")
    void corsPreflight_unknownOrigin_isRejected() throws Exception {
        mockMvc.perform(options("/api/users/me")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}