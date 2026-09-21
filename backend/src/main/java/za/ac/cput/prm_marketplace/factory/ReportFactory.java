package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.Report;
import za.ac.cput.prm_marketplace.domain.User;

public class ReportFactory {

    public static Report createReport(User reporter, String targetType, String reason, String status) {
        if (reporter == null) {
            return null;
        }
        if (targetType == null || targetType.isBlank()) {
            return null;
        }
        if (reason == null || reason.isBlank()) {
            return null;
        }
        if (status == null || status.isBlank()) {
            return null;
        }

        return new Report.Builder()
                .setReporter(reporter)
                .setTargetType(targetType)
                .setReason(reason)
                .setStatus(status)
                .build();
    }
}