package za.ac.cput.prm_marketplace.factory;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import za.ac.cput.prm_marketplace.domain.Notification;
import za.ac.cput.prm_marketplace.domain.NotificationType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationFactoryTest {

    private final UUID userId = UUID.randomUUID();

    @Test
    void createsUnreadNotificationWithValidFields() {
        Notification notification = NotificationFactory.createNotification(userId,
                NotificationType.PAYMENT, "Payment successful", "Your payment went through.");

        assertThat(notification).isNotNull();
        assertThat(notification.getUserId()).isEqualTo(userId);
        assertThat(notification.getType()).isEqualTo(NotificationType.PAYMENT);
        assertThat(notification.getTitle()).isEqualTo("Payment successful");
        assertThat(notification.getMessage()).isEqualTo("Your payment went through.");
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getCreatedAt()).isNotNull();
    }

    @Test
    void returnsNullWhenUserIdIsNull() {
        assertThat(NotificationFactory.createNotification(null, NotificationType.SYSTEM, "Title", "Message")).isNull();
    }

    @Test
    void returnsNullWhenTypeIsNull() {
        assertThat(NotificationFactory.createNotification(userId, null, "Title", "Message")).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void returnsNullWhenTitleIsBlankOrNull(String title) {
        assertThat(NotificationFactory.createNotification(userId, NotificationType.SYSTEM, title, "Message")).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void returnsNullWhenMessageIsBlankOrNull(String message) {
        assertThat(NotificationFactory.createNotification(userId, NotificationType.SYSTEM, "Title", message)).isNull();
    }
}
