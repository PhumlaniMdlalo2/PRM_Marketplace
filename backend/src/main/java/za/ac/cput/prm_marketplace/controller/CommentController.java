package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.Comment;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.ICommentService;
import za.ac.cput.prm_marketplace.service.IStudentDiscussionGroupService;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final ICommentService commentService;
    private final IStudentDiscussionGroupService groupService;

    @Autowired
    public CommentController(ICommentService commentService, IStudentDiscussionGroupService groupService) {
        this.commentService = commentService;
        this.groupService = groupService;
    }

    /**
     * Posts a comment as the caller. The author used to come from the request body, so anyone could
     * post under another person's name.
     */
    @PostMapping
    public ResponseEntity<Comment> create(@RequestBody Comment comment, Authentication authentication) {
        if (comment == null || comment.getPost() == null
                || !canRead(comment.getPost().getId(), authentication)) {
            return ResponseEntity.notFound().build();
        }
        Comment created = commentService.create(comment, CurrentCaller.id(authentication));
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /** Reading a thread is public. There is no endpoint that lists comments across all posts. */
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> getByPost(@PathVariable UUID postId, Authentication authentication) {
        if (!canRead(postId, authentication)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(commentService.getByPost(postId));
    }

    @GetMapping("/post/{postId}/top-level")
    public ResponseEntity<List<Comment>> getTopLevelByPost(@PathVariable UUID postId, Authentication authentication) {
        if (!canRead(postId, authentication)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(commentService.getTopLevelByPost(postId));
    }

    @GetMapping("/post/{postId}/replies/{parentId}")
    public ResponseEntity<List<Comment>> getReplies(@PathVariable UUID postId,
                                                     @PathVariable UUID parentId,
                                                     Authentication authentication) {
        if (!canRead(postId, authentication)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(commentService.getReplies(postId, parentId));
    }

    @GetMapping("/post/{postId}/count")
    public ResponseEntity<Long> countByPost(@PathVariable UUID postId, Authentication authentication) {
        if (!canRead(postId, authentication)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(commentService.countByPost(postId));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<Comment>> getByAuthor(@PathVariable UUID authorId, Authentication authentication) {
        return ResponseEntity.ok(commentService.getByAuthor(authorId).stream()
                .filter(comment -> comment.getPost() != null
                        && canRead(comment.getPost().getId(), authentication))
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Comment> read(@PathVariable UUID id, Authentication authentication) {
        Comment comment = commentService.read(id);
        if (comment == null || comment.getPost() == null
                || !canRead(comment.getPost().getId(), authentication)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(comment);
    }

    /** @return 404 unless the caller wrote the comment */
    @PutMapping("/{id}")
    public ResponseEntity<Comment> update(@PathVariable UUID id,
                                          @RequestBody Comment comment,
                                          Authentication authentication) {
        Comment existing = commentService.read(id);
        if (existing == null || existing.getPost() == null
                || !canRead(existing.getPost().getId(), authentication)) {
            return ResponseEntity.notFound().build();
        }
        Comment toUpdate = new Comment.Builder().copy(comment).setId(id).build();
        Comment updated = commentService.update(toUpdate, CurrentCaller.id(authentication));
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /** @return 404 unless the caller wrote the comment */
    @DeleteMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Nothing to return: the change was applied and there is no state left to read.")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        Comment existing = commentService.read(id);
        if (existing == null || existing.getPost() == null
                || !canRead(existing.getPost().getId(), authentication)) {
            return ResponseEntity.notFound().build();
        }
        if (!commentService.delete(id, CurrentCaller.id(authentication))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    private boolean canRead(UUID postId, Authentication authentication) {
        return authentication == null
                ? groupService.canReadPost(postId, null, null)
                : groupService.canReadPost(postId, CurrentCaller.id(authentication), CurrentCaller.role(authentication));
    }
}
