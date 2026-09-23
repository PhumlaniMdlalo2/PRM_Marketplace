package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Notification;
import za.ac.cput.prm_marketplace.domain.NotificationType;
import za.ac.cput.prm_marketplace.factory.NotificationFactory;
import za.ac.cput.prm_marketplace.repository.NotificationRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification create(Notification notification) {
        if (notification == null) {
            return null;
        }
        // Go through the factory so new notifications always start unread
        // and have a createdAt timestamp, whatever the client sent.
        return send(notification.getUserId(), notification.getType(),
                notification.getTitle(), notification.getMessage());
    }

    @Override
    public Notification read(UUID id) {
        if (id == null) {
            return null;
        }
        return notificationRepository.findById(id).orElse(null);
    }

    @Override
    public Notification update(Notification notification) {
        if (notification == null || notification.getId() == null ||
                !notificationRepository.existsById(notification.getId())) {
            return null;
        }
        return notificationRepository.save(notification);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !notificationRepository.existsById(id)) {
            return false;
        }

        notificationRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Notification> getAll() {
        return notificationRepository.findAll();
    }

    @Override
    public Notification send(UUID userId, NotificationType type, String title, String message) {
        Notification notification =
                NotificationFactory.createNotification(userId, type, title, message);

        if (notification == null) {
            return null;
        }
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getByUserId(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Notification> getUnreadByUserId(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    public long countUnread(UUID userId) {
        if (userId == null) {
            return 0;
        }
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public Notification markAsRead(UUID id) {
        Notification existing = read(id);

        if (existing == null) {
            return null;
        }

        if (existing.isRead()) {
            return existing;
        }

        Notification updated = new Notification.Builder()
                .copy(existing)
                .setRead(true)
                .build();

        return notificationRepository.save(updated);
    }

    @Override
    @Transactional
    public int markAllAsRead(UUID userId) {
        if (userId == null) {
            return 0;
        }

        List<Notification> unread =
                notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);

        if (unread.isEmpty()) {
            return 0;
        }

        List<Notification> updated = new ArrayList<>();
        for (Notification notification : unread) {
            updated.add(new Notification.Builder()
                    .copy(notification)
                    .setRead(true)
                    .build());
        }

        notificationRepository.saveAll(updated);
        return updated.size();
    }
}
