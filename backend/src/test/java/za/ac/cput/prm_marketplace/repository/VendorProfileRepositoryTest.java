package za.ac.cput.prm_marketplace.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductCondition;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the vendor-profile owner queries against a real database, for the reason
 * {@link CommentRepositoryTest} exists: a derived query that does not parse fails at runtime, not at
 * compile time, so mocked tests stay green while the endpoint answers 500.
 *
 * <p>This is the second time this exact trap has been set. {@code VendorProfile} gained a read-only
 * {@code getUserId()} so the frontend could reach {@code POST /conversations/start}, and that was
 * enough for Spring Data to resolve {@code findByUserId} against the bean property instead of the
 * {@code user} association. There is no {@code userId} field on the entity, so all three owner
 * queries threw {@code Could not resolve attribute 'userId'} and {@code GET /api/products/mine} — the
 * page a seller needs to edit their own listing — returned 500. These are now explicit queries naming
 * {@code v.user.id}, and this test is what fails if that ever reverts.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VendorProfileRepositoryTest {

    @Autowired
    private VendorProfileRepository vendorProfileRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private User seller;
    private VendorProfile profile;

    @BeforeEach
    void setUp() {
        seller = userRepository.save(buildUser("Seller", Role.VENDOR));
        profile = vendorProfileRepository.save(new VendorProfile.Builder()
                .setUser(seller)
                .setBusinessName("Campus Books")
                .setRegistrationNo("REG-1")
                .setVerified(true)
                .build());
    }

    @Test
    @DisplayName("a profile is found by the account that owns it")
    void findByUserId_returnsTheCallersProfile() {
        assertThat(vendorProfileRepository.findByUserId(seller.getId()))
                .hasValueSatisfying(found -> {
                    assertThat(found.getId()).isEqualTo(profile.getId());
                    assertThat(found.getBusinessName()).isEqualTo("Campus Books");
                });
    }

    @Test
    @DisplayName("an account with no profile is reported as having none")
    void findByUserId_isEmptyForAnAccountWithoutOne() {
        User student = userRepository.save(buildUser("Student", Role.STUDENT));

        assertThat(vendorProfileRepository.findByUserId(student.getId())).isEmpty();
        assertThat(vendorProfileRepository.existsByUserId(student.getId())).isFalse();
    }

    @Test
    @DisplayName("existence follows the owner, not the presence of any profile")
    void existsByUserId_isTrueOnlyForTheOwner() {
        User student = userRepository.save(buildUser("Student", Role.STUDENT));

        assertThat(vendorProfileRepository.existsByUserId(seller.getId())).isTrue();
        assertThat(vendorProfileRepository.existsByUserId(student.getId())).isFalse();
    }

    @Test
    @DisplayName("the owner filter on an id is what keeps one seller out of another's profile")
    void findByIdAndUserId_requiresBothToMatch() {
        User other = userRepository.save(buildUser("Other", Role.VENDOR));

        assertThat(vendorProfileRepository.findByIdAndUserId(profile.getId(), seller.getId()))
                .isPresent();
        assertThat(vendorProfileRepository.findByIdAndUserId(profile.getId(), other.getId()))
                .as("a seller holding another seller's profile id must not load it")
                .isEmpty();
    }

    @Test
    @DisplayName("the owner id the response carries is the account behind the profile")
    void userId_accessorReportsTheOwningAccount() {
        assertThat(profile.getUserId()).isEqualTo(seller.getId());
    }

    @Test
    @DisplayName("a seller's own listings are still listed, which is the query that broke")
    void productRepositoryByVendorUserId_stillFindsTheListings() {
        saveProduct("Calculus textbook");
        saveProduct("Statistics workbook");

        // GET /api/products/mine resolves the profile through findByUserId and then lists by vendor.
        // That is the path that answered 500 while every mocked test passed. The derived form of this
        // query compiled to `where v.userId = :userId` once VendorProfile gained a userId property,
        // so it needed the explicit traversal too — see ProductRepository.
        assertThat(vendorProfileRepository.findByUserId(seller.getId())).isPresent();
        assertThat(productRepository.findByVendorUserIdOrderByCreatedAtDesc(seller.getId()))
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Calculus textbook", "Statistics workbook");
    }

    @Test
    @DisplayName("the ownership filter behind every product write still excludes other sellers")
    void productRepositoryOwnershipCheck_isNotWeakenedByThePropertyOfTheSameName() {
        Product listed = saveProduct("Calculus textbook");
        User other = userRepository.save(buildUser("Other", Role.VENDOR));

        assertThat(productRepository.findByIdAndVendorUserId(listed.getId(), seller.getId()))
                .as("the owner must still load their own listing to edit it")
                .isPresent();
        assertThat(productRepository.findByIdAndVendorUserId(listed.getId(), other.getId()))
                .as("this predicate is what stops one seller editing another's listing")
                .isEmpty();
    }

    private Product saveProduct(String name) {
        return productRepository.save(new Product.Builder()
                .vendor(profile)
                .name(name)
                .description("Second edition.")
                .price(new BigDecimal("250"))
                .stockQuantity(3)
                .category("Books")
                .condition(ProductCondition.GOOD)
                .build());
    }

    private User buildUser(String prefix, Role role) {
        return new User.Builder()
                .setName(prefix)
                .setEmail(prefix.toLowerCase() + "-" + UUID.randomUUID() + "@example.com")
                .setPasswordHash("hash")
                .setRole(role)
                .setVerified(true)
                .build();
    }
}