package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.Report;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReportFactoryTest {

    private User buildUser() {
        return UserFactory.createUser("Jane Doe", "jane@example.com", "hashed-password",
                Role.STUDENT, null, false, null);
    }

    @Test
    void createsReportWithValidFields() {
        User reporter = buildUser();

        Report report = ReportFactory.createReport(reporter, "PRODUCT", "Counterfeit", "OPEN");

        assertThat(report).isNotNull();
        assertThat(report.getReporter()).isEqualTo(reporter);
        assertThat(report.getTargetType()).isEqualTo("PRODUCT");
        assertThat(report.getReason()).isEqualTo("Counterfeit");
        assertThat(report.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void returnsNullWhenReporterIsNull() {
        Report report = ReportFactory.createReport(null, "PRODUCT", "Counterfeit", "OPEN");

        assertThat(report).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void returnsNullWhenTargetTypeIsBlankOrNull(String targetType) {
        Report report = ReportFactory.createReport(buildUser(), targetType, "Counterfeit", "OPEN");

        assertThat(report).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void returnsNullWhenReasonIsBlankOrNull(String reason) {
        Report report = ReportFactory.createReport(buildUser(), "PRODUCT", reason, "OPEN");

        assertThat(report).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void returnsNullWhenStatusIsBlankOrNull(String status) {
        Report report = ReportFactory.createReport(buildUser(), "PRODUCT", "Counterfeit", status);

        assertThat(report).isNull();
    }
}