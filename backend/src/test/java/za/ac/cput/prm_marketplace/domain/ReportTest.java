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
        UUID targetId = UUID.randomUUID();
        User reporter = buildUser();

        Report report = new Report.Builder()
                .setId(id)
                .setReporter(reporter)
                .setTargetType(ReportTargetType.USER)
                .setTargetId(targetId)
                .setReason("Harassment")
                .setStatus(ReportStatus.OPEN)
                .build();

        assertThat(report.getId()).isEqualTo(id);
        assertThat(report.getReporter()).isEqualTo(reporter);
        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.USER);
        assertThat(report.getTargetId()).isEqualTo(targetId);
        assertThat(report.getReason()).isEqualTo("Harassment");
        assertThat(report.getStatus()).isEqualTo(ReportStatus.OPEN);
    }

    @Test
    void defaultsStatusToOpen() {
        Report report = new Report.Builder()
                .setReporter(buildUser())
                .setTargetType(ReportTargetType.PRODUCT)
                .setReason("Counterfeit")
                .build();

        assertThat(report.getStatus()).isEqualTo(ReportStatus.OPEN);
    }

    @Test
    void copyPreservesAllFields() {
        Report original = new Report.Builder()
                .setId(UUID.randomUUID())
                .setReporter(buildUser())
                .setTargetType(ReportTargetType.PRODUCT)
                .setTargetId(UUID.randomUUID())
                .setReason("Counterfeit")
                .setStatus(ReportStatus.UNDER_REVIEW)
                .build();

        Report copy = new Report.Builder().copy(original).build();

        assertThat(copy.getId()).isEqualTo(original.getId());
        assertThat(copy.getReporter()).isEqualTo(original.getReporter());
        assertThat(copy.getTargetType()).isEqualTo(original.getTargetType());
        assertThat(copy.getTargetId()).isEqualTo(original.getTargetId());
        assertThat(copy.getReason()).isEqualTo(original.getReason());
        assertThat(copy.getStatus()).isEqualTo(original.getStatus());
    }

    @Test
    void copyAllowsOverridingIndividualFields() {
        Report original = new Report.Builder()
                .setId(UUID.randomUUID())
                .setReporter(buildUser())
                .setTargetType(ReportTargetType.PRODUCT)
                .setReason("Counterfeit")
                .setStatus(ReportStatus.OPEN)
                .build();

        Report updated = new Report.Builder().copy(original).setStatus(ReportStatus.RESOLVED).build();

        assertThat(updated.getId()).isEqualTo(original.getId());
        assertThat(updated.getReporter()).isEqualTo(original.getReporter());
        assertThat(updated.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    void resolveMarksReportResolved() {
        Report report = new Report.Builder()
                .setReporter(buildUser())
                .setTargetType(ReportTargetType.PRODUCT)
                .setReason("Counterfeit")
                .build();

        assertThat(report.isResolved()).isFalse();

        report.resolve(ReportStatus.RESOLVED, "Item removed");

        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(report.getResolutionNotes()).isEqualTo("Item removed");
        assertThat(report.getResolvedAt()).isNotNull();
        assertThat(report.isResolved()).isTrue();
    }

    @Test
    void dismissedReportIsAlsoResolved() {
        Report report = new Report.Builder()
                .setReporter(buildUser())
                .setTargetType(ReportTargetType.PRODUCT)
                .setReason("Counterfeit")
                .setStatus(ReportStatus.DISMISSED)
                .build();

        assertThat(report.isResolved()).isTrue();
    }
}