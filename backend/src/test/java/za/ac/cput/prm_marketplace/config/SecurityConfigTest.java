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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    @DisplayName("a caller's own listings require authentication, not just a controller check")
    void myListings_requireAuthentication() throws Exception {
        // "/api/products/**" is a public GET matcher, so this route sat inside it and never asked the
        // filter chain for a token. It answered 401 anyway because the controller asked CurrentCaller
        // who was calling, which means the only thing standing between this route and an anonymous
        // read of one seller's stock was every future method remembering to make that call.
        mockMvc.perform(get("/api/products/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a caller's own seller profile requires authentication")
    void myVendorProfile_requiresAuthentication() throws Exception {
        // Same shape of problem as above, on the other prefix that "/**" covers.
        mockMvc.perform(get("/api/vendor-profiles/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the seller directory stays readable without a token")
    void publicVendorReads_stayPublic() throws Exception {
        // Narrowing "me" must not have narrowed the directory it is served from.
        mockMvc.perform(get("/api/vendor-profiles"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("the liveness probe answers without a token and reports nothing else")
    void liveness_isPublicAndSaysNothingMoreThanUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                // A probe that leaked the database URL, the build version or the bean list would be
                // handing a map of the deployment to anyone who asked.
                .andExpect(content().string("{\"status\":\"UP\"}"));
    }

    @Test
    @DisplayName("an unreachable mail server shows up in readiness instead of failing liveness")
    void readiness_reportsUnreachableMail() throws Exception {
        // Nothing is listening on the test mail port, and until health existed that was invisible:
        // sends were logged and swallowed, the request still returned success, and the only way to
        // claim a bootstrapped faculty account was an email that never arrived.
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable());

        // Liveness must stay up regardless. An orchestrator probing this path should not restart a
        // working process because a mail server is down; restarting would not fix mail.
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("no other actuator endpoint is exposed")
    void otherActuatorEndpoints_areNotExposed() throws Exception {
        // exposure.include is health alone, so these must not exist even to an authenticated caller.
        // /actuator/env in particular hands over every configuration value, and springdoc already
        // publishes the API surface.
        User user = buildUser(UUID.randomUUID());
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        mockMvc.perform(get("/actuator/env").header("Authorization", tokenFor(user)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/beans").header("Authorization", tokenFor(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("an anonymous caller cannot probe the unexposed actuator surface")
    void otherActuatorEndpoints_areNotReachableAnonymously() throws Exception {
        // Security runs before routing, so an endpoint that is not exposed still answers 401 rather
        // than 404 to a caller with no token. Either way nothing is served.
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CORS preflight from the second dev port is accepted")
    void corsPreflight_secondDevPort_isAccepted() throws Exception {
        // Port 3000 is in the default list. It used to be missing from a second copy of that list held
        // in an @Value fallback, so which of the two governed depended on whether the properties file
        // was on the classpath. Asserting both ports pins the default rather than restating it.
        mockMvc.perform(options("/api/users/me")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
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