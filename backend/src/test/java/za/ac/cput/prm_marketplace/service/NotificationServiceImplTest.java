package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Notification;
import za.ac.cput.prm_marketplace.domain.NotificationType;
import za.ac.cput.prm_marketplace.repository.NotificationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationServiceImpl service;

    private UUID ownerId;
    private UUID intruderId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notificationRepository);
        ownerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
    }

    @Test
    @DisplayName("send saves a notification addressed to the given user, unread")
    void send_savesUnreadNotification() {
        when(notificationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Notification sent = service.send(ownerId, NotificationType.SYSTEM, "Welcome", "Hello");

        assertThat(sent).isNotNull();
        assertThat(sent.getUserId()).isEqualTo(ownerId);
        assertThat(sent.isRead()).isFalse();
    }

    @Test
    @DisplayName("send requires a recipient and a type")
    void send_rejectsIncompleteArguments() {
        assertThat(service.send(null, NotificationType.SYSTEM, "t", "m")).isNull();
        assertThat(service.send(ownerId, null, "t", "m")).isNull();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("read returns the caller's own notification")
    void read_ownedNotificationIsReturned() {
        Notification notification = buildNotification(ownerId);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThat(service.read(notificationId, ownerId)).isSameAs(notification);
    }

    @Test
    @DisplayName("read hides a notification addressed to somebody else")
    void read_foreignNotificationIsHidden() {
        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(buildNotification(intruderId)));

        assertThat(service.read(notificationId, ownerId)).isNull();
    }

    @Test
    @DisplayName("read returns null for a missing notification or a null id")
    void read_missingReturnsNull() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThat(service.read(notificationId, ownerId)).isNull();
        assertThat(service.read(null, ownerId)).isNull();
    }

    @Test
    @DisplayName("delete removes the caller's own notification")
    void delete_ownedNotificationIsRemoved() {
        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(buildNotification(ownerId)));

        assertThat(service.delete(notificationId, ownerId)).isTrue();
        verify(notificationRepository).deleteById(notificationId);
    }

    @Test
    @DisplayName("delete refuses to remove somebody else's notification")
    void delete_foreignNotificationIsRefused() {
        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(buildNotification(intruderId)));

        assertThat(service.delete(notificationId, ownerId)).isFalse();
        verify(notificationRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("the inbox is scoped to the caller")
    void getByUserId_scopesToTheRequester() {
        Notification notification = buildNotification(ownerId);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(ownerId))
                .thenReturn(List.of(notification));

        assertThat(service.getByUserId(ownerId)).containsExactly(notification);
        assertThat(service.getByUserId(null)).isEmpty();
    }

    @Test
    @DisplayName("unread notifications are scoped to the caller")
    void getUnreadByUserId_scopesToTheRequester() {
        Notification notification = buildNotification(ownerId);
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(ownerId))
                .thenReturn(List.of(notification));

        assertThat(service.getUnreadByUserId(ownerId)).containsExactly(notification);
        assertThat(service.getUnreadByUserId(null)).isEmpty();
    }

    @Test
    @DisplayName("the unread count is scoped to the caller")
    void countUnread_scopesToTheRequester() {
        when(notificationRepository.countByUserIdAndReadFalse(ownerId)).thenReturn(6L);

        assertThat(service.countUnread(ownerId)).isEqualTo(6L);
        assertThat(service.countUnread(null)).isZero();
    }

    @Test
    @DisplayName("marking the caller's own notification as read saves it")
    void markAsRead_savesAsRead() {
        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(buildNotification(ownerId)));
        when(notificationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Notification updated = service.markAsRead(notificationId, ownerId);

        assertThat(updated).isNotNull();
        assertThat(updated.isRead()).isTrue();
    }

    @Test
    @DisplayName("marking somebody else's notification as read changes nothing")
    void markAsRead_foreignNotificationIsRefused() {
        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(buildNotification(intruderId)));

        assertThat(service.markAsRead(notificationId, ownerId)).isNull();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("an already-read notification is not written again")
    void markAsRead_alreadyReadDoesNotSave() {
        Notification notification = new Notification.Builder()
                .copy(buildNotification(ownerId))
                .setRead(true)
                .build();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThat(service.markAsRead(notificationId, ownerId)).isSameAs(notification);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("mark-as-read keeps the recipient from the stored row")
    void markAsRead_doesNotReassignRecipient() {
        Notification stored = buildNotification(ownerId);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(stored));
        when(notificationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Notification updated = service.markAsRead(notificationId, ownerId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(ownerId);
        assertThat(updated.getUserId()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("mark-all-as-read only rewrites the caller's unread rows")
    void markAllAsRead_scopesToTheRequester() {
        Notification first = buildNotification(ownerId);
        Notification second = buildNotification(ownerId);
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(ownerId))
                .thenReturn(List.of(first, second));

        assertThat(service.markAllAsRead(ownerId)).isEqualTo(2);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).allMatch(Notification::isRead);
    }

    @Test
    @DisplayName("mark-all-as-read with nothing unread reports zero and writes nothing")
    void markAllAsRead_nothingUnreadReportsZero() {
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(ownerId))
                .thenReturn(List.of());

        assertThat(service.markAllAsRead(ownerId)).isZero();
        assertThat(service.markAllAsRead(null)).isZero();
        verify(notificationRepository, never()).saveAll(any());
    }

    private Notification buildNotification(UUID recipientId) {
        return new Notification.Builder()
                .setId(notificationId)
                .setUserId(recipientId)
                .setType(NotificationType.SYSTEM)
                .setTitle("Welcome")
                .setMessage("Hello")
                .build();
    }
}