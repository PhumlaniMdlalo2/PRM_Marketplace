package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.PostLike;
import za.ac.cput.prm_marketplace.service.IPostLikeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/post-likes")
public class PostLikeController {

    private final IPostLikeService postLikeService;

    @Autowired
    public PostLikeController(IPostLikeService postLikeService) {
        this.postLikeService = postLikeService;
    }

    @PostMapping
    public ResponseEntity<PostLike> create(@RequestBody PostLike postLike) {
        PostLike created = postLikeService.create(postLike);
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostLike> read(@PathVariable UUID id) {
        PostLike postLike = postLikeService.read(id);
        if (postLike == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(postLike);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostLike> update(@PathVariable UUID id, @RequestBody PostLike postLike) {
        PostLike existing = postLikeService.read(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        PostLike toUpdate = new PostLike.Builder().copy(postLike).setId(id).build();
        return ResponseEntity.ok(postLikeService.update(toUpdate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!postLikeService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<PostLike>> getAll() {
        return ResponseEntity.ok(postLikeService.getAll());
    }

    @PostMapping("/post/{postId}/user/{userId}/toggle")
    public ResponseEntity<PostLike> toggle(@PathVariable UUID postId, @PathVariable UUID userId) {
        PostLike result = postLikeService.toggle(postId, userId);
        return result == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(result);
    }

    @GetMapping("/post/{postId}/user/{userId}")
    public ResponseEntity<Boolean> hasLiked(@PathVariable UUID postId, @PathVariable UUID userId) {
        return ResponseEntity.ok(postLikeService.hasLiked(postId, userId));
    }

    @GetMapping("/post/{postId}/count")
    public ResponseEntity<Long> countByPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(postLikeService.countByPost(postId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostLike>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(postLikeService.getByUser(userId));
    }
}