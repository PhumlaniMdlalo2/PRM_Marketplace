package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.SavedItem;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.ISavedItemService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/saved-items")
public class SavedItemController {

    private final ISavedItemService savedItemService;

    @Autowired
    public SavedItemController(ISavedItemService savedItemService) {
        this.savedItemService = savedItemService;
    }

    /**
     * The caller's saved items. This replaces the old "/user/{userId}" route and the bare
     * "GET /api/saved-items", which returned every user's saved list.
     */
    @GetMapping
    public ResponseEntity<List<SavedItem>> getAll(Authentication authentication) {
        return ResponseEntity.ok(savedItemService.getByUser(CurrentCaller.id(authentication)));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countByUser(Authentication authentication) {
        return ResponseEntity.ok(savedItemService.countByUser(CurrentCaller.id(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavedItem> read(@PathVariable UUID id, Authentication authentication) {
        SavedItem savedItem = savedItemService.read(id, CurrentCaller.id(authentication));
        if (savedItem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(savedItem);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        if (!savedItemService.delete(id, CurrentCaller.id(authentication))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Saves or unsaves a product on the caller's own list. The userId segment is gone: it was read
     * from the path, so one account could save and unsave items on any other account's list.
     */
    @PostMapping("/product/{productId}/toggle")
    public ResponseEntity<SavedItem> toggle(@PathVariable UUID productId, Authentication authentication) {
        SavedItem result = savedItemService.toggle(CurrentCaller.id(authentication), productId);
        return result == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(result);
    }

    @DeleteMapping("/product/{productId}")
    public ResponseEntity<Void> removeByUserAndProduct(@PathVariable UUID productId,
                                                       Authentication authentication) {
        if (!savedItemService.removeByUserAndProduct(CurrentCaller.id(authentication), productId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}