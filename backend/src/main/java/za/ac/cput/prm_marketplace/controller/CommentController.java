package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Comment;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
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

    /**
     * Posts a comment as the caller. The author used to come from the request body, so anyone could
     * post under another person's name.
     */
    @PostMapping
    public ResponseEntity<Comment> create(@RequestBody Comment comment, Authentication authentication) {
        Comment created = commentService.create(comment, CurrentCaller.id(authentication));
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /** Reading a thread is public. There is no endpoint that lists comments across all posts. */
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

    @GetMapping("/post/{postId}/count")
    public ResponseEntity<Long> countByPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(commentService.countByPost(postId));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<Comment>> getByAuthor(@PathVariable UUID authorId) {
        return ResponseEntity.ok(commentService.getByAuthor(authorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Comment> read(@PathVariable UUID id) {
        Comment comment = commentService.read(id);
        if (comment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(comment);
    }

    /** @return 404 unless the caller wrote the comment */
    @PutMapping("/{id}")
    public ResponseEntity<Comment> update(@PathVariable UUID id,
                                          @RequestBody Comment comment,
                                          Authentication authentication) {
        Comment toUpdate = new Comment.Builder().copy(comment).setId(id).build();
        Comment updated = commentService.update(toUpdate, CurrentCaller.id(authentication));
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /** @return 404 unless the caller wrote the comment */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        if (!commentService.delete(id, CurrentCaller.id(authentication))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}