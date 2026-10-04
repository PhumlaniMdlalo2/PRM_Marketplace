package za.ac.cput.prm_marketplace.controller;

import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.dto.UpdateProfileRequest;
import za.ac.cput.prm_marketplace.dto.UserResponse;
import za.ac.cput.prm_marketplace.exception.ForbiddenException;
import za.ac.cput.prm_marketplace.mapper.UserMapper;
import za.ac.cput.prm_marketplace.security.CurrentCaller;
import za.ac.cput.prm_marketplace.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Account reads and the caller's own profile edit.
 *
 * <p>Mounted at {@code /api/users} so it sits alongside every other controller. It used to be the
 * lone {@code /users} route, which meant it also sat outside the {@code /api/**} group that
 * {@code SecurityConfig} reasons about.
 *
 * <p>The two write endpoints this used to expose — {@code POST /users} and {@code PUT /users} —
 * are gone. Both took a {@code User} entity from the request body and both are privilege
 * escalations: {@code PUT} in particular took the target account from {@code user.getId()} in the
 * body and saved every field of it, so any authenticated caller could set another account's
 * {@code role} to faculty or write a plaintext password into the column. Nothing in the frontend
 * called them. Accounts are created through {@code POST /api/auth/register} and passwords are
 * changed through {@code PUT /api/auth/change-password}, both of which apply their own rules.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    /**
     * The signed-in account, taken from the token.
     *
     * <p>This is what the profile page reads. It previously had to reconstruct the account from
     * whatever the login response happened to contain, because no endpoint returned the current
     * user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication caller) {
        User user = userService.read(CurrentCaller.id(caller));
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }

    /**
     * Edits the signed-in account. The target is the token's subject, never a request field, and
     * {@link UpdateProfileRequest} has no role, password or id for a caller to aim elsewhere with.
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(Authentication caller,
                                                 @Valid @RequestBody UpdateProfileRequest request) {
        User updated = userService.updateProfile(
                CurrentCaller.id(caller), request.name(), request.phone(), request.avatarUrl());
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(UserMapper.toResponse(updated));
    }

    /**
     * A single account by id, for pages that show someone else's name and avatar alongside their
     * listings or posts. Any signed-in user may read one; the password hash is never in the
     * response.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> read(@PathVariable UUID id) {
        User user = userService.read(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }

    /**
     * Every account. Faculty-only: it is a moderation view, and open to any signed-in user it hands
     * over every member's email address and phone number in one request.
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll(Authentication caller) {
        requireFaculty(caller);
        return ResponseEntity.ok(UserMapper.toResponseList(userService.getAll()));
    }

    /**
     * Lookup by email. Faculty-only, for the same reason as {@link #getAll} — unrestricted, it is a
     * membership oracle that confirms whether a given address has an account here.
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> findByEmail(Authentication caller, @PathVariable String email) {
        requireFaculty(caller);
        return userService.findByEmail(email)
                .map(UserMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Account deletion, for self-service account closure. Faculty may delete any account.
     *
     * <p>Previously any signed-in user could delete any account by id.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication caller, @PathVariable UUID id) {
        UUID callerId = CurrentCaller.id(caller);
        if (!callerId.equals(id) && CurrentCaller.role(caller) != Role.FACULTY) {
            throw new ForbiddenException("You may only delete your own account");
        }
        if (!userService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    private void requireFaculty(Authentication caller) {
        if (CurrentCaller.role(caller) != Role.FACULTY) {
            throw new ForbiddenException("Faculty access is required");
        }
    }
}