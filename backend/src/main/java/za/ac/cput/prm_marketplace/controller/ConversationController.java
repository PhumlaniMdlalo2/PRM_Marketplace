package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IConversationService;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final IConversationService conversationService;

    @Autowired
    public ConversationController(IConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * The caller's threads. The former bare "GET /api/conversations" returned every private
     * conversation in the system, and "/user/{userId}" let any caller list another person's.
     */
    @GetMapping
    public ResponseEntity<List<Conversation>> getAll(Authentication authentication) {
        return ResponseEntity.ok(conversationService.getForUser(CurrentCaller.id(authentication)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(Authentication authentication) {
        return ResponseEntity.ok(conversationService.unreadCount(CurrentCaller.id(authentication)));
    }

    /** @return 404 unless the caller is one of the two participants */
    @GetMapping("/{id}")
    public ResponseEntity<Conversation> read(@PathVariable UUID id, Authentication authentication) {
        Conversation conversation = conversationService.read(id, CurrentCaller.id(authentication));
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(conversation);
    }

    /**
     * Opens or reuses a thread with another person. The old endpoint took a whole Conversation body
     * and trusted the buyer and seller inside it, which let a caller post as any other user and
     * forge who was selling. Now the caller is always the buyer and only the other party and an
     * optional product id are taken from the request.
     */
    @PostMapping("/start")
    public ResponseEntity<Conversation> start(@RequestParam UUID sellerId,
                                              @RequestParam(required = false) UUID productId,
                                              Authentication authentication) {
        Conversation conversation = conversationService.getOrCreate(
                CurrentCaller.id(authentication), sellerId, productId);
        if (conversation == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(conversation);
    }

    @DeleteMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Nothing to return: the change was applied and there is no state left to read.")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        if (!conversationService.delete(id, CurrentCaller.id(authentication))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
