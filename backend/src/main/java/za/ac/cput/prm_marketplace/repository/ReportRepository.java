package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.Report;
import za.ac.cput.prm_marketplace.domain.ReportStatus;
import za.ac.cput.prm_marketplace.domain.ReportTargetType;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findByReporterId(UUID reporterId);

    boolean existsByReporterId(UUID reporterId);

    List<Report> findByStatus(ReportStatus status);

    List<Report> findByTargetTypeAndTargetId(ReportTargetType targetType, UUID targetId);

    long countByStatus(ReportStatus status);
}