package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Report;
import za.ac.cput.prm_marketplace.domain.ReportStatus;
import za.ac.cput.prm_marketplace.domain.Role;

import java.util.List;
import java.util.UUID;

/**
 * A report is filed by the caller, who may then read and withdraw only their own. Resolving a
 * report is a moderator action and takes the caller's role explicitly, so the two authorisation
 * rules stay separate.
 */
public interface IReportService {

    /** Files a report on behalf of the caller. The reporter and status come from the token. */
    Report create(Report report, UUID reporterId);

    /** @return the report, or null when it does not exist or the caller did not file it */
    Report read(UUID id, UUID requesterId);

    /** @return false when it does not exist or the caller did not file it */
    boolean delete(UUID id, UUID requesterId);

    List<Report> getByReporter(UUID reporterId);

    /**
     * Moves a report to a new status. Moderators only.
     *
     * @return the updated report, or null when the report is missing or the caller's role may not
     *         resolve reports
     */
    Report resolve(UUID id, ReportStatus status, String resolutionNotes,
                   UUID requesterId, Role requesterRole);

    /** Every report, for moderators. */
    List<Report> getAll(Role requesterRole);

    /** Reports in a given state, for moderators. */
    List<Report> getByStatus(ReportStatus status, Role requesterRole);
}