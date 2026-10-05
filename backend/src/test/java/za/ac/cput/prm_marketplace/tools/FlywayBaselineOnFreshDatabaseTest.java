package za.ac.cput.prm_marketplace.tools;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.ReviewRepository;
import za.ac.cput.prm_marketplace.service.VendorRatingService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the Flyway V1 baseline against a throwaway database to prove the script
 * executes cleanly from nothing. This is the path a fresh deployment takes, which
 * the pre-existing development database never exercises because it is baselined.
 *
 * <p>Opt-in, because the script under test is MySQL-specific: it uses {@code enum(...)}
 * column types and {@code information_schema}, none of which H2 accepts. The rest of the
 * suite runs against an in-memory database, so this test is the only thing standing between
 * a broken migration and production. It therefore still needs to be run deliberately:
 *
 * <pre>
 * RUN_MYSQL_INTEGRATION_TESTS=true mvnw.cmd verify
 * </pre>
 *
 * The MySQL credentials come from the environment rather than being committed.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/prm_marketplace_flywaytest?createDatabaseIfNotExist=true",
        // The "test" profile points the datasource at H2, so the driver and dialect have to be
        // overridden back as well. Setting only the URL would leave H2 trying to parse a MySQL URL.
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.datasource.username=${DB_USERNAME:root}",
        "spring.datasource.password=${DB_PASSWORD:password}",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false",
        "spring.flyway.clean-disabled=false"
})
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION_TESTS", matches = "(?i)true")
class FlywayBaselineOnFreshDatabaseTest {

    private static final String SCRATCH_DATABASE = "prm_marketplace_flywaytest";

    private static String mysqlUsername() {
        return envOrDefault("DB_USERNAME", "root");
    }

    private static String mysqlPassword() {
        return envOrDefault("DB_PASSWORD", "password");
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private VendorRatingService vendorRatingService;

    @Test
    @DisplayName("V1 creates every table the entity model needs on an empty database")
    void v1CreatesSchemaOnEmptyDatabase() {
        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = 1 and version = '1'",
                Integer.class);
        assertThat(applied).isEqualTo(1);

        // A representative table from every schema area.
        for (String table : new String[]{
                "users", "addresses", "vendor_profiles", "verification_codes", "password_reset_tokens",
                "products", "product_images", "cart_items", "saved_items",
                "orders", "order_items", "payments",
                "bulletin_posts", "comments", "post_likes", "reviews",
                "conversations", "messages", "notifications", "reports"}) {
            Integer rows = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = database() and table_name = ?",
                    Integer.class, table);
            assertThat(rows).as("table %s", table).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("the generated schema satisfies the JPA mappings")
    void generatedSchemaSatisfiesMappings() {
        assertThat(entityManagerFactory.isOpen()).isTrue();

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            assertThat(em.getMetamodel().entity(User.class)).isNotNull();
        } finally {
            em.close();
        }
    }

    @Test
    @DisplayName("the vendor rating aggregate runs on MySQL and stores the average and count")
    void vendorRatingAggregateRunsOnMysql() {
        // The rest of the suite runs on H2, which accepts a good deal of JPQL that MySQL rejects, and
        // this is the only query in the review code that reaches through two tables and aggregates.
        // It was written without ever being executed, so this is where it gets found out.
        UUID vendorId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID reviewerOne = UUID.randomUUID();
        UUID reviewerTwo = UUID.randomUUID();
        UUID firstProduct = UUID.randomUUID();
        UUID secondProduct = UUID.randomUUID();

        jdbcTemplate.update("insert into users (id, email, name, password_hash, verified) values (?, ?, ?, ?, true)",
                uuid(ownerId), "owner@example.ac.za", "Owner", "hash");
        // Reviews reference their author, so both reviewers need an account of their own.
        for (UUID reviewer : new UUID[]{reviewerOne, reviewerTwo}) {
            jdbcTemplate.update("insert into users (id, email, name, password_hash, verified) values (?, ?, ?, ?, true)",
                    uuid(reviewer), UUID.randomUUID() + "@example.ac.za", "Reviewer", "hash");
        }
        jdbcTemplate.update(
                "insert into vendor_profiles (id, user_id, business_name, verified, created_at) values (?, ?, ?, ?, now(6))",
                uuid(vendorId), uuid(ownerId), "Acme Repairs", false);
        for (UUID product : new UUID[]{firstProduct, secondProduct}) {
            jdbcTemplate.update(
                    "insert into products (id, vendor_id, name, price, stock_quantity, active) values (?, ?, ?, 1.00, 1, true)",
                    uuid(product), uuid(vendorId), "A listing");
        }
        // 5 and 4 across two listings, so the join has to span products rather than one.
        jdbcTemplate.update("insert into reviews (id, product_id, reviewer_id, rating, comment) values (?, ?, ?, 5, 'Great')",
                uuid(UUID.randomUUID()), uuid(firstProduct), uuid(reviewerOne));
        jdbcTemplate.update("insert into reviews (id, product_id, reviewer_id, rating, comment) values (?, ?, ?, 4, 'Fine')",
                uuid(UUID.randomUUID()), uuid(secondProduct), uuid(reviewerTwo));

        Double average = reviewRepository.averageRatingForVendor(vendorId);
        long count = reviewRepository.reviewCountForVendor(vendorId);

        assertThat(count).isEqualTo(2L);
        assertThat(average).isEqualTo(4.5d);

        vendorRatingService.refresh(vendorId);

        BigDecimal storedAvg = jdbcTemplate.queryForObject(
                "select rating_avg from vendor_profiles where id = ?", BigDecimal.class, uuid(vendorId));
        Integer storedCount = jdbcTemplate.queryForObject(
                "select rating_count from vendor_profiles where id = ?", Integer.class, uuid(vendorId));
        assertThat(storedAvg).isEqualByComparingTo("4.50");
        assertThat(storedCount).isEqualTo(2);
    }

    @Test
    @DisplayName("the last review being removed takes the stored average back to null")
    void removingTheLastReviewClearsTheAverageOnMysql() {
        // Incremental averaging cannot do this correctly. It is the reason the whole number is
        // recalculated rather than adjusted, so it is worth proving the recalculation reaches the
        // database and not just a mocked repository.
        UUID vendorId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        jdbcTemplate.update("insert into users (id, email, name, password_hash, verified) values (?, ?, ?, ?, true)",
                uuid(ownerId), "owner2@example.ac.za", "Owner", "hash");
        jdbcTemplate.update("insert into users (id, email, name, password_hash, verified) values (?, ?, ?, ?, true)",
                uuid(reviewerId), "buyer2@example.ac.za", "Buyer", "hash");
        jdbcTemplate.update(
                "insert into vendor_profiles (id, user_id, business_name, verified, created_at) values (?, ?, ?, ?, now(6))",
                uuid(vendorId), uuid(ownerId), "Acme Repairs", false);
        jdbcTemplate.update(
                "insert into products (id, vendor_id, name, price, stock_quantity, active) values (?, ?, ?, 1.00, 1, true)",
                uuid(productId), uuid(vendorId), "A listing");
        jdbcTemplate.update("insert into reviews (id, product_id, reviewer_id, rating, comment) values (?, ?, ?, 5, 'Great')",
                uuid(reviewId), uuid(productId), uuid(reviewerId));

        vendorRatingService.refresh(vendorId);
        assertThat(jdbcTemplate.queryForObject(
                "select rating_count from vendor_profiles where id = ?", Integer.class, uuid(vendorId)))
                .isEqualTo(1);

        jdbcTemplate.update("delete from reviews where id = ?", uuid(reviewId));
        vendorRatingService.refresh(vendorId);

        // A stale 5.00 here would mean a seller nobody has reviewed is still shown as perfect.
        assertThat(jdbcTemplate.queryForObject(
                "select rating_avg from vendor_profiles where id = ?", BigDecimal.class, uuid(vendorId))).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "select rating_count from vendor_profiles where id = ?", Integer.class, uuid(vendorId)))
                .isZero();
    }

    /** The id column is {@code binary(16)}, so the raw bytes go in rather than the formatted UUID. */
    private static byte[] uuid(UUID id) {
        return toBytes(id);
    }

    private static byte[] toBytes(UUID id) {
        byte[] bytes = new byte[16];
        long high = id.getMostSignificantBits();
        long low = id.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (high >>> (8 * (7 - i)));
            bytes[8 + i] = (byte) (low >>> (8 * (7 - i)));
        }
        return bytes;
    }

    /**
     * Leaves no scratch database behind, so repeated runs always start from a
     * genuinely empty schema.
     */
    @AfterAll
    static void dropScratchDatabase() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/" + SCRATCH_DATABASE, mysqlUsername(), mysqlPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + SCRATCH_DATABASE);
        }
    }
}