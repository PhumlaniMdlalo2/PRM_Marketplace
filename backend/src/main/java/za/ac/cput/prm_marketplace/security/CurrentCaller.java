package za.ac.cput.prm_marketplace.security;

import org.springframework.security.core.Authentication;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.exception.UnauthorizedException;

import java.util.UUID;

/**
 * Turns the authenticated {@link Authentication} into the account it stands for.
 *
 * <p>Controllers take an {@code Authentication} rather than reaching for an id in the request.
 * Spring resolves that parameter early in the argument-resolver chain, before the catch-all
 * model-attribute binder, so it cannot be confused with a request body. That matters: with
 * {@code @AuthenticationPrincipal} on this framework version the parameter fell through to the
 * binder, which tried to bind request parameters onto the principal object and rejected every
 * request with a 400.
 *
 * <p>Every owner-scoped query in the application resolves the caller through here, so no endpoint
 * has to trust an id that arrived in a path, a query string or a body.
 */
public final class CurrentCaller {

    private CurrentCaller() {
    }

    /**
     * @throws UnauthorizedException when there is no authenticated user, which the exception
     *         handler turns into a 401 rather than a 500
     */
    public static UserPrincipal principal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal user) {
            return user;
        }
        throw new UnauthorizedException("Authentication is required");
    }

    /** The authenticated user's id, to scope a query to rows they own. */
    public static UUID id(Authentication authentication) {
        return principal(authentication).getId();
    }

    /** The authenticated user's role, for the checks that are not a simple ownership test. */
    public static Role role(Authentication authentication) {
        return principal(authentication).getRole();
    }

    public static boolean is(Authentication authentication, UUID userId) {
        return authentication != null
                && authentication.getPrincipal() instanceof UserPrincipal user
                && user.getId() != null
                && user.getId().equals(userId);
    }
}