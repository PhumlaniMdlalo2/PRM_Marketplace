package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.PostLike;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.IPostLikeService;
import za.ac.cput.prm_marketplace.service.IStudentDiscussionGroupService;
import za.ac.cput.prm_marketplace.domain.Role;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.asStudent;

@SpringBootTest
@AutoConfigureMockMvc
class PostLikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IPostLikeService postLikeService;

    @MockitoBean
    private IStudentDiscussionGroupService groupService;

    private UUID callerId;
    private UUID postId;
    private UUID likeId;
    private BulletinPost post;
    private PostLike like;

    @BeforeEach
    void setUp() {
        when(groupService.canReadPost(
                org.mockito.ArgumentMatchers.nullable(UUID.class),
                org.mockito.ArgumentMatchers.nullable(UUID.class),
                org.mockito.ArgumentMatchers.nullable(Role.class))).thenReturn(true);
        callerId = UUID.randomUUID();
        postId = UUID.randomUUID();
        likeId = UUID.randomUUID();
        post = new BulletinPost.Builder()
                .setId(postId)
                .setAuthor(new User.Builder().setId(UUID.randomUUID()).setEmail("a@example.com").build())
                .setTitle("Water outage")
                .setBody("Supply is interrupted.")
                .build();
        like = new PostLike.Builder()
                .setId(likeId)
                .setPost(post)
                .setUser(new User.Builder().setId(callerId).setEmail("c@example.com").build())
                .build();
    }

    @Test
    @DisplayName("toggling a like uses the caller from the token, not a userId in the path")
    void toggle_takesTheLikerFromTheToken() throws Exception {
        when(postLikeService.toggle(postId, callerId)).thenReturn(like);

        mockMvc.perform(post("/api/post-likes/post/" + postId + "/toggle").with(asStudent(callerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(likeId.toString()));

        verify(postLikeService).toggle(postId, callerId);
    }

    @Test
    @DisplayName("the route that let anyone like on behalf of another account is gone")
    void toggle_theUserScopedRouteIsGone() throws Exception {
        UUID intruderId = UUID.randomUUID();

        mockMvc.perform(post("/api/post-likes/toggle/" + postId + "/user/" + intruderId)
                        .with(asStudent(callerId)))
                .andExpect(status().isNotFound());

        verify(postLikeService, never()).toggle(any(), any());
    }

    @Test
    @DisplayName("creating a like from a whole body is no longer possible")
    void create_bodyEndpointIsGone() throws Exception {
        // The path is bound for the toggle route but no longer accepts a bare POST, so the router
        // answers 405 rather than 404.
        mockMvc.perform(post("/api/post-likes").with(asStudent(callerId)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("toggling off returns 204 rather than an error")
    void toggle_returnsNoContentWhenTheLikeWasRemoved() throws Exception {
        // The service returns null both when it removes a like and when the post is missing.
        when(postLikeService.toggle(postId, callerId)).thenReturn(null);

        mockMvc.perform(post("/api/post-likes/post/" + postId + "/toggle").with(asStudent(callerId)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("group like counts are hidden from callers without group access")
    void count_hidesGroupLikesWithoutMembership() throws Exception {
        when(groupService.canReadPost(postId, callerId, Role.STUDENT)).thenReturn(false);

        mockMvc.perform(get("/api/post-likes/post/" + postId + "/count").with(asStudent(callerId)))
                .andExpect(status().isNotFound());

        verify(postLikeService, never()).countByPost(postId);
    }

    @Test
    @DisplayName("the has-liked check is scoped to the caller")
    void hasLiked_isScopedToTheCaller() throws Exception {
        when(postLikeService.hasLiked(postId, callerId)).thenReturn(true);

        mockMvc.perform(get("/api/post-likes/post/" + postId).with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(postLikeService).hasLiked(postId, callerId);
    }

    @Test
    @DisplayName("the like count for a post is readable by any signed-in user")
    void countByPost_isReadableByAnyCaller() throws Exception {
        when(postLikeService.countByPost(postId)).thenReturn(12L);

        mockMvc.perform(get("/api/post-likes/post/" + postId + "/count").with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(12));
    }

    @Test
    @DisplayName("the like list is the caller's own, not every like in the system")
    void getByUser_isScopedToTheCaller() throws Exception {
        when(postLikeService.getByUser(callerId)).thenReturn(List.of(like));

        mockMvc.perform(get("/api/post-likes").with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(likeId.toString()));

        verify(postLikeService).getByUser(callerId);
    }

    @Test
    @DisplayName("liking requires authentication")
    void toggle_rejectsAnonymousCallers() throws Exception {
        mockMvc.perform(post("/api/post-likes/post/" + postId + "/toggle"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/post-likes"))
                .andExpect(status().isUnauthorized());

        verify(postLikeService, never()).toggle(any(), any());
        verify(postLikeService, never()).getByUser(eq(callerId));
    }
}