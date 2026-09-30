package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.service.IConversationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final IConversationService conversationService;

    @Autowired
    public ConversationController(IConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<Conversation> create(@RequestBody Conversation conversation) {
        Conversation created = conversationService.create(conversation);
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Conversation> read(@PathVariable UUID id) {
        Conversation conversation = conversationService.read(id);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(conversation);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Conversation> update(@PathVariable UUID id,
                                               @RequestBody Conversation conversation) {
        Conversation existing = conversationService.read(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Conversation toUpdate = new Conversation.Builder().copy(conversation).setId(id).build();
        return ResponseEntity.ok(conversationService.update(toUpdate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!conversationService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Conversation>> getAll() {
        return ResponseEntity.ok(conversationService.getAll());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Conversation>> getForUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(conversationService.getForUser(userId));
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Long> unreadCount(@PathVariable UUID userId) {
        return ResponseEntity.ok(conversationService.unreadCount(userId));
    }

    @GetMapping("/{id}/participants/{userId}")
    public ResponseEntity<Conversation> readForParticipant(@PathVariable UUID id,
                                                          @PathVariable UUID userId) {
        Conversation conversation = conversationService.readForParticipant(id, userId);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(conversation);
    }

    @PostMapping("/start")
    public ResponseEntity<Conversation> getOrCreate(@RequestBody Conversation request) {
        if (request == null || request.getBuyer() == null || request.getSeller() == null) {
            return ResponseEntity.badRequest().build();
        }
        Conversation conversation = conversationService.getOrCreate(
                request.getBuyer(), request.getSeller(), request.getProduct());
        if (conversation == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(conversation);
    }
}