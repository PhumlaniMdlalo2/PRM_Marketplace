package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Report;
import za.ac.cput.prm_marketplace.domain.ReportStatus;
import za.ac.cput.prm_marketplace.domain.ReportTargetType;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.ReportRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    private ReportServiceImpl service;

    private UUID reporterId;
    private UUID intruderId;
    private UUID reportId;
    private UUID targetId;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(reportRepository, userRepository);
        reporterId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        reportId = UUID.randomUUID();
        targetId = UUID.randomUUID();
    }

    @Test
    @DisplayName("filing a report attributes it to the caller and starts it OPEN")
    void create_takesReporterFromTheRequester() {
        when(userRepository.findById(reporterId)).thenReturn(Optional.of(buildUser(reporterId)));
        when(reportRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Report hostile = new Report.Builder()
                .setReporter(buildUser(intruderId))
                .setTargetType(ReportTargetType.PRODUCT)
                .setTargetId(targetId)
                .setReason("Misleading description")
                .setStatus(ReportStatus.RESOLVED)
                .setResolutionNotes("Already handled")
                .build();

        Report created = service.create(hostile, reporterId);

        assertThat(created).isNotNull();
        assertThat(created.getReporter().getId()).isEqualTo(reporterId);
        // A complaint must not arrive pre-dismissed by whoever filed it.
        assertThat(created.getStatus()).isEqualTo(ReportStatus.OPEN);
        assertThat(created.getResolutionNotes()).isNull();
        assertThat(created.getResolvedAt()).isNull();
        // A client-supplied id must not turn the insert into an overwrite.
        assertThat(created.getId()).isNull();
    }

    @Test
    @DisplayName("filing a report returns null when the caller has no user row")
    void create_unknownReporterReturnsNull() {
        when(userRepository.findById(reporterId)).thenReturn(Optional.empty());

        assertThat(service.create(buildReport(ReportStatus.OPEN), reporterId)).isNull();
        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("filing a report returns null for a null payload or reporter")
    void create_rejectsNulls() {
        assertThat(service.create(null, reporterId)).isNull();
        assertThat(service.create(buildReport(ReportStatus.OPEN), null)).isNull();
        verifyNoInteractions(reportRepository);
    }

    @Test
    @DisplayName("read returns the caller's own report")
    void read_ownedReportIsReturned() {
        Report report = buildReport(ReportStatus.OPEN);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

        assertThat(service.read(reportId, reporterId)).isSameAs(report);
    }

    @Test
    @DisplayName("read hides a report filed by somebody else")
    void read_foreignReportIsHidden() {
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(buildReport(intruderId)));

        assertThat(service.read(reportId, reporterId)).isNull();
    }

    @Test
    @DisplayName("read returns null for a missing report or a null id")
    void read_missingReturnsNull() {
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThat(service.read(reportId, reporterId)).isNull();
        assertThat(service.read(null, reporterId)).isNull();
    }

    @Test
    @DisplayName("delete withdraws the caller's own report")
    void delete_ownedReportIsRemoved() {
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(buildReport(ReportStatus.OPEN)));

        assertThat(service.delete(reportId, reporterId)).isTrue();
        verify(reportRepository).deleteById(reportId);
    }

    @Test
    @DisplayName("delete refuses to withdraw a report filed by somebody else")
    void delete_foreignReportIsRefused() {
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(buildReport(intruderId)));

        assertThat(service.delete(reportId, reporterId)).isFalse();
        verify(reportRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete returns false for a missing report")
    void delete_missingReturnsFalse() {
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThat(service.delete(reportId, reporterId)).isFalse();
        verify(reportRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("getByReporter returns only the caller's own reports")
    void getByReporter_isScopedToTheRequester() {
        Report report = buildReport(ReportStatus.OPEN);
        when(reportRepository.findByReporterId(reporterId)).thenReturn(List.of(report));

        assertThat(service.getByReporter(reporterId)).containsExactly(report);
        assertThat(service.getByReporter(null)).isEmpty();
    }

    @Test
    @DisplayName("a student cannot resolve or dismiss a report")
    void resolve_isRefusedForStudents() {
        // The role check runs before the report is even loaded, so nothing is read or written.
        assertThat(service.resolve(reportId, ReportStatus.RESOLVED, "done", reporterId, Role.STUDENT)).isNull();
        assertThat(service.resolve(reportId, ReportStatus.RESOLVED, "done", reporterId, Role.VENDOR)).isNull();
        assertThat(service.resolve(reportId, ReportStatus.RESOLVED, "done", reporterId, Role.RESIDENT)).isNull();
        assertThat(service.resolve(reportId, ReportStatus.RESOLVED, "done", reporterId, null)).isNull();

        verifyNoInteractions(reportRepository);
    }

    @Test
    @DisplayName("admin can resolve a report and the resolution is timestamped")
    void resolve_isAllowedForAdmin() {
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(buildReport(ReportStatus.OPEN)));
        when(reportRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Report resolved = service.resolve(reportId, ReportStatus.RESOLVED, "Vendor corrected the listing",
                intruderId, Role.ADMIN);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(resolved.getResolutionNotes()).isEqualTo("Vendor corrected the listing");
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(resolved.isResolved()).isTrue();
    }

    @Test
    @DisplayName("dismissing a report also counts as resolved")
    void resolve_dismissalIsTimestamped() {
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(buildReport(ReportStatus.UNDER_REVIEW)));
        when(reportRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Report dismissed = service.resolve(reportId, ReportStatus.DISMISSED, "No breach", intruderId, Role.ADMIN);

        assertThat(dismissed.isResolved()).isTrue();
        assertThat(dismissed.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("reopening a report clears the resolution timestamp")
    void resolve_reopeningClearsTheTimestamp() {
        Report alreadyResolved = buildReport(ReportStatus.RESOLVED);
        alreadyResolved.resolve(ReportStatus.RESOLVED, "done");
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(alreadyResolved));
        when(reportRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Report reopened = service.resolve(reportId, ReportStatus.UNDER_REVIEW, null, intruderId, Role.ADMIN);

        assertThat(reopened.getResolvedAt()).isNull();
        assertThat(reopened.isResolved()).isFalse();
    }

    @Test
    @DisplayName("resolve returns null for a missing report or a null status")
    void resolve_invalidInputReturnsNull() {
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(buildReport(ReportStatus.OPEN)));

        // A null target status is rejected outright.
        assertThat(service.resolve(reportId, null, "done", intruderId, Role.ADMIN)).isNull();

        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());
        assertThat(service.resolve(reportId, ReportStatus.RESOLVED, "done", intruderId, Role.ADMIN)).isNull();

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("the moderation listing is empty for anyone who is not admin")
    void moderationListings_areAdminOnly() {
        assertThat(service.getAll(Role.STUDENT)).isEmpty();
        assertThat(service.getAll(Role.VENDOR)).isEmpty();
        assertThat(service.getAll(Role.RESIDENT)).isEmpty();
        assertThat(service.getAll(null)).isEmpty();
        assertThat(service.getByStatus(ReportStatus.OPEN, Role.STUDENT)).isEmpty();

        verifyNoInteractions(reportRepository);
    }

    @Test
    @DisplayName("admin can list every report and filter by status")
    void moderationListings_areAvailableToAdmin() {
        Report report = buildReport(ReportStatus.OPEN);
        when(reportRepository.findAll()).thenReturn(List.of(report));
        when(reportRepository.findByStatus(ReportStatus.OPEN)).thenReturn(List.of(report));

        assertThat(service.getAll(Role.ADMIN)).containsExactly(report);
        assertThat(service.getByStatus(ReportStatus.OPEN, Role.ADMIN)).containsExactly(report);
        assertThat(service.getByStatus(null, Role.ADMIN)).isEmpty();
    }

    private User buildUser(UUID id) {
        return new User.Builder().setId(id).setEmail(id + "@example.com").build();
    }

    private Report buildReport(ReportStatus status) {
        return buildReport(reporterId, status);
    }

    private Report buildReport(UUID reporter) {
        return buildReport(reporter, ReportStatus.OPEN);
    }

    private Report buildReport(UUID reporter, ReportStatus status) {
        return new Report.Builder()
                .setId(reportId)
                .setReporter(buildUser(reporter))
                .setTargetType(ReportTargetType.PRODUCT)
                .setTargetId(targetId)
                .setReason("Misleading description")
                .setStatus(status)
                .build();
    }
}