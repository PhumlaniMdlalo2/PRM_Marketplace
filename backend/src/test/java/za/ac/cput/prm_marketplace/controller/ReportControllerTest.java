package za.ac.cput.prm_marketplace.controller;

import za.ac.cput.prm_marketplace.domain.Report;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.IReportService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IReportService reportService;

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
    void createReturnsCreatedWhenServiceSucceeds() throws Exception {
        Report report = buildReport(null);
        Report saved = buildReport(UUID.randomUUID());
        when(reportService.create(any(Report.class))).thenReturn(saved);

        mockMvc.perform(post("/reports")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(report)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason").value("Counterfeit"));
    }

    @Test
    void createReturnsBadRequestWhenServiceRejects() throws Exception {
        Report report = buildReport(null);
        when(reportService.create(any(Report.class))).thenReturn(null);

        mockMvc.perform(post("/reports")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(report)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readReturnsReportWhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(reportService.read(id)).thenReturn(buildReport(id));

        mockMvc.perform(get("/reports/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Counterfeit"));
    }

    @Test
    void readReturnsNotFoundWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(reportService.read(id)).thenReturn(null);

        mockMvc.perform(get("/reports/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturnsOkWhenServiceSucceeds() throws Exception {
        Report report = buildReport(UUID.randomUUID());
        when(reportService.update(any(Report.class))).thenReturn(report);

        mockMvc.perform(put("/reports")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(report)))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsNotFoundWhenServiceRejects() throws Exception {
        Report report = buildReport(UUID.randomUUID());
        when(reportService.update(any(Report.class))).thenReturn(null);

        mockMvc.perform(put("/reports")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(report)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturnsNoContentWhenDeleted() throws Exception {
        UUID id = UUID.randomUUID();
        when(reportService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/reports/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturnsNotFoundWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(reportService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/reports/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllReturnsListOfReports() throws Exception {
        when(reportService.getAll()).thenReturn(List.of(buildReport(UUID.randomUUID())));

        mockMvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reason").value("Counterfeit"));
    }

    @Test
    void findByReporterIdReturnsReportWhenFound() throws Exception {
        UUID reporterId = UUID.randomUUID();
        Report report = buildReport(UUID.randomUUID());
        when(reportService.findByReporterId(reporterId)).thenReturn(Optional.of(report));

        mockMvc.perform(get("/reports/reporter/{reporterId}", reporterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Counterfeit"));
    }

    @Test
    void findByReporterIdReturnsNotFoundWhenMissing() throws Exception {
        UUID reporterId = UUID.randomUUID();
        when(reportService.findByReporterId(reporterId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/reports/reporter/{reporterId}", reporterId))
                .andExpect(status().isNotFound());
    }
}