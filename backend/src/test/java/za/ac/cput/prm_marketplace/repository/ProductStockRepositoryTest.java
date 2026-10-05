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
 * Runs stock reservation against a real database.
 *
 * <p>{@code decrementStock} exists to stop two concurrent checkouts from both selling the last item.
 * The claim is about what the database does under contention, not about what the service asks for, so
 * a mocked repository test cannot make it: with a mock, every caller is handed whatever the test told
 * it to return and the {@code stockQuantity >= :quantity} predicate is never evaluated by anything.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductStockRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VendorProfileRepository vendorProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private VendorProfile vendor;

    @BeforeEach
    void setUp() {
        User seller = userRepository.save(new User.Builder()
                .setName("seller")
                .setEmail("seller-" + UUID.randomUUID() + "@example.com")
                .setPasswordHash("hash")
                .setRole(Role.VENDOR)
                .setVerified(true)
                .build());
        vendor = vendorProfileRepository.save(new VendorProfile.Builder()
                .setUser(seller)
                .setBusinessName("Block B Books")
                .setRegistrationNo("REG-" + UUID.randomUUID())
                .build());
    }

    @Test
    @DisplayName("reserving stock takes it off the listing in the same statement")
    void decrementStock_subtractsTheReservedQuantity() {
        Product listed = saveProduct("Calculus textbook", 5, true);

        int updated = productRepository.decrementStock(listed.getId(), 2);

        assertThat(updated).as("one row matched, so the reservation was allowed").isEqualTo(1);
        assertThat(stockOf(listed)).isEqualTo(3);
    }

    @Test
    @DisplayName("asking for more than is left matches no rows and leaves the listing alone")
    void decrementStock_refusesToGoNegative() {
        Product listed = saveProduct("Statistics workbook", 1, true);

        int updated = productRepository.decrementStock(listed.getId(), 2);

        assertThat(updated)
                .as("zero rows is how the second of two racing checkouts is turned away")
                .isZero();
        assertThat(stockOf(listed))
                .as("the column must still hold what it held, not the requested negative")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("reserving the last item succeeds and a second attempt does not")
    void decrementStock_allowsTheLastItemExactlyOnce() {
        Product listed = saveProduct("Lab coat", 1, true);

        assertThat(productRepository.decrementStock(listed.getId(), 1)).isEqualTo(1);
        assertThat(productRepository.decrementStock(listed.getId(), 1))
                .as("this is the oversell: two buyers, one coat")
                .isZero();
        assertThat(stockOf(listed)).isZero();
    }

    @Test
    @DisplayName("a delisted product cannot be reserved, whatever stock it shows")
    void decrementStock_refusesAnInactiveListing() {
        Product delisted = saveProduct("Out of print notes", 9, false);

        int updated = productRepository.decrementStock(delisted.getId(), 1);

        assertThat(updated)
                .as("an inactive row still has a stock column, and the predicate has to notice")
                .isZero();
        assertThat(stockOf(delisted)).isEqualTo(9);
    }

    @Test
    @DisplayName("reserving a product that does not exist matches nothing rather than failing")
    void decrementStock_onAnUnknownId_isZero() {
        assertThat(productRepository.decrementStock(UUID.randomUUID(), 1)).isZero();
    }

    @Test
    @DisplayName("cancelling an order puts the reserved stock back")
    void incrementStock_returnsTheReservedQuantity() {
        Product listed = saveProduct("Ruler", 4, true);
        productRepository.decrementStock(listed.getId(), 3);

        int updated = productRepository.incrementStock(listed.getId(), 3);

        assertThat(updated).isEqualTo(1);
        assertThat(stockOf(listed)).isEqualTo(4);
    }

    @Test
    @DisplayName("a listing with no stock at all is still listed by the low-stock query")
    void findByStockQuantityLessThanEqual_includesZeroAndTheThreshold() {
        Product out = saveProduct("Sold out", 0, true);
        Product atThreshold = saveProduct("Last one", 2, true);
        saveProduct("Plenty", 5, true);

        assertThat(productRepository.findByStockQuantityLessThanEqual(2))
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Sold out", "Last one");
        assertThat(productRepository.findByStockQuantityLessThanEqual(2))
                .as("the boundary is inclusive, otherwise a listing at the threshold never alerts")
                .contains(atThreshold, out);
    }

    /**
     * Re-reads through the repository because the modifying queries clear the persistence context.
     * Reading the field off the instance handed to the test would report the value it was constructed
     * with, which is the bug this whole file exists to rule out.
     */
    private int stockOf(Product product) {
        return productRepository.findById(product.getId()).orElseThrow().getStockQuantity();
    }

    private Product saveProduct(String name, int stock, boolean active) {
        return productRepository.save(new Product.Builder()
                .vendor(vendor)
                .name(name)
                .description("Second edition.")
                .price(new BigDecimal("250"))
                .stockQuantity(stock)
                .category("Books")
                .condition(ProductCondition.GOOD)
                .active(active)
                .build());
    }
}