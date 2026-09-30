package za.ac.cput.prm_marketplace.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import za.ac.cput.prm_marketplace.domain.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Development helper. Booting the context with Hibernate's script generation enabled
 * writes the CREATE TABLE statements the entity model implies to
 * target/generated-ddl/schema.sql, which is the source for the Flyway V1 baseline.
 * The assertions only confirm the persistence model is loadable.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
        "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/generated-ddl/schema.sql"
})
class SchemaDumpTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("the persistence model loads and can be queried")
    void modelLoads() {
        assertThat(entityManagerFactory.isOpen()).isTrue();

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            assertThat(em.getMetamodel().getEntities()).isNotEmpty();
            assertThat(em.getMetamodel().entity(User.class)).isNotNull();
        } finally {
            em.close();
        }
    }
}
