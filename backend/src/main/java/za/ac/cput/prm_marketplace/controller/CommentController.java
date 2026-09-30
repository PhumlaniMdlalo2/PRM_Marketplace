package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Comment;
import za.ac.cput.prm_marketplace.service.ICommentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final ICommentService commentService;

    @Autowired
    public CommentController(ICommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<Comment> create(@RequestBody Comment comment) {
        Comment created = commentService.create(comment);
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Comment> read(@PathVariable UUID id) {
        Comment comment = commentService.read(id);
        if (comment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(comment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Comment> update(@PathVariable UUID id, @RequestBody Comment comment) {
        Comment existing = commentService.read(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Comment toUpdate = new Comment.Builder().copy(comment).setId(id).build();
        return ResponseEntity.ok(commentService.update(toUpdate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!commentService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Comment>> getAll() {
        return ResponseEntity.ok(commentService.getAll());
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> getByPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(commentService.getByPost(postId));
    }

    @GetMapping("/post/{postId}/top-level")
    public ResponseEntity<List<Comment>> getTopLevelByPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(commentService.getTopLevelByPost(postId));
    }

    @GetMapping("/post/{postId}/replies/{parentId}")
    public ResponseEntity<List<Comment>> getReplies(@PathVariable UUID postId,
                                                    @PathVariable UUID parentId) {
        return ResponseEntity.ok(commentService.getReplies(postId, parentId));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<Comment>> getByAuthor(@PathVariable UUID authorId) {
        return ResponseEntity.ok(commentService.getByAuthor(authorId));
    }

    @GetMapping("/post/{postId}/count")
    public ResponseEntity<Long> countByPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(commentService.countByPost(postId));
    }
}