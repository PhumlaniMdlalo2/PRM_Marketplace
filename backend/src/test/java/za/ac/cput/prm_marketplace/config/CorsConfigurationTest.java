package za.ac.cput.prm_marketplace.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The CORS allow-list is the one control standing between a page on another site and an
 * authenticated request from this application, because credentials are allowed. That makes its
 * failure modes worth pinning: the list has to be right, and it has to fail loudly when it is not.
 */
class CorsConfigurationTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    private CorsConfiguration configurationFor(List<String> origins) {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource(origins);
        return source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/products"));
    }

    @Test
    @DisplayName("explicit origins are accepted and applied as given")
    void explicitOrigins_areApplied() {
        CorsConfiguration configuration =
                configurationFor(List.of("http://localhost:5173", "https://app.example.ac.za"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .containsExactly("http://localhost:5173", "https://app.example.ac.za");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("a wildcard is refused at startup rather than allowed to work")
    void wildcard_isRefused() {
        // Credentials are allowed here, so "*" would let any site on the internet read authenticated
        // responses. Failing at startup means nobody deploys it by accident and finds out later.
        assertThatThrownBy(() -> securityConfig.corsConfigurationSource(List.of("*")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("explicit origins");
    }

    @Test
    @DisplayName("a wildcard mixed into real origins is refused too")
    void wildcardAmongOrigins_isRefused() {
        // A list like this is the likely typo, and it must not be quietly trimmed down to the two
        // real origins: someone asked for something unsafe and should be told.
        assertThatThrownBy(() -> securityConfig.corsConfigurationSource(
                List.of("https://app.example.ac.za", "https://*.example.ac.za")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("an empty allow-list is refused rather than silently rejecting every request")
    void emptyList_isRefused() {
        // This is the case that used to pass. An empty list built a valid configuration that permitted
        // nothing, so the application started cleanly and every cross-origin call failed only once
        // traffic arrived, with an error that pointed at the frontend rather than at configuration.
        assertThatThrownBy(() -> securityConfig.corsConfigurationSource(List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("a missing allow-list is refused")
    void missingList_isRefused() {
        assertThatThrownBy(() -> securityConfig.corsConfigurationSource(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("a blank entry is refused")
    void blankEntry_isRefused() {
        // "http://a.example,,http://b.example" or a trailing comma both produce this, and neither
        // entry matches any real origin while still looking like a configured allow-list.
        assertThatThrownBy(() -> securityConfig.corsConfigurationSource(
                List.of("http://localhost:5173", "  ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("the property carries no second default hiding in the code")
    void propertyHasNoCodeDefault() throws Exception {
        // The list used to be written twice: once in application.properties and once as an @Value
        // fallback, and the two had already drifted, with the code naming only port 5173. Any fallback
        // here would quietly become the value actually used whenever the properties file is not on
        // the classpath, and the guards above would stop governing anything.
        Method method = SecurityConfig.class.getMethod("corsConfigurationSource", List.class);
        Value value = method.getParameters()[0].getAnnotation(Value.class);

        assertThat(value).isNotNull();
        assertThat(value.value()).isEqualTo("${app.cors.allowed-origins}");
    }
}