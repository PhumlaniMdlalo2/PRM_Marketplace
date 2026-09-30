package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Message;
import za.ac.cput.prm_marketplace.service.IConversationService;
import za.ac.cput.prm_marketplace.service.IMessageService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final IMessageService messageService;
    private final IConversationService conversationService;

    @Autowired
    public MessageController(IMessageService messageService, IConversationService conversationService) {
        this.messageService = messageService;
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<Message> create(@RequestBody Message message) {
        Message created = messageService.create(message);
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Message> read(@PathVariable UUID id) {
        Message message = messageService.read(id);
        if (message == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(message);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Message> update(@PathVariable UUID id, @RequestBody Message message) {
        Message existing = messageService.read(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Message toUpdate = new Message.Builder().copy(message).setId(id).build();
        return ResponseEntity.ok(messageService.update(toUpdate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!messageService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Message>> getAll() {
        return ResponseEntity.ok(messageService.getAll());
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<Message>> getByConversation(@PathVariable UUID conversationId) {
        return ResponseEntity.ok(messageService.getByConversation(conversationId));
    }

    @GetMapping("/conversation/{conversationId}/unread-count")
    public ResponseEntity<Long> unreadCount(@PathVariable UUID conversationId) {
        return ResponseEntity.ok(messageService.unreadCount(conversationId));
    }

    @PostMapping("/conversation/{conversationId}/send")
    public ResponseEntity<Message> send(@PathVariable UUID conversationId,
                                        @RequestParam UUID senderId,
                                        @RequestParam String body) {
        Message sent = messageService.send(conversationId, senderId, body);
        if (sent == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(sent, HttpStatus.CREATED);
    }

    @PatchMapping("/conversation/{conversationId}/read")
    public ResponseEntity<Map<String, Integer>> markRead(@PathVariable UUID conversationId,
                                                         @RequestParam UUID readerId) {
        int updated = messageService.markRead(conversationId, readerId);
        return ResponseEntity.ok(Map.of("markedRead", updated));
    }
}