package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Report;
import za.ac.cput.prm_marketplace.domain.ReportStatus;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IReportService;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Mounted under "/api" to match the rest of the application; the old "/reports" mapping sat outside
 * the group the security rules are written against.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final IReportService reportService;

    public ReportController(IReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Files a report as the caller. The reporter and status used to come from the body, so a
     * complaint could be filed in another person's name and pre-marked as resolved.
     */
    @PostMapping
    public ResponseEntity<Report> create(@RequestBody Report report, Authentication authentication) {
        Report created = reportService.create(report, CurrentCaller.id(authentication));
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /** The caller's own reports. The former "GET /reports" returned every report to every caller. */
    @GetMapping
    public ResponseEntity<List<Report>> getAll(Authentication authentication) {
        return ResponseEntity.ok(reportService.getByReporter(CurrentCaller.id(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Report> read(@PathVariable UUID id, Authentication authentication) {
        Report report = reportService.read(id, CurrentCaller.id(authentication));
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(report);
    }

    /** Withdraws one of the caller's own reports. */
    @DeleteMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Nothing to return: the change was applied and there is no state left to read.")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        if (!reportService.delete(id, CurrentCaller.id(authentication))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Moderator view of every report. Admin only; for anyone else the service returns an empty
     * list rather than the reports.
     */
    @GetMapping("/moderation/all")
    public ResponseEntity<List<Report>> getAllForModeration(Authentication authentication) {
        return ResponseEntity.ok(reportService.getAll(CurrentCaller.role(authentication)));
    }

    @GetMapping("/moderation/status/{status}")
    public ResponseEntity<List<Report>> getByStatus(@PathVariable ReportStatus status,
                                                    Authentication authentication) {
        return ResponseEntity.ok(reportService.getByStatus(status, CurrentCaller.role(authentication)));
    }

    /** Moderator decision on a report. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Report> resolve(@PathVariable UUID id,
                                          @RequestParam ReportStatus status,
                                          @RequestParam(required = false) String notes,
                                          Authentication authentication) {
        Report updated = reportService.resolve(id, status, notes,
                CurrentCaller.id(authentication), CurrentCaller.role(authentication));
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }
}
