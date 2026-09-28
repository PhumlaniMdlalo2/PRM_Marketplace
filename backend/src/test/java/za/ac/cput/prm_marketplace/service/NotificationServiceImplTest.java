package za.ac.cput.prm_marketplace.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Notification;
import za.ac.cput.prm_marketplace.domain.NotificationType;
import za.ac.cput.prm_marketplace.repository.NotificationRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID id;
    private UUID userId;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private Notification buildNotification(boolean read) {
        return new Notification.Builder()
                .setId(id)
                .setUserId(userId)
                .setType(NotificationType.ORDER)
                .setTitle("Order placed")
                .setMessage("Your order has been placed.")
                .setRead(read)
                .build();
    }

    // create / send

    @Test
    void create_validNotification_savesAsUnread() {
        Notification request = new Notification.Builder()
                .setUserId(userId).setType(NotificationType.SYSTEM)
                .setTitle("Welcome").setMessage("Welcome to the marketplace")
                .setRead(true) // client must not be able to create it as already read
                .build();
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification created = notificationService.create(request);

        assertNotNull(created);
        assertFalse(created.isRead());
        assertNotNull(created.getCreatedAt());
    }

    @Test
    void create_invalidNotification_returnsNull() {
        assertNull(notificationService.create(null));
        assertNull(notificationService.create(new Notification.Builder().setUserId(userId).build()));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void send_validArguments_savesNotification() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification sent = notificationService.send(userId, NotificationType.PAYMENT, "Title", "Message");

        assertNotNull(sent);
        assertEquals(userId, sent.getUserId());
        assertEquals(NotificationType.PAYMENT, sent.getType());
    }

    // read / update / delete

    @Test
    void read_existingNotification_returnsIt() {
        Notification notification = buildNotification(false);
        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        assertEquals(notification, notificationService.read(id));
    }

    @Test
    void read_missingNotification_returnsNull() {
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertNull(notificationService.read(id));
        assertNull(notificationService.read(null));
    }

    @Test
    void update_existingNotification_saves() {
        Notification notification = buildNotification(false);
        when(notificationRepository.existsById(id)).thenReturn(true);
        when(notificationRepository.save(notification)).thenReturn(notification);

        assertEquals(notification, notificationService.update(notification));
    }

    @Test
    void update_missingNotification_returnsNull() {
        when(notificationRepository.existsById(id)).thenReturn(false);

        assertNull(notificationService.update(buildNotification(false)));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void delete_existingNotification_returnsTrue() {
        when(notificationRepository.existsById(id)).thenReturn(true);

        assertTrue(notificationService.delete(id));
        verify(notificationRepository).deleteById(id);
    }

    @Test
    void delete_missingNotification_returnsFalse() {
        when(notificationRepository.existsById(id)).thenReturn(false);

        assertFalse(notificationService.delete(id));
    }

    // per-user queries

    @Test
    void getByUserId_returnsNotificationsForUser() {
        List<Notification> list = Arrays.asList(buildNotification(false), buildNotification(true));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(list);

        assertEquals(2, notificationService.getByUserId(userId).size());
        assertTrue(notificationService.getByUserId(null).isEmpty());
    }

    @Test
    void getUnreadByUserId_returnsOnlyUnread() {
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId))
                .thenReturn(Arrays.asList(buildNotification(false)));

        assertEquals(1, notificationService.getUnreadByUserId(userId).size());
    }

    @Test
    void countUnread_returnsRepositoryCount() {
        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(3L);

        assertEquals(3L, notificationService.countUnread(userId));
        assertEquals(0L, notificationService.countUnread(null));
    }

    // mark as read

    @Test
    void markAsRead_unreadNotification_savesAsRead() {
        when(notificationRepository.findById(id)).thenReturn(Optional.of(buildNotification(false)));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.markAsRead(id);

        assertTrue(result.isRead());
    }

    @Test
    void markAsRead_alreadyRead_doesNotSaveAgain() {
        when(notificationRepository.findById(id)).thenReturn(Optional.of(buildNotification(true)));

        Notification result = notificationService.markAsRead(id);

        assertTrue(result.isRead());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markAsRead_missingNotification_returnsNull() {
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertNull(notificationService.markAsRead(id));
    }

    @Test
    void markAllAsRead_updatesEveryUnreadNotification() {
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId))
                .thenReturn(Arrays.asList(buildNotification(false), buildNotification(false)));

        int count = notificationService.markAllAsRead(userId);

        assertEquals(2, count);
        verify(notificationRepository).saveAll(any(Iterable.class));
    }

    @Test
    void markAllAsRead_nothingUnread_returnsZero() {
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId))
                .thenReturn(Arrays.<Notification>asList());

        assertEquals(0, notificationService.markAllAsRead(userId));
        verify(notificationRepository, never()).saveAll(any(Iterable.class));
    }
}
