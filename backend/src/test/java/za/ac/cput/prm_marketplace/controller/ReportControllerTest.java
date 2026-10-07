package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Report;
import za.ac.cput.prm_marketplace.domain.ReportStatus;
import za.ac.cput.prm_marketplace.domain.ReportTargetType;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.IReportService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.as;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.asStudent;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IReportService reportService;

    private UUID reporterId;
    private UUID intruderId;
    private UUID reportId;
    private UUID targetId;
    private Report report;

    @BeforeEach
    void setUp() {
        reporterId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        reportId = UUID.randomUUID();
        targetId = UUID.randomUUID();
        report = new Report.Builder()
                .setId(reportId)
                .setReporter(buildUser(reporterId))
                .setTargetType(ReportTargetType.PRODUCT)
                .setTargetId(targetId)
                .setReason("Misleading description")
                .setStatus(ReportStatus.OPEN)
                .build();
    }

    @Test
    @DisplayName("filing a report attributes it to the caller and starts it OPEN")
    void create_takesTheReporterFromTheToken() throws Exception {
        when(reportService.create(any(), eq(reporterId))).thenReturn(report);

        // The body files the complaint against another account and pre-dismisses it.
        Report hostile = new Report.Builder()
                .copy(report)
                .setReporter(buildUser(intruderId))
                .setStatus(ReportStatus.DISMISSED)
                .build();

        mockMvc.perform(post("/api/reports")
                        .with(asStudent(reporterId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hostile)))
                .andExpect(status().isCreated());

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportService).create(captor.capture(), eq(reporterId));
        assertThat(captor.getValue().getTargetId()).isEqualTo(targetId);
    }

    @Test
    @DisplayName("filing a report returns 400 when it cannot be saved")
    void create_returnsBadRequestWhenServiceReturnsNull() throws Exception {
        when(reportService.create(any(), eq(reporterId))).thenReturn(null);

        mockMvc.perform(post("/api/reports")
                        .with(asStudent(reporterId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(report)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("the report list is the caller's own, and it moved under /api")
    void getAll_isScopedToTheCaller() throws Exception {
        when(reportService.getByReporter(reporterId)).thenReturn(List.of(report));

        mockMvc.perform(get("/api/reports").with(asStudent(reporterId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reportId.toString()));

        verify(reportService).getByReporter(reporterId);

        mockMvc.perform(get("/reports").with(asStudent(reporterId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("reading a report filed by somebody else returns 404")
    void read_returnsNotFoundForAnotherAccountsReport() throws Exception {
        when(reportService.read(reportId, reporterId)).thenReturn(null);

        mockMvc.perform(get("/api/reports/" + reportId).with(asStudent(reporterId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("reading the caller's own report succeeds")
    void read_returnsTheReport() throws Exception {
        when(reportService.read(reportId, reporterId)).thenReturn(report);

        mockMvc.perform(get("/api/reports/" + reportId).with(asStudent(reporterId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId.toString()));
    }

    @Test
    @DisplayName("withdrawing a report filed by somebody else returns 404")
    void delete_returnsNotFoundForAnotherAccountsReport() throws Exception {
        when(reportService.delete(reportId, reporterId)).thenReturn(false);

        mockMvc.perform(delete("/api/reports/" + reportId).with(asStudent(reporterId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("withdrawing the caller's own report succeeds")
    void delete_returnsNoContent() throws Exception {
        when(reportService.delete(reportId, reporterId)).thenReturn(true);

        mockMvc.perform(delete("/api/reports/" + reportId).with(asStudent(reporterId)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("the generic update that let anyone rewrite a report is not reachable")
    void update_bodyEndpointIsGone() throws Exception {
        // The path is bound for filing a report, so a bare PUT is answered 405 rather than 404.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/reports")
                        .with(asStudent(reporterId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(report)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("a student asking for the moderation view gets nothing")
    void moderationView_isEmptyForStudents() throws Exception {
        when(reportService.getAll(Role.STUDENT)).thenReturn(List.of());

        mockMvc.perform(get("/api/reports/moderation/all").with(asStudent(reporterId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(reportService).getAll(Role.STUDENT);
    }

    @Test
    @DisplayName("admin can list every report and filter by status")
    void moderationView_isAvailableToAdmin() throws Exception {
        when(reportService.getAll(Role.ADMIN)).thenReturn(List.of(report));
        when(reportService.getByStatus(ReportStatus.OPEN, Role.ADMIN)).thenReturn(List.of(report));

        mockMvc.perform(get("/api/reports/moderation/all").with(as(reporterId, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reportId.toString()));

        mockMvc.perform(get("/api/reports/moderation/status/OPEN").with(as(reporterId, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reportId.toString()));
    }

    @Test
    @DisplayName("a student cannot resolve a report")
    void resolve_isRefusedForStudents() throws Exception {
        when(reportService.resolve(any(), any(), any(), any(), eq(Role.STUDENT))).thenReturn(null);

        mockMvc.perform(patch("/api/reports/" + reportId + "/status")
                        .with(asStudent(reporterId))
                        .param("status", "RESOLVED"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("admin can resolve a report")
    void resolve_isAllowedForAdmin() throws Exception {
        when(reportService.resolve(reportId, ReportStatus.RESOLVED, "Vendor corrected the listing",
                reporterId, Role.ADMIN)).thenReturn(report);

        mockMvc.perform(patch("/api/reports/" + reportId + "/status")
                        .with(as(reporterId, Role.ADMIN))
                        .param("status", "RESOLVED")
                        .param("notes", "Vendor corrected the listing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId.toString()));
    }

    @Test
    @DisplayName("report endpoints reject anonymous callers")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/reports")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reports/" + reportId)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/reports/" + reportId)).andExpect(status().isUnauthorized());

        verify(reportService, never()).delete(any(), any());
    }

    private User buildUser(UUID id) {
        return new User.Builder().setId(id).setEmail(id + "@example.com").build();
    }
}