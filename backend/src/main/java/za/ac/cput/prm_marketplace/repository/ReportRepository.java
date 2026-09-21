package za.ac.cput.prm_marketplace.repository;

import za.ac.cput.prm_marketplace.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findByReporterId(UUID reporterId);

    boolean existsByReporterId(UUID reporterId);
}