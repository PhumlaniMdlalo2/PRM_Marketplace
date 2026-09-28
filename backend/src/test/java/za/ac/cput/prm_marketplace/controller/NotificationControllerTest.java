package za.ac.cput.prm_marketplace.controller;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Notification;
import za.ac.cput.prm_marketplace.domain.NotificationType;
import za.ac.cput.prm_marketplace.service.INotificationService;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private INotificationService notificationService;

    private UUID id;
    private UUID userId;
    private Notification notification;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        userId = UUID.randomUUID();
        notification = buildNotification(false);
    }

    private Notification buildNotification(boolean read) {
        return new Notification.Builder()
                .setId(id)
                .setUserId(userId)
                .setType(NotificationType.PAYMENT)
                .setTitle("Payment successful")
                .setMessage("Your payment went through.")
                .setRead(read)
                .build();
    }

    @Test
    void create_returnsCreatedWhenServiceSucceeds() throws Exception {
        when(notificationService.create(any(Notification.class))).thenReturn(notification);

        mockMvc.perform(post("/notifications")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(notification)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Payment successful"))
                .andExpect(jsonPath("$.read").value(false));
    }

    @Test
    void create_returnsBadRequestWhenServiceRejects() throws Exception {
        when(notificationService.create(any(Notification.class))).thenReturn(null);

        mockMvc.perform(post("/notifications")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(notification)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void read_existingNotification_returnsOk() throws Exception {
        when(notificationService.read(id)).thenReturn(notification);

        mockMvc.perform(get("/notifications/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PAYMENT"));
    }

    @Test
    void read_missingNotification_returnsNotFound() throws Exception {
        when(notificationService.read(id)).thenReturn(null);

        mockMvc.perform(get("/notifications/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_existingNotification_returnsOk() throws Exception {
        when(notificationService.update(any(Notification.class))).thenReturn(notification);

        mockMvc.perform(put("/notifications")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(notification)))
                .andExpect(status().isOk());
    }

    @Test
    void update_missingNotification_returnsNotFound() throws Exception {
        when(notificationService.update(any(Notification.class))).thenReturn(null);

        mockMvc.perform(put("/notifications")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(notification)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingNotification_returnsNoContent() throws Exception {
        when(notificationService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/notifications/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_missingNotification_returnsNotFound() throws Exception {
        when(notificationService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/notifications/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_returnsList() throws Exception {
        when(notificationService.getAll()).thenReturn(Arrays.asList(notification, buildNotification(true)));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByUserId_returnsList() throws Exception {
        when(notificationService.getByUserId(userId)).thenReturn(Arrays.asList(notification));

        mockMvc.perform(get("/notifications/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getUnreadByUserId_returnsList() throws Exception {
        when(notificationService.getUnreadByUserId(userId)).thenReturn(Arrays.asList(notification));

        mockMvc.perform(get("/notifications/user/{userId}/unread", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void countUnread_returnsCount() throws Exception {
        when(notificationService.countUnread(userId)).thenReturn(4L);

        mockMvc.perform(get("/notifications/user/{userId}/unread/count", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("4"));
    }

    @Test
    void markAsRead_existingNotification_returnsOk() throws Exception {
        when(notificationService.markAsRead(id)).thenReturn(buildNotification(true));

        mockMvc.perform(patch("/notifications/{id}/read", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markAsRead_missingNotification_returnsNotFound() throws Exception {
        when(notificationService.markAsRead(id)).thenReturn(null);

        mockMvc.perform(patch("/notifications/{id}/read", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAllAsRead_returnsNumberUpdated() throws Exception {
        when(notificationService.markAllAsRead(userId)).thenReturn(3);

        mockMvc.perform(patch("/notifications/user/{userId}/read-all", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }
}
