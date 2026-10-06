package za.ac.cput.prm_marketplace.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import za.ac.cput.prm_marketplace.security.CustomUserDetailsService;
import za.ac.cput.prm_marketplace.security.JwtAuthenticationFilter;
import za.ac.cput.prm_marketplace.security.JwtService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** The value that used to be committed to version control. It must never sign a real token. */
    private static final String FORBIDDEN_PLACEHOLDER_SECRET =
            "change-this-development-signing-secret-value-please";

    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
                                                           CustomUserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    /**
     * Fails fast when the signing key is missing, too short for HS256 or later, or still set to
     * the placeholder that was previously committed. Without this the application would happily
     * sign tokens with a key anyone can read from the repository.
     */
    @Bean
    public JwtSecret jwtSecretValidator(@Value("${app.jwt.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret is not set. Provide a secret of at least " + MIN_SECRET_BYTES
                            + " bytes via the JWT_SECRET environment variable.");
        }
        if (FORBIDDEN_PLACEHOLDER_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "app.jwt.secret is still the development placeholder that was committed to "
                            + "version control. Set JWT_SECRET to a private random value.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes.");
        }
        return new JwtSecret(secret);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // "My own" routes are named before the public catalogue wildcards below,
                        // because Spring Security takes the first matcher that fits and
                        // "/api/products/**" would otherwise swallow these. They used to be covered
                        // by that wildcard and so did not need a token from the filter chain: they
                        // only answered 401 because the controller happened to ask CurrentCaller who
                        // was calling. A new method on either controller that forgot to would have
                        // been an unauthenticated read of caller-scoped data with nothing to catch it.
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/mine",
                                "/api/vendor-profiles/me")
                        .authenticated()
                        // Only the endpoints that must work before a caller has a token. Note that
                        // change-password is deliberately absent: it requires an authenticated caller.
                        // refresh belongs here for a reason that is easy to get wrong: the access
                        // token it renews is the very thing that just expired, so requiring one to
                        // ask for another would lock the caller out of renewing.
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/verify",
                                "/api/auth/resend-code",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                // The health probes. They report up or down and nothing else, so they
                                // need no token: an orchestrator asking "is this up?" should not have
                                // to hold a credential to be told no. The sub-paths are the liveness
                                // and readiness groups, which is where a probe should actually point.
                                "/actuator/health",
                                "/actuator/health/**",
                                "/error")
                        .permitAll()
                        // Browsing the catalogue and the seller directory is public; only writes
                        // on these paths need a token. "/bulletin-posts/**" here was stale: the
                        // controller is mounted at "/api/bulletin-posts", so this rule never matched
                        // anything and left public board reads behind a login.
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/**",
                                "/api/product-images/**",
                                "/api/comments/**",
                                "/api/bulletin-posts/**",
                                "/api/reviews/**",
                                "/api/vendor-profiles/**",
                                // Stored photographs. A listing's picture is meant to be seen by
                                // anyone who can see the listing, so reading needs no token - but
                                // this matcher admits GET only, and POST /api/uploads/images does
                                // not match it, so putting a file up still needs to be signed in.
                                "/api/uploads/images/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"Unauthorized\","
                                            + "\"message\":\"Authentication is required\"}");
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":403,\"error\":\"Forbidden\","
                                            + "\"message\":\"You do not have access to this resource\"}");
                        }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        // No default here on purpose. application.properties already supplies one, so the property is
        // always present; repeating it as an @Value fallback meant the two could drift apart, and they
        // had: the fallback named only port 5173 while the properties file also allowed 3000. Anything
        // genuinely missing now fails here rather than silently allowing one origin and not the other.
        if (allowedOrigins == null || allowedOrigins.isEmpty()
                || allowedOrigins.stream().anyMatch(String::isBlank)) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins is empty. Set CORS_ALLOWED_ORIGINS to the origins that may "
                            + "call this API, comma separated. An empty list is refused rather than "
                            + "accepted because the symptom otherwise only appears once the frontend "
                            + "starts making requests and every one of them is rejected.");
        }
        if (allowedOrigins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins must list explicit origins. Credentials are allowed, so a "
                            + "wildcard would let any site issue authenticated requests.");
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Carries the validated signing secret. Its only job is to prove at startup that the key is
     * usable; {@link JwtService} reads the property itself.
     */
    public record JwtSecret(String value) {
    }
}