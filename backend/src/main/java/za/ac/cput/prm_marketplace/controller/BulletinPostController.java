package za.ac.cput.prm_marketplace.controller;

import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.service.IBulletinPostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/bulletin-posts")
public class BulletinPostController {

    private final IBulletinPostService bulletinPostService;

    public BulletinPostController(IBulletinPostService bulletinPostService) {
        this.bulletinPostService = bulletinPostService;
    }

    @PostMapping
    public ResponseEntity<BulletinPost> create(@RequestBody BulletinPost bulletinPost) {
        BulletinPost created = bulletinPostService.create(bulletinPost);
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BulletinPost> read(@PathVariable UUID id) {
        BulletinPost bulletinPost = bulletinPostService.read(id);
        if (bulletinPost == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(bulletinPost);
    }

    @PutMapping
    public ResponseEntity<BulletinPost> update(@RequestBody BulletinPost bulletinPost) {
        BulletinPost updated = bulletinPostService.update(bulletinPost);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        boolean deleted = bulletinPostService.delete(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<BulletinPost>> getAll() {
        return ResponseEntity.ok(bulletinPostService.getAll());
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<BulletinPost> findByAuthorId(@PathVariable UUID authorId) {
        Optional<BulletinPost> bulletinPost = bulletinPostService.findByAuthorId(authorId);
        return bulletinPost
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}