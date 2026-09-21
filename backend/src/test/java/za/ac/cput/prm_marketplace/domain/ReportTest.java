package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportTest {

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .build();
    }

    @Test
    void builderSetsAllFields() {
        UUID id = UUID.randomUUID();
        User reporter = buildUser();

        Report report = new Report.Builder()
                .setId(id)
                .setReporter(reporter)
                .setTargetType("USER")
                .setReason("Harassment")
                .setStatus("OPEN")
                .build();

        assertThat(report.getId()).isEqualTo(id);
        assertThat(report.getReporter()).isEqualTo(reporter);
        assertThat(report.getTargetType()).isEqualTo("USER");
        assertThat(report.getReason()).isEqualTo("Harassment");
        assertThat(report.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void copyPreservesAllFields() {
        Report original = new Report.Builder()
                .setId(UUID.randomUUID())
                .setReporter(buildUser())
                .setTargetType("PRODUCT")
                .setReason("Counterfeit")
                .setStatus("REVIEWING")
                .build();

        Report copy = new Report.Builder().copy(original).build();

        assertThat(copy.getId()).isEqualTo(original.getId());
        assertThat(copy.getReporter()).isEqualTo(original.getReporter());
        assertThat(copy.getTargetType()).isEqualTo(original.getTargetType());
        assertThat(copy.getReason()).isEqualTo(original.getReason());
        assertThat(copy.getStatus()).isEqualTo(original.getStatus());
    }

    @Test
    void copyAllowsOverridingIndividualFields() {
        Report original = new Report.Builder()
                .setId(UUID.randomUUID())
                .setReporter(buildUser())
                .setTargetType("PRODUCT")
                .setReason("Counterfeit")
                .setStatus("OPEN")
                .build();

        Report updated = new Report.Builder().copy(original).setStatus("RESOLVED").build();

        assertThat(updated.getId()).isEqualTo(original.getId());
        assertThat(updated.getReporter()).isEqualTo(original.getReporter());
        assertThat(updated.getStatus()).isEqualTo("RESOLVED");
    }
}