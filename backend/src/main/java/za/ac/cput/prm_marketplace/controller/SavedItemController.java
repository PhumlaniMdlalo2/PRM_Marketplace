package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.SavedItem;
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

    @PostMapping
    public ResponseEntity<SavedItem> create(@RequestBody SavedItem savedItem) {
        SavedItem created = savedItemService.create(savedItem);
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavedItem> read(@PathVariable UUID id) {
        SavedItem savedItem = savedItemService.read(id);
        if (savedItem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(savedItem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavedItem> update(@PathVariable UUID id, @RequestBody SavedItem savedItem) {
        SavedItem existing = savedItemService.read(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        SavedItem toUpdate = new SavedItem.Builder().copy(savedItem).setId(id).build();
        return ResponseEntity.ok(savedItemService.update(toUpdate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!savedItemService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<SavedItem>> getAll() {
        return ResponseEntity.ok(savedItemService.getAll());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SavedItem>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(savedItemService.getByUser(userId));
    }

    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> countByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(savedItemService.countByUser(userId));
    }

    @PostMapping("/user/{userId}/product/{productId}/toggle")
    public ResponseEntity<SavedItem> toggle(@PathVariable UUID userId, @PathVariable UUID productId) {
        SavedItem result = savedItemService.toggle(userId, productId);
        return result == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(result);
    }

    @DeleteMapping("/user/{userId}/product/{productId}")
    public ResponseEntity<Void> removeByUserAndProduct(@PathVariable UUID userId,
                                                       @PathVariable UUID productId) {
        if (!savedItemService.removeByUserAndProduct(userId, productId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}