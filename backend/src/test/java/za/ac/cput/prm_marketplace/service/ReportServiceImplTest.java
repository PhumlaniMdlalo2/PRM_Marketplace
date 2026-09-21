package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Report;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .build();
    }

    private Report buildReport(UUID id) {
        return new Report.Builder()
                .setId(id)
                .setReporter(buildUser())
                .setTargetType("PRODUCT")
                .setReason("Counterfeit")
                .setStatus("OPEN")
                .build();
    }

    @Test
    void createReturnsNullWhenReportIsNull() {
        Report result = reportService.create(null);

        assertThat(result).isNull();
        verifyNoInteractions(reportRepository);
    }

    @Test
    void createSavesAndReturnsReport() {
        Report report = buildReport(null);
        Report saved = buildReport(UUID.randomUUID());
        when(reportRepository.save(report)).thenReturn(saved);

        Report result = reportService.create(report);

        assertThat(result).isEqualTo(saved);
        verify(reportRepository).save(report);
    }

    @Test
    void readReturnsNullWhenIdIsNull() {
        Report result = reportService.read(null);

        assertThat(result).isNull();
        verifyNoInteractions(reportRepository);
    }

    @Test
    void readReturnsReportWhenFound() {
        UUID id = UUID.randomUUID();
        Report report = buildReport(id);
        when(reportRepository.findById(id)).thenReturn(Optional.of(report));

        Report result = reportService.read(id);

        assertThat(result).isEqualTo(report);
    }

    @Test
    void readReturnsNullWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(reportRepository.findById(id)).thenReturn(Optional.empty());

        Report result = reportService.read(id);

        assertThat(result).isNull();
    }

    @Test
    void updateReturnsNullWhenReportIsNull() {
        Report result = reportService.update(null);

        assertThat(result).isNull();
        verifyNoInteractions(reportRepository);
    }

    @Test
    void updateReturnsNullWhenIdIsNull() {
        Report report = buildReport(null);

        Report result = reportService.update(report);

        assertThat(result).isNull();
        verifyNoInteractions(reportRepository);
    }

    @Test
    void updateReturnsNullWhenReportDoesNotExist() {
        UUID id = UUID.randomUUID();
        Report report = buildReport(id);
        when(reportRepository.existsById(id)).thenReturn(false);

        Report result = reportService.update(report);

        assertThat(result).isNull();
        verify(reportRepository, never()).save(any());
    }

    @Test
    void updateSavesAndReturnsReportWhenExists() {
        UUID id = UUID.randomUUID();
        Report report = buildReport(id);
        when(reportRepository.existsById(id)).thenReturn(true);
        when(reportRepository.save(report)).thenReturn(report);

        Report result = reportService.update(report);

        assertThat(result).isEqualTo(report);
        verify(reportRepository).save(report);
    }

    @Test
    void deleteReturnsFalseWhenIdIsNull() {
        boolean result = reportService.delete(null);

        assertThat(result).isFalse();
        verifyNoInteractions(reportRepository);
    }

    @Test
    void deleteReturnsFalseWhenReportDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(reportRepository.existsById(id)).thenReturn(false);

        boolean result = reportService.delete(id);

        assertThat(result).isFalse();
        verify(reportRepository, never()).deleteById(any());
    }

    @Test
    void deleteReturnsTrueAndDeletesWhenExists() {
        UUID id = UUID.randomUUID();
        when(reportRepository.existsById(id)).thenReturn(true);

        boolean result = reportService.delete(id);

        assertThat(result).isTrue();
        verify(reportRepository).deleteById(id);
    }

    @Test
    void getAllReturnsAllReports() {
        List<Report> reports = List.of(buildReport(UUID.randomUUID()), buildReport(UUID.randomUUID()));
        when(reportRepository.findAll()).thenReturn(reports);

        List<Report> result = reportService.getAll();

        assertThat(result).isEqualTo(reports);
    }

    @Test
    void findByReporterIdReturnsFirstReportWhenFound() {
        UUID reporterId = UUID.randomUUID();
        Report report = buildReport(UUID.randomUUID());
        when(reportRepository.findByReporterId(reporterId)).thenReturn(List.of(report));

        Optional<Report> result = reportService.findByReporterId(reporterId);

        assertThat(result).contains(report);
    }

    @Test
    void findByReporterIdReturnsEmptyWhenNoneFound() {
        UUID reporterId = UUID.randomUUID();
        when(reportRepository.findByReporterId(reporterId)).thenReturn(List.of());

        Optional<Report> result = reportService.findByReporterId(reporterId);

        assertThat(result).isEmpty();
    }
}