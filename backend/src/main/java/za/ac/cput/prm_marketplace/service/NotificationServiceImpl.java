package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Notification;
import za.ac.cput.prm_marketplace.domain.NotificationType;
import za.ac.cput.prm_marketplace.factory.NotificationFactory;
import za.ac.cput.prm_marketplace.repository.NotificationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification send(UUID userId, NotificationType type, String title, String message) {
        if (userId == null || type == null) {
            return null;
        }
        Notification notification =
                NotificationFactory.createNotification(userId, type, title, message);
        if (notification == null) {
            return null;
        }
        return notificationRepository.save(notification);
    }

    @Override
    public Notification read(UUID id, UUID requesterId) {
        Notification notification = find(id);
        return isAddressedTo(notification, requesterId) ? notification : null;
    }

    @Override
    @Transactional
    public boolean delete(UUID id, UUID requesterId) {
        if (read(id, requesterId) == null) {
            return false;
        }
        notificationRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Notification> getByUserId(UUID requesterId) {
        if (requesterId == null) {
            return List.of();
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(requesterId);
    }

    @Override
    public List<Notification> getUnreadByUserId(UUID requesterId) {
        if (requesterId == null) {
            return List.of();
        }
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(requesterId);
    }

    @Override
    public long countUnread(UUID requesterId) {
        if (requesterId == null) {
            return 0;
        }
        return notificationRepository.countByUserIdAndReadFalse(requesterId);
    }

    @Override
    @Transactional
    public Notification markAsRead(UUID id, UUID requesterId) {
        Notification existing = read(id, requesterId);
        if (existing == null || existing.isRead()) {
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
    public int markAllAsRead(UUID requesterId) {
        if (requesterId == null) {
            return 0;
        }
        List<Notification> unread =
                notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(requesterId);
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

    private Notification find(UUID id) {
        if (id == null) {
            return null;
        }
        return notificationRepository.findById(id).orElse(null);
    }

    private boolean isAddressedTo(Notification notification, UUID requesterId) {
        return notification != null
                && notification.getUserId() != null
                && notification.getUserId().equals(requesterId);
    }
}