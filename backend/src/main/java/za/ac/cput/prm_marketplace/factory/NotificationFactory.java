package za.ac.cput.prm_marketplace.factory;

import za.ac.cput.prm_marketplace.domain.Notification;
import za.ac.cput.prm_marketplace.domain.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationFactory {

    public static Notification createNotification(UUID userId, NotificationType type,
                                                  String title, String message) {

        if (userId == null) {
            return null;
        }

        if (type == null) {
            return null;
        }

        if (title == null || title.isBlank()) {
            return null;
        }

        if (message == null || message.isBlank()) {
            return null;
        }

        return new Notification.Builder()
                .setUserId(userId)
                .setType(type)
                .setTitle(title)
                .setMessage(message)
                .setRead(false)
                .setCreatedAt(LocalDateTime.now())
                .build();
    }
}

