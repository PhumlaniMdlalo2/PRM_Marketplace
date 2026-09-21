package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Report;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IReportService {

    Report create(Report report);

    Report read(UUID id);

    Report update(Report report);

    boolean delete(UUID id);

    List<Report> getAll();

    Optional<Report> findByReporterId(UUID reporterId);
}