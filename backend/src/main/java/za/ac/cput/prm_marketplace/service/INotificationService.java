package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Notification;
import za.ac.cput.prm_marketplace.domain.NotificationType;

import java.util.List;
import java.util.UUID;

public interface INotificationService {

    Notification create(Notification notification);

    Notification read(UUID id);

    Notification update(Notification notification);

    boolean delete(UUID id);

    List<Notification> getAll();

    /**
     * Convenience method other services (payments, orders, reviews) can call
     * to notify a user without building a Notification themselves.
     */
    Notification send(UUID userId, NotificationType type, String title, String message);

    List<Notification> getByUserId(UUID userId);

    List<Notification> getUnreadByUserId(UUID userId);

    long countUnread(UUID userId);

    Notification markAsRead(UUID id);

    int markAllAsRead(UUID userId);
}

