package za.ac.cput.prm_marketplace.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Comment;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.BulletinPostRepository;
import za.ac.cput.prm_marketplace.repository.CommentRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;
import za.ac.cput.prm_marketplace.service.IReportService;
import za.ac.cput.prm_marketplace.service.IReviewService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.asStudent;

/**
 * Drives the bulletin board through real JSON, the real services and a real database.
 *
 * <p>The controller tests mock the service layer, so they cannot tell whether a body can actually
 * reach the fields the service reads. That gap is not theoretical: marking {@code Comment.post}
 * read-only made every comment creation fail, because the client has to name the post it is
 * commenting on, yet no mocked test noticed. These tests close that gap by posting literal JSON.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BulletinJsonBindingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BulletinPostRepository bulletinPostRepository;

    @Autowired
    private CommentRepository commentRepository;

    /**
     * These services are replaced only because the surrounding slice seeds no catalogue or vendor
     * rows; nothing in these tests touches them.
     */
    @MockitoBean
    private IReviewService reviewService;

    @MockitoBean
    private IReportService reportService;

    private User caller;
    private BulletinPost post;

    @BeforeEach
    void setUp() {
        caller = userRepository.save(new User.Builder()
                .setName("caller")
                .setEmail("caller-" + UUID.randomUUID() + "@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .setVerified(true)
                .build());
        post = bulletinPostRepository.save(new BulletinPost.Builder()
                .setAuthor(caller)
                .setTitle("Water outage in Block C")
                .setBody("Supply is interrupted until Friday.")
                .setCategory("Maintenance")
                .build());
    }

    @Test
    @DisplayName("a post created from literal JSON keeps its text and takes the author from the token")
    void createPost_fromLiteralJson() throws Exception {
        String intruder = userRepository.save(new User.Builder()
                .setName("intruder")
                .setEmail("intruder-" + UUID.randomUUID() + "@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .setVerified(true)
                .build()).getId().toString();

        // The body names another author, supplies an id, and tries to preset the counters.
        String body = """
                {
                  "id": "%s",
                  "author": { "id": "%s", "email": "intruder@example.com" },
                  "title": "Lift out of order",
                  "body": "The lift in A Block is stuck.",
                  "category": "Maintenance",
                  "commentCount": 42,
                  "likeCount": 99
                }
                """.formatted(UUID.randomUUID(), intruder);

        mockMvc.perform(post("/api/bulletin-posts")
                        .with(asStudent(caller.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Lift out of order"))
                // Server-owned fields are ignored on the way in.
                .andExpect(jsonPath("$.commentCount").value(0))
                .andExpect(jsonPath("$.likeCount").value(0));

        BulletinPost stored = bulletinPostRepository.findAll().stream()
                .filter(p -> "Lift out of order".equals(p.getTitle()))
                .findFirst()
                .orElseThrow();
        assertThat(stored.getAuthor().getId()).isEqualTo(caller.getId());
        assertThat(stored.getCommentCount()).isZero();
        assertThat(stored.getLikeCount()).isZero();
    }

    @Test
    @DisplayName("a comment created from literal JSON reaches the post the client named")
    void createComment_fromLiteralJson() throws Exception {
        // This is the case the read-only annotation broke: the body must be able to say which post
        // it is commenting on.
        String body = """
                {
                  "post": { "id": "%s" },
                  "body": "Any update on this?"
                }
                """.formatted(post.getId());

        mockMvc.perform(post("/api/comments")
                        .with(asStudent(caller.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Any update on this?"));

        Comment stored = commentRepository.findAll().stream()
                .filter(c -> "Any update on this?".equals(c.getBody()))
                .findFirst()
                .orElseThrow();
        assertThat(stored.getPost().getId()).isEqualTo(post.getId());
        assertThat(stored.getAuthor().getId()).isEqualTo(caller.getId());
        // The reply tree and the post itself stay out of the response body.
        assertThat(bulletinPostRepository.findById(post.getId()).orElseThrow().getCommentCount())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("a reply created from literal JSON reaches the parent comment it named")
    void createReply_fromLiteralJson() throws Exception {
        Comment parent = commentRepository.save(new Comment.Builder()
                .setPost(post)
                .setAuthor(caller)
                .setBody("Is Friday confirmed?")
                .build());

        String body = """
                {
                  "post": { "id": "%s" },
                  "parent": { "id": "%s" },
                  "body": "Yes, per the notice."
                }
                """.formatted(post.getId(), parent.getId());

        mockMvc.perform(post("/api/comments")
                        .with(asStudent(caller.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        Comment reply = commentRepository.findAll().stream()
                .filter(c -> "Yes, per the notice.".equals(c.getBody()))
                .findFirst()
                .orElseThrow();
        assertThat(reply.getParent().getId()).isEqualTo(parent.getId());
        assertThat(reply.getPost().getId()).isEqualTo(post.getId());
    }

    @Test
    @DisplayName("a comment aimed at a post that does not exist is refused")
    void createComment_againstAMissingPostIsRefused() throws Exception {
        String body = """
                {
                  "post": { "id": "%s" },
                  "body": "Any update on this?"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/comments")
                        .with(asStudent(caller.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                        .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a reply wired to another post's comment is refused")
    void createReply_toAnotherPostsCommentIsRefused() throws Exception {
        BulletinPost otherPost = bulletinPostRepository.save(new BulletinPost.Builder()
                .setAuthor(caller)
                .setTitle("Another thread")
                .setBody("Unrelated.")
                .build());
        Comment foreignParent = commentRepository.save(new Comment.Builder()
                .setPost(otherPost)
                .setAuthor(caller)
                .setBody("Root of the other thread")
                .build());

        String body = """
                {
                  "post": { "id": "%s" },
                  "parent": { "id": "%s" },
                  "body": "Agreed."
                }
                """.formatted(post.getId(), foreignParent.getId());

        mockMvc.perform(post("/api/comments")
                        .with(asStudent(caller.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}