package za.ac.cput.prm_marketplace.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Message;
import za.ac.cput.prm_marketplace.dto.SendMessageRequest;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IMessageService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final IMessageService messageService;

    @Autowired
    public MessageController(IMessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * The messages in a thread the caller belongs to. "GET /api/messages" returned the entire
     * message history of every conversation in the marketplace to any authenticated caller; that
     * endpoint no longer exists, so there is no way to page through other people's private mail.
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<Message>> getByConversation(@PathVariable UUID conversationId,
                                                           Authentication authentication) {
        return ResponseEntity.ok(messageService.getByConversation(
                conversationId, CurrentCaller.id(authentication)));
    }

    @GetMapping("/conversation/{conversationId}/unread-count")
    public ResponseEntity<Long> unreadCount(@PathVariable UUID conversationId,
                                            Authentication authentication) {
        return ResponseEntity.ok(messageService.unreadCount(
                conversationId, CurrentCaller.id(authentication)));
    }

    /**
     * Sends a message as the caller. The senderId parameter is gone: it was read from the request,
     * so anyone could post into a thread under somebody else's name.
     *
     * <p>The {@code body} request parameter is gone for the same reason the passwords are out of the
     * change-password URL. Message text is private correspondence between two named accounts, and as
     * a parameter it sat in the request line, which means it was written to this server's access log,
     * to every proxy log along the way, and to the sender's browser history. Private mail should have
     * exactly two readers. Validation now lives on {@link SendMessageRequest}, which also caps the
     * length at the column's own limit so an over-long message is a 400 rather than a truncated row.
     */
    @PostMapping("/conversation/{conversationId}/send")
    public ResponseEntity<Message> send(@PathVariable UUID conversationId,
                                        @Valid @RequestBody SendMessageRequest request,
                                        Authentication authentication) {
        Message sent = messageService.send(conversationId, CurrentCaller.id(authentication),
                request.body());
        if (sent == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(sent, HttpStatus.CREATED);
    }

    /**
     * Marks the caller's own unread messages in the thread as read. The readerId parameter is gone
     * for the same reason as senderId above.
     */
    @PatchMapping("/conversation/{conversationId}/read")
    public ResponseEntity<Map<String, Integer>> markRead(@PathVariable UUID conversationId,
                                                         Authentication authentication) {
        int updated = messageService.markRead(conversationId, CurrentCaller.id(authentication));
        return ResponseEntity.ok(Map.of("markedRead", updated));
    }

    /** @return 404 unless the caller is a participant in the message's conversation */
    @GetMapping("/{id}")
    public ResponseEntity<Message> read(@PathVariable UUID id, Authentication authentication) {
        Message message = messageService.read(id, CurrentCaller.id(authentication));
        if (message == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(message);
    }
}