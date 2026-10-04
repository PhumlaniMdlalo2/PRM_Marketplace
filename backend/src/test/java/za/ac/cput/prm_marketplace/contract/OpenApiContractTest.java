package za.ac.cput.prm_marketplace.contract;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Freezes the HTTP contract to a committed file and asserts the rules this project depends on.
 *
 * <p>The route list used to live only in the code and in the author's head, which made a route
 * change impossible to review: nothing appeared in a diff unless you already knew the old route.
 * Writing the generated spec to {@code docs/openapi.json} turns every route change into a
 * reviewable diff, and the assertions below turn the security rules into something that fails the
 * build instead of something to remember.
 *
 * <p>Regenerate with {@code mvnw.cmd test -Dtest=OpenApiContractTest} and commit the result
 * alongside the controller change.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenApiContractTest {

    private static final Path SPEC = Path.of("docs", "openapi.json");

    /** HTTP methods that can appear under a path in an OpenAPI document. */
    private static final Set<String> METHODS = Set.of("get", "post", "put", "patch", "delete", "head", "options");

    /**
     * Routes that predate the {@code /api} prefix. They are still mounted outside the group the
     * security rules are written against, and they are expected to disappear as the remaining
     * resources are migrated. Naming them here makes their removal a deliberate, visible change
     * rather than a silent one: adding a new unprefixed route fails this test.
     *
     * <p>{@code /payments} left this list in Batch E, {@code /vendor-profiles} in Batch D, and
     * {@code /users} when the account routes moved to {@code /api/users} and the entity-taking
     * writes were dropped. Empty now, so this list stays as the tripwire it was written to be.
     */
    private static final List<String> KNOWN_LEGACY_PREFIXES = List.of();

    /**
     * Ownership-taking routes that are known to be vulnerable and not yet fixed. Each one is a
     * real bug, listed here only so the build stays green while they are outstanding.
     *
     * <p>This list is a self-removing to-do. The assertions below require the set of vulnerable
     * routes to equal this list exactly, so when a batch fixes one the build fails until the
     * entry is deleted. Do not relax the assertions to accommodate a new vulnerable route.
     *
     * <p>Empty now. Batch E removed {@code /payments/user/{userId}}, and Batch D removed
     * {@code /vendor-profiles/user/{userId}}: every owner in the API is derived from the token, so
     * no route takes a user id to decide what a caller may see or change. Adding an entry here
     * should mean re-introducing a bug.
     */
    private static final List<String> KNOWN_UNFIXED_IDOR_ROUTES = List.of();

    /** Endpoints that mint or reset credentials, so a token cannot be required to reach them. */
    private static final List<String> PUBLIC_AUTH_ROUTES = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/verify",
            "/api/auth/resend-code",
            "/api/auth/forgot-password",
            "/api/auth/reset-password");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Map<String, Object> spec;
    private static Map<String, Object> paths;

    @BeforeAll
    @SuppressWarnings("unchecked")
    void captureSpec() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        spec = objectMapper.readValue(body, Map.class);
        paths = (Map<String, Object>) spec.get("paths");
        writeSpecFile();
    }

    /**
     * Writes the spec to disk so the route list is reviewable in a diff rather than inferred from
     * the controllers. A missing directory on a fresh clone is normal and is created here.
     */
    private void writeSpecFile() throws IOException {
        Files.createDirectories(SPEC.getParent());
        Files.writeString(SPEC,
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec) + System.lineSeparator());
    }

    private Set<String> routePaths() {
        return new TreeSet<>(paths.keySet());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> operation(String path, String method) {
        Object operations = paths.get(path);
        if (!(operations instanceof Map)) {
            return null;
        }
        Object operation = ((Map<String, Object>) operations).get(method);
        return operation instanceof Map ? (Map<String, Object>) operation : null;
    }

    /** The declared properties of a component schema, or an empty map when it is not declared. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> schemaProperties(String name) {
        Object components = spec.get("components");
        if (!(components instanceof Map)) {
            return Map.of();
        }
        Object schemas = ((Map<String, Object>) components).get("schemas");
        if (!(schemas instanceof Map)) {
            return Map.of();
        }
        Object schema = ((Map<String, Object>) schemas).get(name);
        if (!(schema instanceof Map)) {
            return Map.of();
        }
        Object properties = ((Map<String, Object>) schema).get("properties");
        return properties instanceof Map ? (Map<String, Object>) properties : Map.of();
    }

    /**
     * One declared property of a component schema.
     *
     * <p>READ_ONLY fields still appear here, which is correct: {@code verified} is worth telling a
     * client about, it just must not be settable. Returning the raw map lets each assertion say
     * which of those two things it is checking.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> schemaProperty(String schema, String property) {
        Object value = schemaProperties(schema).get(property);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    /** Collects every declared parameter across every operation, as {@code path [METHOD] name}. */
    @SuppressWarnings("unchecked")
    private List<String> parameterNames() {
        List<String> found = new ArrayList<>();
        paths.forEach((path, operations) -> {
            if (!(operations instanceof Map)) {
                return;
            }
            ((Map<String, Object>) operations).forEach((method, operation) -> {
                if (!METHODS.contains(method) || !(operation instanceof Map)) {
                    return;
                }
                Object parameters = ((Map<String, Object>) operation).get("parameters");
                if (!(parameters instanceof List)) {
                    return;
                }
                for (Object parameter : (List<Object>) parameters) {
                    if (parameter instanceof Map<?, ?> map && map.get("name") instanceof String name) {
                        found.add(path + " [" + method.toUpperCase() + "] " + name);
                    }
                }
            });
        });
        return found;
    }

    @Test
    @DisplayName("the spec is written to docs/openapi.json so route changes are reviewable")
    void specIsWritten() {
        assertThat(Files.exists(SPEC))
                .as("regenerate from the backend module with mvnw.cmd test -Dtest=OpenApiContractTest")
                .isTrue();
        assertThat(routePaths()).isNotEmpty();
    }

    @Test
    @DisplayName("the vulnerable ownership routes are exactly the ones we already know about")
    void ownershipRoutesAreKnownAndAccountedFor() {
        Set<String> vulnerable = new TreeSet<>(routePaths().stream()
                .filter(path -> path.contains("{userId}"))
                .toList());

        assertThat(vulnerable)
                .as("no route may take the owner from the path; delete this entry once a listed route is fixed")
                .isEqualTo(new TreeSet<>(KNOWN_UNFIXED_IDOR_ROUTES));
    }

    @Test
    @DisplayName("no route takes an owner from a userId query parameter")
    void noRouteTakesAUserIdQueryParameterOutsideTheKnownOnes() {
        List<String> offenders = parameterNames().stream()
                .filter(entry -> entry.endsWith(" userId"))
                .map(entry -> entry.substring(0, entry.lastIndexOf(" [")))
                .distinct()
                .filter(path -> !KNOWN_UNFIXED_IDOR_ROUTES.contains(path))
                .toList();

        assertThat(offenders)
                .as("ownership must come from the token, never from a userId query parameter")
                .isEmpty();
    }

    @Test
    @DisplayName("the unprefixed routes removed during hardening do not come back")
    void removedRoutesStayRemoved() {
        // Each of these was mounted outside /api and exposed ownership in the body or the path.
        for (String gone : List.of("/notifications", "/bulletin-posts", "/reports")) {
            assertThat(routePaths()).as("%s was removed and must stay removed", gone).doesNotContain(gone);
        }
    }

    @Test
    @DisplayName("the caller-scoped replacements are present")
    void callerScopedRoutesExist() {
        assertThat(routePaths()).contains(
                "/api/notifications",
                "/api/bulletin-posts",
                "/api/reports",
                "/api/comments",
                "/api/post-likes",
                "/api/reviews",
                "/api/orders",
                "/api/payments",
                "/api/order-items/{id}",
                "/api/order-items/order/{orderId}");
    }

    @Test
    @DisplayName("the payment routes that leaked other accounts are gone")
    void paymentLeakRoutesAreGone() {
        assertThat(routePaths())
                .as("these returned or accepted another account's payments")
                .doesNotContain("/payments", "/payments/user/{userId}");
    }

    @Test
    @DisplayName("line items cannot be written through the API")
    void orderItemWritesAreNotExposed() {
        // Line items are built at checkout and the order total is derived from them, so a route
        // that accepts one from a client is a route that lets a client set its own price.
        Map<String, Object> collection = operation("/api/order-items", "post");
        Map<String, Object> update = operation("/api/order-items", "put");

        assertThat(collection).as("POST /api/order-items must not exist").isNull();
        assertThat(update).as("PUT /api/order-items must not exist").isNull();
        assertThat(operation("/api/order-items/{id}", "delete"))
                .as("DELETE /api/order-items/{id} must not exist").isNull();
    }

    @Test
    @DisplayName("the seller routes are prefixed and take no owner from the path")
    void sellerRoutesAreCallerScoped() {
        assertThat(routePaths()).contains(
                "/api/vendor-profiles",
                "/api/vendor-profiles/{id}",
                "/api/vendor-profiles/me",
                "/api/products/mine");

        assertThat(routePaths())
                .as("the user-id lookup let any caller map an account to the business behind it")
                .doesNotContain("/vendor-profiles", "/vendor-profiles/user/{userId}",
                        "/api/vendor-profiles/user/{userId}");
    }

    @Test
    @DisplayName("products and vendor profiles cannot be deleted by id")
    void catalogueDestructiveRoutesAreNotExposed() {
        // A product is referenced by the order lines that bought it and a profile by every product
        // listed under it, both through non-nullable foreign keys. A delete route could only ever
        // fail on those keys, so the routes are gone and listings are retired through active=false.
        assertThat(operation("/api/products/{id}", "delete"))
                .as("DELETE /api/products/{id} must not exist").isNull();
        assertThat(operation("/api/vendor-profiles/{id}", "delete"))
                .as("DELETE /api/vendor-profiles/{id} must not exist").isNull();
    }

    @Test
    @DisplayName("product images are created against a product in the path, not one in the body")
    void productImageCreationNamesTheProductInThePath() {
        assertThat(routePaths())
                .as("POST /api/product-images took the product from the request body")
                .doesNotContain("/api/product-images");

        assertThat(operation("/api/product-images/product/{productId}", "post"))
                .as("the owning product must come from the path").isNotNull();
    }

    @Test
    @DisplayName("the bare product-image listing that dumped every seller is gone")
    void productImageGlobalListingIsGone() {
        assertThat(operation("/api/product-images", "get"))
                .as("GET /api/product-images must not exist").isNull();
    }

    @Test
    @DisplayName("product responses do not declare the lazy image collection")
    void productDoesNotDeclareItsImageCollection() {
        assertThat(schemaProperties("Product"))
                .as("open-in-view is off, so serialising Product.images was a 500")
                .doesNotContainKey("images");
    }

    @Test
    @DisplayName("the verification flag is readable but never writable")
    void vendorProfileVerifiedIsReadOnly() {
        Map<String, Object> verified = schemaProperty("VendorProfile", "verified");

        assertThat(verified)
                .as("verified is a useful output field, so it stays in the schema")
                .isNotNull();
        assertThat(verified)
                .as("a seller that could post verified:true would be marking itself trusted")
                .containsEntry("readOnly", Boolean.TRUE);

        assertThat(schemaProperties("VendorProfile"))
                .as("the owner's account is never exposed in either direction")
                .doesNotContainKey("user");
    }

    @Test
    @DisplayName("the product's vendor is readable but never writable")
    void productVendorIsReadOnly() {
        Map<String, Object> vendor = schemaProperty("Product", "vendor");

        assertThat(vendor)
                .as("buyers need to see who is selling")
                .isNotNull();
        assertThat(vendor)
                .as("the vendor is derived from the token, so it cannot be posted")
                .containsEntry("readOnly", Boolean.TRUE);
    }

    @Test
    @DisplayName("the user entity does not carry a vendor profile in either direction")
    void userVendorProfileIsNotExposed() {
        // VendorProfile.user is ignored, and this closes the pair. Leaving the inverse side
        // reachable meant a user request body could attach a profile to its own account and skip
        // the ownership and verification rules VendorProfileService applies.
        assertThat(schemaProperties("User"))
                .as("a vendor profile is created and read through /api/vendor-profiles")
                .doesNotContainKey("vendorProfile");
    }

    @Test
    @DisplayName("change-password names no account, so no address parameter can appear on it")
    void changePasswordTakesNoAccountParameter() {
        List<String> onThisRoute = parameterNames().stream()
                .filter(name -> name.startsWith("/api/auth/change-password [POST] "))
                .map(name -> name.substring("/api/auth/change-password [POST] ".length()))
                .toList();

        assertThat(onThisRoute)
                .as("the account comes from the token; an email parameter let any authenticated "
                        + "caller aim the request at another account and turned the endpoint into "
                        + "an account-existence oracle")
                .doesNotContain("email");
        assertThat(onThisRoute)
                .as("the caller still has to prove they know the current password")
                .contains("currentPassword", "newPassword");
    }

    @Test
    @DisplayName("every unprefixed route belongs to a resource we still have to migrate")
    void everythingElseIsPrefixed() {
        Set<String> unaccounted = routePaths().stream()
                .filter(path -> !path.startsWith("/api"))
                // Framework-provided routes are not ours to prefix.
                .filter(path -> !path.startsWith("/v3"))
                .filter(path -> !path.startsWith("/swagger-ui"))
                .filter(path -> !path.equals("/error"))
                .filter(path -> KNOWN_LEGACY_PREFIXES.stream().noneMatch(path::startsWith))
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));

        assertThat(unaccounted)
                .as("these routes sit outside /api and are not on the migration list")
                .isEmpty();
    }

    @Test
    @DisplayName("the bearer scheme is documented so a generated client can authenticate")
    @SuppressWarnings("unchecked")
    void bearerSchemeIsDocumented() {
        Map<String, Object> components = (Map<String, Object>) spec.get("components");
        assertThat(components).isNotNull();
        Map<String, Object> schemes = (Map<String, Object>) components.get("securitySchemes");
        assertThat(schemes).containsKey("bearerAuth");

        Map<String, Object> bearer = (Map<String, Object>) schemes.get("bearerAuth");
        assertThat(bearer)
                .containsEntry("type", "http")
                .containsEntry("scheme", "bearer")
                .containsEntry("bearerFormat", "JWT");
    }

    @Test
    @DisplayName("credential endpoints are documented as public")
    void credentialEndpointsArePublic() {
        for (String path : PUBLIC_AUTH_ROUTES) {
            Map<String, Object> operation = operation(path, "post");
            assertThat(operation).as("%s should be documented", path).isNotNull();
            assertThat(operation.get("security"))
                    .as("%s mints or resets credentials, so requiring a token to reach it is wrong", path)
                    .isEqualTo(List.of());
        }
    }
}