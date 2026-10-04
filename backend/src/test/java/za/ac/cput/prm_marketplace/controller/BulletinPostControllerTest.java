package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.IBulletinPostService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.asStudent;

@SpringBootTest
@AutoConfigureMockMvc
class BulletinPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IBulletinPostService bulletinPostService;

    private UUID authorId;
    private UUID intruderId;
    private UUID postId;
    private BulletinPost post;

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        postId = UUID.randomUUID();
        post = buildPost(authorId);
    }

    @Test
    @DisplayName("creating a post files it under the caller, not the author named in the body")
    void create_takesTheAuthorFromTheToken() throws Exception {
        when(bulletinPostService.create(any(), eq(authorId))).thenReturn(post);

        // The body claims a different author. The controller must not forward that.
        BulletinPost hostile = new BulletinPost.Builder()
                .copy(post)
                .setAuthor(buildUser(intruderId))
                .build();

        mockMvc.perform(post("/api/bulletin-posts")
                        .with(asStudent(authorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hostile)))
                .andExpect(status().isCreated());

        ArgumentCaptor<BulletinPost> captor = ArgumentCaptor.forClass(BulletinPost.class);
        verify(bulletinPostService).create(captor.capture(), eq(authorId));
        assertThat(captor.getValue().getTitle()).isEqualTo(post.getTitle());
    }

    @Test
    @DisplayName("creating a post returns 400 when it cannot be saved")
    void create_returnsBadRequestWhenServiceReturnsNull() throws Exception {
        when(bulletinPostService.create(any(), eq(authorId))).thenReturn(null);

        mockMvc.perform(post("/api/bulletin-posts")
                        .with(asStudent(authorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("posts are served from the /api prefix, not the old /bulletin-posts")
    void read_usesTheApiPrefix() throws Exception {
        when(bulletinPostService.read(postId)).thenReturn(post);

        mockMvc.perform(get("/api/bulletin-posts/" + postId).with(asStudent(authorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId.toString()));

        mockMvc.perform(get("/bulletin-posts/" + postId).with(asStudent(authorId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("reading a post that does not exist returns 404")
    void read_returnsNotFoundWhenMissing() throws Exception {
        when(bulletinPostService.read(postId)).thenReturn(null);

        mockMvc.perform(get("/api/bulletin-posts/" + postId).with(asStudent(authorId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the post list is readable by any signed-in user")
    void getAll_isReadableByAnyCaller() throws Exception {
        when(bulletinPostService.getAll()).thenReturn(List.of(post));

        mockMvc.perform(get("/api/bulletin-posts").with(asStudent(authorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(postId.toString()));
    }

    @Test
    @DisplayName("posts can be listed per author")
    void getByAuthor_returnsTheAuthorsPosts() throws Exception {
        when(bulletinPostService.getByAuthor(authorId)).thenReturn(List.of(post));

        mockMvc.perform(get("/api/bulletin-posts/author/" + authorId).with(asStudent(authorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(postId.toString()));
    }

    @Test
    @DisplayName("updating a post that is not the caller's returns 404")
    void update_returnsNotFoundForAnotherAuthorsPost() throws Exception {
        when(bulletinPostService.update(any(), eq(authorId))).thenReturn(null);

        mockMvc.perform(put("/api/bulletin-posts/" + postId)
                        .with(asStudent(authorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isNotFound());

        verify(bulletinPostService).update(any(), eq(authorId));
    }

    @Test
    @DisplayName("updating the caller's own post succeeds")
    void update_returnsUpdatedPost() throws Exception {
        when(bulletinPostService.update(any(), eq(authorId))).thenReturn(post);

        mockMvc.perform(put("/api/bulletin-posts/" + postId)
                        .with(asStudent(authorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId.toString()));
    }

    @Test
    @DisplayName("deleting a post that is not the caller's returns 404")
    void delete_returnsNotFoundForAnotherAuthorsPost() throws Exception {
        when(bulletinPostService.delete(postId, authorId)).thenReturn(false);

        mockMvc.perform(delete("/api/bulletin-posts/" + postId).with(asStudent(authorId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deleting the caller's own post succeeds")
    void delete_returnsNoContent() throws Exception {
        when(bulletinPostService.delete(postId, authorId)).thenReturn(true);

        mockMvc.perform(delete("/api/bulletin-posts/" + postId).with(asStudent(authorId)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("post writes require authentication, reads do not")
    void writeEndpointsRejectAnonymousCallers() throws Exception {
        String body = objectMapper.writeValueAsString(post);

        mockMvc.perform(post("/api/bulletin-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/bulletin-posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/bulletin-posts/" + postId))
                .andExpect(status().isUnauthorized());

        verify(bulletinPostService, never()).delete(any(), any());
    }

    @Test
    @DisplayName("bulletin reads are genuinely reachable without a token")
    void readEndpointsAllowAnonymousCallers() throws Exception {
        // The test above asserts writes are refused; this asserts the other half of the promise,
        // which nothing was actually checking. A security rule that covers GETs by accident — a
        // matcher narrowed to one path, or a future change that adds
        // "/api/bulletin-posts/**" to the authenticated list — would have gone unnoticed, and the
        // bulletin board is public by design: it is the part of the app meant to be readable
        // before anyone has signed in.
        when(bulletinPostService.getAll()).thenReturn(List.of(post));
        when(bulletinPostService.read(postId)).thenReturn(post);
        when(bulletinPostService.getByAuthor(authorId)).thenReturn(List.of(post));

        mockMvc.perform(get("/api/bulletin-posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(postId.toString()));
        mockMvc.perform(get("/api/bulletin-posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId.toString()));
        mockMvc.perform(get("/api/bulletin-posts/author/{authorId}", authorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(postId.toString()));
    }

    @Test
    @DisplayName("an anonymous read of a post that does not exist is a 404, not a 401")
    void readOfMissingPostIsNotAnAuthProblem() throws Exception {
        // Proves the request reached the controller and the service rather than being turned away
        // at the security layer. A 401 here would mean anonymous browsing had silently stopped
        // working while every other bulletin test still passed.
        when(bulletinPostService.read(any())).thenReturn(null);

        mockMvc.perform(get("/api/bulletin-posts/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a post names its author by id and name only, not by email")
    void read_reducesTheAuthorToASummary() throws Exception {
        when(bulletinPostService.read(postId)).thenReturn(post);

        mockMvc.perform(get("/api/bulletin-posts/" + postId).with(asStudent(authorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author.id").value(authorId.toString()))
                .andExpect(jsonPath("$.author.email").doesNotExist())
                .andExpect(jsonPath("$.author.phone").doesNotExist())
                .andExpect(jsonPath("$.author.role").doesNotExist());
    }

    private User buildUser(UUID id) {
        return new User.Builder().setId(id).setEmail(id + "@example.com").build();
    }

    private BulletinPost buildPost(UUID author) {
        return new BulletinPost.Builder()
                .setId(postId)
                .setAuthor(buildUser(author))
                .setTitle("Water outage in Block C")
                .setBody("Supply is interrupted until Friday.")
                .setCategory("Maintenance")
                .build();
    }
}