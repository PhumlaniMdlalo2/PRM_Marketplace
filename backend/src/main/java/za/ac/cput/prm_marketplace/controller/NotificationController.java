package za.ac.cput.prm_marketplace.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Notification;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.INotificationService;

import java.util.List;
import java.util.UUID;

/**
 * Mounted under "/api" to match the rest of the application. The old "/notifications" mapping sat
 * outside the "/api/**" group, which is where the security rules are expressed.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final INotificationService notificationService;

    public NotificationController(INotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * The caller's notifications. The old "GET /notifications" returned every notification for
     * every account, and "/user/{userId}" let any caller ask for a specific person's inbox.
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getAll(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getByUserId(CurrentCaller.id(authentication)));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnread(Authentication authentication) {
        return ResponseEntity.ok(
                notificationService.getUnreadByUserId(CurrentCaller.id(authentication)));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> countUnread(Authentication authentication) {
        return ResponseEntity.ok(notificationService.countUnread(CurrentCaller.id(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> read(@PathVariable UUID id, Authentication authentication) {
        Notification notification = notificationService.read(id, CurrentCaller.id(authentication));
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(notification);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        if (!notificationService.delete(id, CurrentCaller.id(authentication))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable UUID id,
                                                  Authentication authentication) {
        Notification updated = notificationService.markAsRead(id, CurrentCaller.id(authentication));
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Integer> markAllAsRead(Authentication authentication) {
        return ResponseEntity.ok(notificationService.markAllAsRead(CurrentCaller.id(authentication)));
    }
}