package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Notification;
import za.ac.cput.prm_marketplace.domain.NotificationType;
import za.ac.cput.prm_marketplace.service.INotificationService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.asStudent;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private INotificationService notificationService;

    private UUID callerId;
    private UUID intruderId;
    private UUID notificationId;
    private Notification notification;

    @BeforeEach
    void setUp() {
        callerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        notification = new Notification.Builder()
                .setId(notificationId)
                .setUserId(callerId)
                .setType(NotificationType.SYSTEM)
                .setTitle("Welcome")
                .setMessage("Your account is ready")
                .build();
    }

    @Test
    @DisplayName("the inbox is the caller's, and the /user/{userId} route is gone")
    void getAll_isScopedToTheCaller() throws Exception {
        when(notificationService.getByUserId(callerId)).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications").with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(notificationId.toString()));

        verify(notificationService).getByUserId(callerId);

        mockMvc.perform(get("/api/notifications/user/" + intruderId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the old unprefixed route is gone")
    void legacyUnprefixedRouteIsGone() throws Exception {
        mockMvc.perform(get("/notifications").with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("unread notifications are the caller's")
    void getUnread_isScopedToTheCaller() throws Exception {
        when(notificationService.getUnreadByUserId(callerId)).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications/unread").with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(notificationId.toString()));

        verify(notificationService).getUnreadByUserId(callerId);
    }

    @Test
    @DisplayName("the unread count is the caller's")
    void countUnread_isScopedToTheCaller() throws Exception {
        when(notificationService.countUnread(callerId)).thenReturn(7L);

        mockMvc.perform(get("/api/notifications/unread/count").with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(7));
    }

    @Test
    @DisplayName("reading a notification addressed to someone else returns 404")
    void read_returnsNotFoundForAnotherAccountsNotification() throws Exception {
        when(notificationService.read(notificationId, callerId)).thenReturn(null);

        mockMvc.perform(get("/api/notifications/" + notificationId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("marking someone else's notification as read returns 404 and changes nothing")
    void markAsRead_returnsNotFoundForAnotherAccountsNotification() throws Exception {
        when(notificationService.markAsRead(notificationId, callerId)).thenReturn(null);

        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read")
                        .with(asStudent(callerId)))
                .andExpect(status().isNotFound());

        verify(notificationService).markAsRead(notificationId, callerId);
    }

    @Test
    @DisplayName("deleting a notification addressed to someone else returns 404")
    void delete_returnsNotFoundForAnotherAccountsNotification() throws Exception {
        when(notificationService.delete(notificationId, callerId)).thenReturn(false);

        mockMvc.perform(delete("/api/notifications/" + notificationId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("mark-all-as-read only clears the caller's own notifications")
    void markAllAsRead_scopesToTheCaller() throws Exception {
        when(notificationService.markAllAsRead(callerId)).thenReturn(3);

        mockMvc.perform(patch("/api/notifications/read-all").with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));

        verify(notificationService).markAllAsRead(callerId);

        mockMvc.perform(patch("/api/notifications/user/" + intruderId + "/read-all")
                        .with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("notifications cannot be created or edited over HTTP")
    void createAndUpdateAreNotExposed() throws Exception {
        String body = objectMapper.writeValueAsString(notification);

        mockMvc.perform(post("/api/notifications")
                        .with(asStudent(callerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/notifications")
                        .with(asStudent(callerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("a body cannot forge the read flag or the recipient")
    void readOnlyFieldsAreIgnored() throws Exception {
        when(notificationService.read(notificationId, callerId)).thenReturn(notification);

        mockMvc.perform(put("/api/notifications/" + notificationId)
                        .with(asStudent(callerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + intruderId + "\",\"read\":true}"))
                .andExpect(status().isMethodNotAllowed());

        verify(notificationService, never()).read(eq(notificationId), any());
    }

    @Test
    @DisplayName("notification endpoints reject anonymous callers")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/notifications/" + notificationId)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read"))
                .andExpect(status().isUnauthorized());
        verify(notificationService, never()).getByUserId(any());
    }
}