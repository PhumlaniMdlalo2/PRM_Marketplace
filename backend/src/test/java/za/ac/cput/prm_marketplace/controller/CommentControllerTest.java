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
import za.ac.cput.prm_marketplace.domain.Comment;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.ICommentService;
import za.ac.cput.prm_marketplace.service.IStudentDiscussionGroupService;
import za.ac.cput.prm_marketplace.domain.Role;

import java.util.List;
import java.util.Map;
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
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ICommentService commentService;

    @MockitoBean
    private IStudentDiscussionGroupService groupService;

    private UUID authorId;
    private UUID intruderId;
    private UUID postId;
    private UUID commentId;
    private BulletinPost post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        when(groupService.canReadPost(
                org.mockito.ArgumentMatchers.nullable(UUID.class),
                org.mockito.ArgumentMatchers.nullable(UUID.class),
                org.mockito.ArgumentMatchers.nullable(Role.class))).thenReturn(true);
        authorId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        postId = UUID.randomUUID();
        commentId = UUID.randomUUID();
        post = new BulletinPost.Builder()
                .setId(postId)
                .setAuthor(buildUser(intruderId))
                .setTitle("Water outage")
                .setBody("Supply is interrupted.")
                .build();
        comment = new Comment.Builder()
                .setId(commentId)
                .setPost(post)
                .setAuthor(buildUser(authorId))
                .setBody("Any update on this?")
                .build();
    }

    @Test
    @DisplayName("posting a comment files it under the caller, not the author in the body")
    void create_takesTheAuthorFromTheToken() throws Exception {
        when(commentService.create(any(), eq(authorId))).thenReturn(comment);

        // The post and author are included in the JSON request even though the entity serialiser
        // intentionally hides them on output.
        mockMvc.perform(post("/api/comments")
                        .with(asStudent(authorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentRequest(intruderId)))
                .andExpect(status().isCreated());

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentService).create(captor.capture(), eq(authorId));
        assertThat(captor.getValue().getBody()).isEqualTo("Any update on this?");
    }

    @Test
    @DisplayName("posting a comment returns 400 when it cannot be saved")
    void create_returnsBadRequestWhenServiceReturnsNull() throws Exception {
        when(commentService.create(any(), eq(authorId))).thenReturn(null);

        mockMvc.perform(post("/api/comments")
                        .with(asStudent(authorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentRequest(authorId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("the comment list for a post is served from the /api prefix")
    void getByPost_usesTheApiPrefix() throws Exception {
        when(commentService.getByPost(postId)).thenReturn(List.of(comment));

        mockMvc.perform(get("/api/comments/post/" + postId).with(asStudent(authorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(commentId.toString()));

        mockMvc.perform(get("/comments/post/" + postId).with(asStudent(authorId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("group comments are hidden from callers without group access")
    void getByPost_hidesGroupCommentsWithoutMembership() throws Exception {
        when(groupService.canReadPost(postId, authorId, Role.STUDENT)).thenReturn(false);

        mockMvc.perform(get("/api/comments/post/" + postId).with(asStudent(authorId)))
                .andExpect(status().isNotFound());

        verify(commentService, never()).getByPost(postId);
    }

    @Test
    @DisplayName("there is no listing of comments across every post")
    void getAll_isNotAvailable() throws Exception {
        // The path exists for posting, so the router answers 405 rather than 404. Either way the
        // caller cannot reach an unscoped comment list.
        mockMvc.perform(get("/api/comments").with(asStudent(authorId)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("top-level comments and replies are listed separately")
    void threadViews_areAvailable() throws Exception {
        when(commentService.getTopLevelByPost(postId)).thenReturn(List.of(comment));
        when(commentService.getReplies(postId, commentId)).thenReturn(List.of());

        mockMvc.perform(get("/api/comments/post/" + postId + "/top-level").with(asStudent(authorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(commentId.toString()));

        mockMvc.perform(get("/api/comments/post/" + postId + "/replies/" + commentId)
                        .with(asStudent(authorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("the comment count for a post is available")
    void countByPost_returnsTheCount() throws Exception {
        when(commentService.countByPost(postId)).thenReturn(7L);

        mockMvc.perform(get("/api/comments/post/" + postId + "/count").with(asStudent(authorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(7));
    }

    @Test
    @DisplayName("reading a comment that does not exist returns 404")
    void read_returnsNotFoundWhenMissing() throws Exception {
        when(commentService.read(commentId)).thenReturn(null);

        mockMvc.perform(get("/api/comments/" + commentId).with(asStudent(authorId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("updating a comment that is not the caller's returns 404")
    void update_returnsNotFoundForAnotherAuthorsComment() throws Exception {
        when(commentService.read(commentId)).thenReturn(comment);
        when(commentService.update(any(), eq(authorId))).thenReturn(null);

        mockMvc.perform(put("/api/comments/" + commentId)
                        .with(asStudent(authorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isNotFound());

        verify(commentService).update(any(), eq(authorId));
    }

    @Test
    @DisplayName("updating the caller's own comment succeeds")
    void update_returnsUpdatedComment() throws Exception {
        when(commentService.read(commentId)).thenReturn(comment);
        when(commentService.update(any(), eq(authorId))).thenReturn(comment);

        mockMvc.perform(put("/api/comments/" + commentId)
                        .with(asStudent(authorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId.toString()));
    }

    @Test
    @DisplayName("deleting a comment that is not the caller's returns 404")
    void delete_returnsNotFoundForAnotherAuthorsComment() throws Exception {
        when(commentService.read(commentId)).thenReturn(comment);
        when(commentService.delete(commentId, authorId)).thenReturn(false);

        mockMvc.perform(delete("/api/comments/" + commentId).with(asStudent(authorId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deleting the caller's own comment succeeds")
    void delete_returnsNoContent() throws Exception {
        when(commentService.read(commentId)).thenReturn(comment);
        when(commentService.delete(commentId, authorId)).thenReturn(true);

        mockMvc.perform(delete("/api/comments/" + commentId).with(asStudent(authorId)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("comment writes require authentication")
    void writeEndpointsRejectAnonymousCallers() throws Exception {
        String body = commentRequest(authorId);

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/comments/" + commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/comments/" + commentId))
                .andExpect(status().isUnauthorized());

        verify(commentService, never()).delete(any(), any());
    }

    private String commentRequest(UUID claimedAuthorId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "post", Map.of("id", postId),
                "author", Map.of("id", claimedAuthorId),
                "body", comment.getBody()));
    }

    @Test
    @DisplayName("a comment names its author by id and name only, not by email")
    void getByPost_reducesTheAuthorToASummary() throws Exception {
        when(commentService.getByPost(postId)).thenReturn(List.of(comment));

        mockMvc.perform(get("/api/comments/post/" + postId).with(asStudent(authorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].author.id").value(authorId.toString()))
                .andExpect(jsonPath("$[0].author.email").doesNotExist())
                .andExpect(jsonPath("$[0].author.phone").doesNotExist())
                .andExpect(jsonPath("$[0].author.role").doesNotExist());
    }

    @Test
    @DisplayName("a reply reports the id of the comment it answers")
    void getByPost_namesTheParentOfAReply() throws Exception {
        Comment reply = new Comment.Builder()
                .setId(UUID.randomUUID())
                .setPost(post)
                .setAuthor(buildUser(intruderId))
                .setParent(comment)
                .setBody("Same here.")
                .build();
        when(commentService.getByPost(postId)).thenReturn(List.of(comment, reply));

        mockMvc.perform(get("/api/comments/post/" + postId).with(asStudent(authorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].parentId").doesNotExist())
                .andExpect(jsonPath("$[0].reply").value(false))
                .andExpect(jsonPath("$[1].reply").value(true))
                .andExpect(jsonPath("$[1].parentId").value(commentId.toString()))
                // The parent object itself stays off the wire, so a listing cannot nest replies.
                .andExpect(jsonPath("$[1].parent").doesNotExist());
    }

    private User buildUser(UUID id) {
        return new User.Builder().setId(id).setEmail(id + "@example.com").build();
    }
}