package za.ac.cput.prm_marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IBulletinPostService;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/bulletin-posts")
public class BulletinPostController {

    private final IBulletinPostService bulletinPostService;

    @Autowired
    public BulletinPostController(IBulletinPostService bulletinPostService) {
        this.bulletinPostService = bulletinPostService;
    }

    /**
     * The author is the caller. The old endpoint took the author from the request body, so anyone
     * could publish under another person's name.
     */
    @PostMapping
    public ResponseEntity<BulletinPost> create(@RequestBody BulletinPost bulletinPost,
                                               Authentication authentication) {
        BulletinPost created = bulletinPostService.create(bulletinPost, CurrentCaller.id(authentication));
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /** Reading the board is public. */
    @GetMapping
    public ResponseEntity<List<BulletinPost>> getAll() {
        return ResponseEntity.ok(bulletinPostService.getAll());
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<BulletinPost>> getByAuthor(@PathVariable UUID authorId) {
        return ResponseEntity.ok(bulletinPostService.getByAuthor(authorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BulletinPost> read(@PathVariable UUID id) {
        BulletinPost post = bulletinPostService.read(id);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(post);
    }

    /** @return 404 unless the caller wrote the post */
    @PutMapping("/{id}")
    public ResponseEntity<BulletinPost> update(@PathVariable UUID id,
                                               @RequestBody BulletinPost bulletinPost,
                                               Authentication authentication) {
        BulletinPost toUpdate = new BulletinPost.Builder().copy(bulletinPost).setId(id).build();
        BulletinPost updated = bulletinPostService.update(toUpdate, CurrentCaller.id(authentication));
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    /** @return 404 unless the caller wrote the post */
    @DeleteMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Nothing to return: the change was applied and there is no state left to read.")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        if (!bulletinPostService.delete(id, CurrentCaller.id(authentication))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
