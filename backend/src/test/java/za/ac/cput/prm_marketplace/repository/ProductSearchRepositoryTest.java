package za.ac.cput.prm_marketplace.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductCondition;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.dto.ProductSearchCriteria;
import za.ac.cput.prm_marketplace.service.ProductServiceImpl;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the catalogue search specification against a real database. The mocked service
 * tests cannot prove that the generated JPQL actually runs, so these cases cover the query
 * semantics: which products match, how the total is counted, and that paging splits results.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductSearchRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorProfileRepository vendorProfileRepository;

    private VendorProfile vendor;

    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(productRepository, vendorProfileRepository);

        User user = userRepository.save(new User.Builder()
                .setName("Vendor Owner")
                .setEmail("vendor-" + System.nanoTime() + "@example.com")
                .setPasswordHash("hash")
                .setRole(Role.VENDOR)
                .setCampus("CPUT - Bellville")
                .setVerified(true)
                .build());

        vendor = vendorProfileRepository.save(new VendorProfile.Builder()
                .setUser(user)
                .setBusinessName("Campus Books")
                .setVerified(true)
                .build());
    }

    private Product product(String name, String category, String city,
                            String price, ProductCondition condition, boolean active) {
        return productRepository.save(new Product.Builder()
                .name(name)
                .description("Description of " + name)
                .category(category)
                .city(city)
                .price(new BigDecimal(price))
                .stockQuantity(5)
                .condition(condition)
                .active(active)
                .vendor(vendor)
                .build());
    }

    /** Disambiguates findAll(Specification, Pageable) from the other findAll overloads. */
    private Page<Product> allMatching(Specification<Product> specification, Pageable pageable) {
        return productRepository.findAll(specification, pageable);
    }

    @Test
    @DisplayName("an empty specification returns the whole catalogue")
    void noFilters_returnsEverything() {
        product("Algebra Textbook", "Books", "Cape Town", "120.00", ProductCondition.GOOD, true);
        product("Desk Lamp", "Furniture", "Durban", "80.00", ProductCondition.NEW, true);
        product("Graphing Calculator", "Electronics", "Cape Town", "450.00", ProductCondition.LIKE_NEW, true);

        Page<Product> page = allMatching(null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("keyword matches the product name case insensitively")
    void keyword_matchesName() {
        product("Algebra Textbook", "Books", "Cape Town", "120.00", ProductCondition.GOOD, true);
        product("Desk Lamp", "Furniture", "Durban", "80.00", ProductCondition.NEW, true);

        Page<Product> page = allMatching(
                (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%textbook%"),
                PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("Algebra Textbook");
    }

    @Test
    @DisplayName("keyword also matches the description")
    void keyword_matchesDescription() {
        product("Desk Lamp", "Furniture", "Durban", "80.00", ProductCondition.NEW, true);
        product("Study Podio", "Furniture", "Durban", "95.00", ProductCondition.NEW, true);

        Page<Product> page = allMatching(
                (root, query, cb) -> cb.like(cb.lower(root.get("description")), "%of desk%"),
                PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("Desk Lamp");
    }

    @Test
    @DisplayName("price bounds are inclusive on both ends")
    void priceBounds_areInclusive() {
        product("Cheap", "Books", "Cape Town", "50.00", ProductCondition.GOOD, true);
        product("Mid", "Books", "Cape Town", "100.00", ProductCondition.GOOD, true);
        product("Expensive", "Books", "Cape Town", "500.00", ProductCondition.GOOD, true);

        Page<Product> page = allMatching(
                (root, query, cb) -> cb.between(root.get("price"),
                        new BigDecimal("50.00"), new BigDecimal("100.00")),
                PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Product::getName)
                .containsExactlyInAnyOrder("Cheap", "Mid");
    }

    @Test
    @DisplayName("paging splits results and reports the full total")
    void paging_splitsResults() {
        for (int i = 0; i < 7; i++) {
            product("Book " + i, "Books", "Cape Town", "10.00", ProductCondition.GOOD, true);
        }

        Page<Product> first = allMatching(null, PageRequest.of(0, 3));
        Page<Product> second = allMatching(null, PageRequest.of(1, 3));

        assertThat(first.getContent()).hasSize(3);
        assertThat(first.getTotalElements()).isEqualTo(7);
        assertThat(first.getTotalPages()).isEqualTo(3);
        assertThat(first.isFirst()).isTrue();
        assertThat(first.isLast()).isFalse();

        assertThat(second.getContent()).hasSize(3);
        assertThat(second.isFirst()).isFalse();

        assertThat(first.getContent()).doesNotContainAnyElementsOf(second.getContent());
    }

    @Test
    @DisplayName("sorting by price ascending orders the page contents")
    void sorting_ordersResults() {
        product("Expensive", "Books", "Cape Town", "500.00", ProductCondition.GOOD, true);
        product("Cheap", "Books", "Cape Town", "50.00", ProductCondition.GOOD, true);
        product("Mid", "Books", "Cape Town", "100.00", ProductCondition.GOOD, true);

        Page<Product> page = allMatching(null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "price")));

        assertThat(page.getContent()).extracting(Product::getName)
                .containsExactly("Cheap", "Mid", "Expensive");
    }

    @Test
    @DisplayName("an out of range page is empty rather than an error")
    void pageBeyondLast_returnsEmpty() {
        product("Only", "Books", "Cape Town", "10.00", ProductCondition.GOOD, true);

        Page<Product> page = allMatching(null, PageRequest.of(5, 20));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    /**
     * The remaining cases drive the real {@link ProductServiceImpl} rather than a hand-written
     * specification, because the interesting behaviour (LIKE escaping, case folding) only exists
     * once the service has turned the criteria into a query.
     */
    @Test
    @DisplayName("a bare percent sign is searched for literally instead of matching every product")
    void keywordWildcard_isTreatedAsALiteral() {
        product("Algebra Textbook", "Books", "Cape Town", "120.00", ProductCondition.GOOD, true);
        product("Desk Lamp", "Furniture", "Durban", "80.00", ProductCondition.NEW, true);

        Page<Product> page = service.search(criteria("%", null, null, null, null, null));

        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("an underscore is searched for literally instead of matching any single character")
    void keywordUnderscore_isTreatedAsALiteral() {
        product("Graphing Calculator", "Electronics", "Cape Town", "450.00", ProductCondition.LIKE_NEW, true);

        // "Study_Podio" would match "Study Podio" if the underscore were a wildcard.
        assertThat(service.search(criteria("Study_Podio", null, null, null, null, null))
                .getTotalElements()).isZero();
    }

    @Test
    @DisplayName("a real keyword containing an underscore still matches")
    void keywordUnderscore_stillMatchesLiteralText() {
        product("Study_Podio", "Furniture", "Durban", "95.00", ProductCondition.NEW, true);

        assertThat(service.search(criteria("Study_Podio", null, null, null, null, null))
                .getContent()).hasSize(1);
    }

    @Test
    @DisplayName("category is matched case insensitively, like city and keyword already are")
    void category_isMatchedCaseInsensitively() {
        product("Laptop", "Electronics", "Cape Town", "8999.99", ProductCondition.NEW, true);
        product("Desk Lamp", "Furniture", "Durban", "80.00", ProductCondition.NEW, true);

        assertThat(service.search(criteria(null, "electronics", null, null, null, null))
                .getContent()).extracting(Product::getName).containsExactly("Laptop");

        assertThat(service.search(criteria(null, "ELECTRONICS", null, null, null, null))
                .getContent()).extracting(Product::getName).containsExactly("Laptop");
    }

    @Test
    @DisplayName("a comma-separated category filter matches any listed category")
    void categories_matchAnyListedValue() {
        product("Laptop", "Electronics", "Cape Town", "8999.99", ProductCondition.NEW, true);
        product("Desk Lamp", "Furniture", "Durban", "80.00", ProductCondition.NEW, true);
        product("Tutoring", "Tutoring and academic help", "Cape Town", "150.00", ProductCondition.GOOD, true);

        assertThat(service.search(criteria(null, "ELECTRONICS, furniture", null, null, null, null))
                .getContent()).extracting(Product::getName).containsExactlyInAnyOrder("Laptop", "Desk Lamp");
    }

    @Test
    @DisplayName("campus filter matches the seller campus case insensitively")
    void campus_matchesSellerProfile() {
        product("Bellville Book", "Books", "Cape Town", "120.00", ProductCondition.GOOD, true);

        assertThat(service.search(criteria(null, null, "cput - bellville"))
                .getContent()).extracting(Product::getName).containsExactly("Bellville Book");
        assertThat(service.search(criteria(null, null, "UWC - Bellville")).getTotalElements()).isZero();
    }

    private ProductSearchCriteria criteria(String keyword, String category, String minPrice, String maxPrice,
                                           ProductCondition condition, Boolean activeOnly) {
        return criteria(keyword, category, null, minPrice, maxPrice, condition, activeOnly);
    }

    private ProductSearchCriteria criteria(String keyword, String category, String campus) {
        return criteria(keyword, category, campus, null, null, null, null);
    }

    private ProductSearchCriteria criteria(String keyword, String category, String campus,
                                           String minPrice, String maxPrice,
                                           ProductCondition condition, Boolean activeOnly) {
        return new ProductSearchCriteria(
                keyword,
                category,
                null,
                campus,
                minPrice == null ? null : new BigDecimal(minPrice),
                maxPrice == null ? null : new BigDecimal(maxPrice),
                condition,
                activeOnly,
                0,
                20,
                "name",
                "asc");
    }
}
