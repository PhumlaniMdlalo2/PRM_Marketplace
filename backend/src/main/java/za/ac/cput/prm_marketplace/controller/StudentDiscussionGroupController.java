package za.ac.cput.prm_marketplace.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.dto.StudentDiscussionGroupRequest;
import za.ac.cput.prm_marketplace.dto.StudentDiscussionGroupResponse;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IStudentDiscussionGroupService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/student-groups")
public class StudentDiscussionGroupController {

    private final IStudentDiscussionGroupService groupService;

    public StudentDiscussionGroupController(IStudentDiscussionGroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ResponseEntity<List<StudentDiscussionGroupResponse>> list(Authentication authentication) {
        if (!isStudent(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(groupService.listForStudent(
                CurrentCaller.id(authentication), CurrentCaller.role(authentication)));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody StudentDiscussionGroupRequest request,
                                    Authentication authentication) {
        if (!isStudent(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        StudentDiscussionGroupResponse created = groupService.create(request,
                CurrentCaller.id(authentication), CurrentCaller.role(authentication));
        if (created == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Add a campus to your student profile, or choose a group name not already used there."));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{groupId}/membership")
    public ResponseEntity<Void> join(@PathVariable UUID groupId, Authentication authentication) {
        if (!isStudent(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return groupService.join(groupId, CurrentCaller.id(authentication), CurrentCaller.role(authentication))
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{groupId}/membership")
    public ResponseEntity<Void> leave(@PathVariable UUID groupId, Authentication authentication) {
        if (!isStudent(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return groupService.leave(groupId, CurrentCaller.id(authentication), CurrentCaller.role(authentication))
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{groupId}/posts")
    public ResponseEntity<List<BulletinPost>> posts(@PathVariable UUID groupId,
                                                     Authentication authentication) {
        if (!isStudent(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (!groupService.canAccessGroup(groupId, CurrentCaller.id(authentication),
                CurrentCaller.role(authentication))) {
            return ResponseEntity.notFound().build();
        }
        List<BulletinPost> posts = groupService.listPosts(groupId, CurrentCaller.id(authentication),
                CurrentCaller.role(authentication));
        return ResponseEntity.ok(posts);
    }

    @PostMapping("/{groupId}/posts")
    public ResponseEntity<BulletinPost> createPost(@PathVariable UUID groupId,
                                                   @RequestBody BulletinPost post,
                                                   Authentication authentication) {
        if (!isStudent(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        BulletinPost created = groupService.createPost(groupId, post, CurrentCaller.id(authentication),
                CurrentCaller.role(authentication));
        return created == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private boolean isStudent(Authentication authentication) {
        return authentication != null && CurrentCaller.role(authentication) == Role.STUDENT;
    }
}
