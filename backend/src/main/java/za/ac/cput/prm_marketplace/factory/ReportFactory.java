package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.Report;
import za.ac.cput.prm_marketplace.domain.ReportStatus;
import za.ac.cput.prm_marketplace.domain.ReportTargetType;
import za.ac.cput.prm_marketplace.domain.User;

import java.util.UUID;

public class ReportFactory {

    public static Report createReport(User reporter, ReportTargetType targetType, String reason) {
        return createReport(reporter, targetType, null, reason, ReportStatus.OPEN);
    }

    public static Report createReport(User reporter, ReportTargetType targetType,
                                      UUID targetId, String reason) {
        return createReport(reporter, targetType, targetId, reason, ReportStatus.OPEN);
    }

    public static Report createReport(User reporter, ReportTargetType targetType, UUID targetId,
                                      String reason, ReportStatus status) {
        if (reporter == null) {
            return null;
        }
        if (targetType == null) {
            return null;
        }
        if (reason == null || reason.isBlank()) {
            return null;
        }
        if (status == null) {
            return null;
        }

        return new Report.Builder()
                .setReporter(reporter)
                .setTargetType(targetType)
                .setTargetId(targetId)
                .setReason(reason)
                .setStatus(status)
                .build();
    }

    public static Report createReport(User reporter, String targetType, String reason, String status) {
        return createReport(reporter, parseTargetType(targetType), null, reason, parseStatus(status));
    }

    public static Report createReport(User reporter, ReportTargetType targetType, String reason, String status) {
        return createReport(reporter, targetType, null, reason, parseStatus(status));
    }

    private static ReportTargetType parseTargetType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ReportTargetType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static ReportStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ReportStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}