package za.ac.cput.prm_marketplace.tools;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import za.ac.cput.prm_marketplace.domain.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the Flyway V1 baseline against a throwaway database to prove the script
 * executes cleanly from nothing. This is the path a fresh deployment takes, which
 * the pre-existing development database never exercises because it is baselined.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/prm_marketplace_flywaytest?createDatabaseIfNotExist=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false",
        "spring.flyway.clean-disabled=false"
})
class FlywayBaselineOnFreshDatabaseTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    /**
     * Leaves no scratch database behind, so repeated runs always start from a
     * genuinely empty schema.
     */
    @AfterAll
    static void dropScratchDatabase() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/prm_marketplace_flywaytest", "root", "password");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS prm_marketplace_flywaytest");
        }
    }
}
