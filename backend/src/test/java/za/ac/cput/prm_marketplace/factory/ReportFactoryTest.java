package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.Report;
import za.ac.cput.prm_marketplace.domain.ReportStatus;
import za.ac.cput.prm_marketplace.domain.ReportTargetType;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportFactoryTest {

    private User buildUser() {
        return UserFactory.createUser("Jane Doe", "jane@example.com", "hashed-password",
                Role.STUDENT, null, false, null);
    }

    @Test
    void createsReportWithValidFields() {
        User reporter = buildUser();

        Report report = ReportFactory.createReport(reporter, ReportTargetType.PRODUCT, "Counterfeit");

        assertThat(report).isNotNull();
        assertThat(report.getReporter()).isEqualTo(reporter);
        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.PRODUCT);
        assertThat(report.getReason()).isEqualTo("Counterfeit");
        assertThat(report.getStatus()).isEqualTo(ReportStatus.OPEN);
    }

    @Test
    void createsReportWithTargetIdAndStatus() {
        UUID targetId = UUID.randomUUID();

        Report report = ReportFactory.createReport(
                buildUser(), ReportTargetType.USER, targetId, "Harassment", ReportStatus.UNDER_REVIEW);

        assertThat(report).isNotNull();
        assertThat(report.getTargetId()).isEqualTo(targetId);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.UNDER_REVIEW);
    }

    @Test
    void defaultsNullStatusToOpen() {
        Report report = ReportFactory.createReport(buildUser(), ReportTargetType.COMMENT, "Spam");

        assertThat(report.getStatus()).isEqualTo(ReportStatus.OPEN);
    }

    @Test
    void returnsNullWhenReporterIsNull() {
        Report report = ReportFactory.createReport(null, ReportTargetType.PRODUCT, "Counterfeit");

        assertThat(report).isNull();
    }

    @Test
    void returnsNullWhenTargetTypeIsNull() {
        Report report = ReportFactory.createReport(buildUser(), (ReportTargetType) null, "Counterfeit");

        assertThat(report).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void returnsNullWhenReasonIsBlankOrNull(String reason) {
        Report report = ReportFactory.createReport(buildUser(), ReportTargetType.PRODUCT, reason);

        assertThat(report).isNull();
    }

    @Test
    void parsesStringTargetTypeAndStatus() {
        Report report = ReportFactory.createReport(buildUser(), "PRODUCT", "Counterfeit", "OPEN");

        assertThat(report).isNotNull();
        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.PRODUCT);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.OPEN);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void returnsNullWhenStringTargetTypeIsBlankOrNull(String targetType) {
        Report report = ReportFactory.createReport(buildUser(), targetType, "Counterfeit", "OPEN");

        assertThat(report).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void returnsNullWhenStringStatusIsBlankOrNull(String status) {
        Report report = ReportFactory.createReport(buildUser(), "PRODUCT", "Counterfeit", status);

        assertThat(report).isNull();
    }

    @Test
    void returnsNullWhenStringTargetTypeIsNotAValidEnumValue() {
        Report report = ReportFactory.createReport(buildUser(), "NOT_A_TYPE", "Counterfeit", "OPEN");

        assertThat(report).isNull();
    }
}