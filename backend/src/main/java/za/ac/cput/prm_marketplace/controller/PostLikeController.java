package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.PostLike;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IPostLikeService;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/post-likes")
public class PostLikeController {

    private final IPostLikeService postLikeService;

    @Autowired
    public PostLikeController(IPostLikeService postLikeService) {
        this.postLikeService = postLikeService;
    }

    /**
     * Likes or unlikes as the caller. The former route took a userId in the path, so anyone could
     * like a post on behalf of any other account, and "POST /api/post-likes" accepted a whole body
     * naming whoever they liked as.
     */
    @PostMapping("/post/{postId}/toggle")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The like was created and the full row is returned."),
        @ApiResponse(responseCode = "204", description = "Nothing to return: the change was applied and there is no state left to read.")
    })
    public ResponseEntity<PostLike> toggle(@PathVariable UUID postId, Authentication authentication) {
        PostLike result = postLikeService.toggle(postId, CurrentCaller.id(authentication));
        // A null result means the caller's like was removed, which is a 204 rather than an error.
        return result == null
                ? ResponseEntity.noContent().build()
                : new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    /** Whether the caller liked the post. */
    @GetMapping("/post/{postId}")
    public ResponseEntity<Boolean> hasLiked(@PathVariable UUID postId, Authentication authentication) {
        return ResponseEntity.ok(postLikeService.hasLiked(postId, CurrentCaller.id(authentication)));
    }

    @GetMapping("/post/{postId}/count")
    public ResponseEntity<Long> countByPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(postLikeService.countByPost(postId));
    }

    /** The posts the caller has liked. */
    @GetMapping
    public ResponseEntity<List<PostLike>> getByUser(Authentication authentication) {
        return ResponseEntity.ok(postLikeService.getByUser(CurrentCaller.id(authentication)));
    }
}
