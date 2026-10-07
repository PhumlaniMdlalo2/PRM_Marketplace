package za.ac.cput.prm_marketplace.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.security.UserPrincipal;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Builds authenticated MockMvc requests.
 *
 * <p>These controller tests run the real security filter chain rather than
 * {@code addFilters = false}. A slice with the filters disabled never populates the
 * {@code SecurityContextHolder}, so the controller under test would have no way to learn who the
 * caller is. Running the real chain also proves the endpoints actually reject anonymous traffic.
 */
public final class AuthenticatedRequests {

    private AuthenticatedRequests() {
    }

    public static RequestPostProcessor as(UUID userId, Role role) {
        UserPrincipal principal =
                new UserPrincipal(userId, userId + "@example.com", "hash", role, true);
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    }

    public static RequestPostProcessor asStudent(UUID userId) {
        return as(userId, Role.STUDENT);
    }

    public static RequestPostProcessor asAdmin(UUID userId) {
        return as(userId, Role.ADMIN);
    }
}