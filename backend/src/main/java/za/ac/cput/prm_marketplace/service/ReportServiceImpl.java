package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Report;
import za.ac.cput.prm_marketplace.domain.ReportStatus;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.ReportRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReportServiceImpl implements IReportService {

    /**
     * Resolving a report is a moderation action, so it is limited to faculty. Students, vendors and
     * residents must not be able to dismiss a complaint or mark it resolved.
     */
    private static boolean mayResolve(Role role) {
        return role == Role.FACULTY;
    }

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    public ReportServiceImpl(ReportRepository reportRepository, UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Report create(Report report, UUID reporterId) {
        if (report == null || reporterId == null) {
            return null;
        }
        User reporter = userRepository.findById(reporterId).orElse(null);
        if (reporter == null) {
            return null;
        }
        // The reporter comes from the token and a new report always starts OPEN, so a body cannot
        // file a complaint against someone else or pre-mark it resolved.
        Report created = new Report.Builder()
                .setReporter(reporter)
                .setTargetType(report.getTargetType())
                .setTargetId(report.getTargetId())
                .setReason(report.getReason())
                .setStatus(ReportStatus.OPEN)
                .build();
        return reportRepository.save(created);
    }

    @Override
    public Report read(UUID id, UUID requesterId) {
        Report report = find(id);
        return isReporter(report, requesterId) ? report : null;
    }

    @Override
    @Transactional
    public boolean delete(UUID id, UUID requesterId) {
        Report report = find(id);
        if (report == null || !isReporter(report, requesterId)) {
            return false;
        }
        reportRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Report> getByReporter(UUID reporterId) {
        if (reporterId == null) {
            return List.of();
        }
        return reportRepository.findByReporterId(reporterId);
    }

    @Override
    @Transactional
    public Report resolve(UUID id, ReportStatus status, String resolutionNotes,
                          UUID requesterId, Role requesterRole) {
        if (!mayResolve(requesterRole)) {
            return null;
        }
        Report existing = find(id);
        if (existing == null || status == null) {
            return null;
        }
        Report updated = new Report.Builder()
                .copy(existing)
                .setStatus(status)
                .setResolutionNotes(resolutionNotes)
                .setResolvedAt(resolved(status) ? LocalDateTime.now() : null)
                .build();
        return reportRepository.save(updated);
    }

    @Override
    public List<Report> getAll(Role requesterRole) {
        if (!mayResolve(requesterRole)) {
            return List.of();
        }
        return reportRepository.findAll();
    }

    @Override
    public List<Report> getByStatus(ReportStatus status, Role requesterRole) {
        if (!mayResolve(requesterRole) || status == null) {
            return List.of();
        }
        return reportRepository.findByStatus(status);
    }

    private boolean resolved(ReportStatus status) {
        return status == ReportStatus.RESOLVED || status == ReportStatus.DISMISSED;
    }

    private Report find(UUID id) {
        if (id == null) {
            return null;
        }
        return reportRepository.findById(id).orElse(null);
    }

    private boolean isReporter(Report report, UUID requesterId) {
        return report != null
                && report.getReporter() != null
                && report.getReporter().getId() != null
                && report.getReporter().getId().equals(requesterId);
    }
}