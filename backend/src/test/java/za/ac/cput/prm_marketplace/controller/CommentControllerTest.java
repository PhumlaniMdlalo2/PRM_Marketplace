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
import za.ac.cput.prm_marketplace.domain.Comment;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.ICommentService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ICommentService commentService;

    private UUID id;
    private UUID postId;
    private UUID authorId;
    private Comment comment;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        postId = UUID.randomUUID();
        authorId = UUID.randomUUID();
        comment = buildComment();
    }

    private User buildUser(UUID userId) {
        return new User.Builder()
                .setId(userId)
                .setName("Commenter")
                .setEmail("commenter@example.com")
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

    private Comment buildComment() {
        return new Comment.Builder()
                .setId(id)
                .setPost(buildPost())
                .setAuthor(buildUser(authorId))
                .setBody("Is the desk still available?")
                .build();
    }

    @Test
    @DisplayName("create returns 201 with the comment")
    void create_returnsCreated() throws Exception {
        when(commentService.create(any(Comment.class))).thenReturn(comment);

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.body").value("Is the desk still available?"));
    }

    @Test
    @DisplayName("create returns 400 when the service refuses")
    void create_returnsBadRequest() throws Exception {
        when(commentService.create(any(Comment.class))).thenReturn(null);

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create accepts a parent comment in the request body")
    void create_acceptsParent() throws Exception {
        when(commentService.create(any(Comment.class))).thenReturn(comment);

        String body = "{\"id\":\"" + id + "\",\"body\":\"Reply\","
                + "\"parent\":{\"id\":\"" + UUID.randomUUID() + "\",\"body\":\"Root\"}}";

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        org.mockito.ArgumentCaptor<Comment> captor = org.mockito.ArgumentCaptor.forClass(Comment.class);
        verify(commentService).create(captor.capture());
        assertThat(captor.getValue().getParent()).isNotNull();
    }

    @Test
    @DisplayName("the parent comment is not echoed back in the response")
    void create_doesNotEchoParent() throws Exception {
        Comment reply = new Comment.Builder()
                .setId(id)
                .setPost(buildPost())
                .setAuthor(buildUser(authorId))
                .setParent(buildComment())
                .setBody("Reply")
                .build();
        when(commentService.create(any(Comment.class))).thenReturn(reply);

        String body = mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reply)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("\"parent\"");
    }

    @Test
    @DisplayName("read returns the comment")
    void read_returnsComment() throws Exception {
        when(commentService.read(id)).thenReturn(comment);

        mockMvc.perform(get("/api/comments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("read returns 404 for an unknown comment")
    void read_returnsNotFound() throws Exception {
        when(commentService.read(id)).thenReturn(null);

        mockMvc.perform(get("/api/comments/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update returns 200 on success")
    void update_returnsOk() throws Exception {
        when(commentService.read(id)).thenReturn(comment);
        when(commentService.update(any(Comment.class))).thenReturn(comment);

        mockMvc.perform(put("/api/comments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("update returns 404 for an unknown comment")
    void update_returnsNotFound() throws Exception {
        when(commentService.read(id)).thenReturn(null);

        mockMvc.perform(put("/api/comments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isNotFound());

        verify(commentService, never()).update(any());
    }

    @Test
    @DisplayName("delete returns 204 on success")
    void delete_returnsNoContent() throws Exception {
        when(commentService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/api/comments/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete returns 404 for an unknown comment")
    void delete_returnsNotFound() throws Exception {
        when(commentService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/api/comments/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getAll returns every comment")
    void getAll_returnsList() throws Exception {
        when(commentService.getAll()).thenReturn(List.of(comment));

        mockMvc.perform(get("/api/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("getByPost returns the post's comments")
    void getByPost_returnsList() throws Exception {
        when(commentService.getByPost(postId)).thenReturn(List.of(comment));

        mockMvc.perform(get("/api/comments/post/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("getTopLevelByPost returns only root comments")
    void getTopLevelByPost_returnsList() throws Exception {
        when(commentService.getTopLevelByPost(postId)).thenReturn(List.of(comment));

        mockMvc.perform(get("/api/comments/post/{postId}/top-level", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("getReplies returns the replies to a comment")
    void getReplies_returnsList() throws Exception {
        when(commentService.getReplies(postId, id)).thenReturn(List.of(comment));

        mockMvc.perform(get("/api/comments/post/{postId}/replies/{parentId}", postId, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("getByAuthor returns the author's comments")
    void getByAuthor_returnsList() throws Exception {
        when(commentService.getByAuthor(authorId)).thenReturn(List.of(comment));

        mockMvc.perform(get("/api/comments/author/{authorId}", authorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("countByPost returns the comment total")
    void countByPost_returnsNumber() throws Exception {
        when(commentService.countByPost(postId)).thenReturn(11L);

        mockMvc.perform(get("/api/comments/post/{postId}/count", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(11));
    }

    @Test
    @DisplayName("a comment never leaks the author's password hash")
    void comment_doesNotLeakPasswordHash() throws Exception {
        when(commentService.read(id)).thenReturn(comment);

        String body = mockMvc.perform(get("/api/comments/{id}", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("passwordHash").doesNotContain("\"hash\"");
    }
}
