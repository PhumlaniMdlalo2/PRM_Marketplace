package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.PostLike;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.IPostLikeService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostLikeController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostLikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IPostLikeService postLikeService;

    private UUID id;
    private UUID postId;
    private UUID userId;
    private PostLike postLike;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        postId = UUID.randomUUID();
        userId = UUID.randomUUID();
        postLike = buildPostLike();
    }

    private User buildUser(UUID userId) {
        return new User.Builder()
                .setId(userId)
                .setName("Reader")
                .setEmail("reader@example.com")
                .setPasswordHash("hash")
                .build();
    }

    private BulletinPost buildPost() {
        return new BulletinPost.Builder()
                .setId(postId)
                .setAuthor(buildUser(UUID.randomUUID()))
                .setTitle("Selling a desk")
                .setBody("Desk in good condition")
                .build();
    }

    private PostLike buildPostLike() {
        return new PostLike.Builder()
                .setId(id)
                .setPost(buildPost())
                .setUser(buildUser(userId))
                .build();
    }

    @Test
    @DisplayName("create returns 201 with the like")
    void create_returnsCreated() throws Exception {
        when(postLikeService.create(any(PostLike.class))).thenReturn(postLike);

        mockMvc.perform(post("/api/post-likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postLike)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("create returns 400 when the service refuses")
    void create_returnsBadRequest() throws Exception {
        when(postLikeService.create(any(PostLike.class))).thenReturn(null);

        mockMvc.perform(post("/api/post-likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postLike)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("read returns the like")
    void read_returnsPostLike() throws Exception {
        when(postLikeService.read(id)).thenReturn(postLike);

        mockMvc.perform(get("/api/post-likes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("read returns 404 for an unknown like")
    void read_returnsNotFound() throws Exception {
        when(postLikeService.read(id)).thenReturn(null);

        mockMvc.perform(get("/api/post-likes/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update returns 200 on success")
    void update_returnsOk() throws Exception {
        when(postLikeService.read(id)).thenReturn(postLike);
        when(postLikeService.update(any(PostLike.class))).thenReturn(postLike);

        mockMvc.perform(put("/api/post-likes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postLike)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("update returns 404 for an unknown like")
    void update_returnsNotFound() throws Exception {
        when(postLikeService.read(id)).thenReturn(null);

        mockMvc.perform(put("/api/post-likes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postLike)))
                .andExpect(status().isNotFound());

        verify(postLikeService, never()).update(any());
    }

    @Test
    @DisplayName("delete returns 204 on success")
    void delete_returnsNoContent() throws Exception {
        when(postLikeService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/api/post-likes/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete returns 404 for an unknown like")
    void delete_returnsNotFound() throws Exception {
        when(postLikeService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/api/post-likes/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getAll returns every like")
    void getAll_returnsList() throws Exception {
        when(postLikeService.getAll()).thenReturn(List.of(postLike));

        mockMvc.perform(get("/api/post-likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("toggle creates a like and returns it")
    void toggle_addsLike() throws Exception {
        when(postLikeService.toggle(postId, userId)).thenReturn(postLike);

        mockMvc.perform(post("/api/post-likes/post/{postId}/user/{userId}/toggle", postId, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("toggle reports 204 when the like is removed")
    void toggle_removesLike() throws Exception {
        when(postLikeService.toggle(postId, userId)).thenReturn(null);

        mockMvc.perform(post("/api/post-likes/post/{postId}/user/{userId}/toggle", postId, userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("hasLiked reports the current state as a boolean")
    void hasLiked_returnsBoolean() throws Exception {
        when(postLikeService.hasLiked(postId, userId)).thenReturn(true);

        mockMvc.perform(get("/api/post-likes/post/{postId}/user/{userId}", postId, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @DisplayName("countByPost returns the like total")
    void countByPost_returnsNumber() throws Exception {
        when(postLikeService.countByPost(postId)).thenReturn(7L);

        mockMvc.perform(get("/api/post-likes/post/{postId}/count", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(7));
    }

    @Test
    @DisplayName("getByUser returns the user's likes")
    void getByUser_returnsList() throws Exception {
        when(postLikeService.getByUser(userId)).thenReturn(List.of(postLike));

        mockMvc.perform(get("/api/post-likes/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }
}
