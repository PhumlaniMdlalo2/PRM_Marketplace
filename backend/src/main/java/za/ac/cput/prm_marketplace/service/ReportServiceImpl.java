package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Report;
import za.ac.cput.prm_marketplace.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReportServiceImpl implements IReportService {

    private final ReportRepository reportRepository;

    public ReportServiceImpl(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    public Report create(Report report) {
        if (report == null) {
            return null;
        }
        return reportRepository.save(report);
    }

    @Override
    public Report read(UUID id) {
        if (id == null) {
            return null;
        }
        return reportRepository.findById(id).orElse(null);
    }

    @Override
    public Report update(Report report) {
        if (report == null || report.getId() == null
                || !reportRepository.existsById(report.getId())) {
            return null;
        }
        return reportRepository.save(report);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !reportRepository.existsById(id)) {
            return false;
        }
        reportRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Report> getAll() {
        return reportRepository.findAll();
    }

    @Override
    public Optional<Report> findByReporterId(UUID reporterId) {
        List<Report> reports = reportRepository.findByReporterId(reporterId);
        return reports.isEmpty() ? Optional.empty() : Optional.of(reports.get(0));
    }
}