package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Notification;
import za.ac.cput.prm_marketplace.domain.NotificationType;

import java.util.List;
import java.util.UUID;

/**
 * Notifications are private to the account they were addressed to. Read, delete and mark-as-read
 * are all scoped by the caller's id.
 *
 * <p>{@link #send} is the only way a notification comes into existence and is deliberately not
 * exposed over HTTP: the audience and the read flag are server decisions, not request data.
 */
public interface INotificationService {

    Notification send(UUID userId, NotificationType type, String title, String message);

    /** @return the notification, or null when it does not exist or belongs to someone else */
    Notification read(UUID id, UUID requesterId);

    /** @return false when it does not exist or belongs to someone else */
    boolean delete(UUID id, UUID requesterId);

    List<Notification> getByUserId(UUID requesterId);

    List<Notification> getUnreadByUserId(UUID requesterId);

    long countUnread(UUID requesterId);

    /** @return the updated notification, or null when it does not exist or is not the caller's */
    Notification markAsRead(UUID id, UUID requesterId);

    int markAllAsRead(UUID requesterId);
}