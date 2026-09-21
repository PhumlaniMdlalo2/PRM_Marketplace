package za.ac.cput.prm_marketplace.controller;

import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.IBulletinPostService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BulletinPostController.class)
class BulletinPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IBulletinPostService bulletinPostService;

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .build();
    }

    private BulletinPost buildPost(UUID id) {
        return new BulletinPost.Builder()
                .setId(id)
                .setAuthor(buildUser())
                .setTitle("Books for sale")
                .setCategory("Textbooks")
                .build();
    }

    @Test
    void createReturnsCreatedWhenServiceSucceeds() throws Exception {
        BulletinPost post = buildPost(null);
        BulletinPost saved = buildPost(UUID.randomUUID());
        when(bulletinPostService.create(any(BulletinPost.class))).thenReturn(saved);

        mockMvc.perform(post("/bulletin-posts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Books for sale"));
    }

    @Test
    void createReturnsBadRequestWhenServiceRejects() throws Exception {
        BulletinPost post = buildPost(null);
        when(bulletinPostService.create(any(BulletinPost.class))).thenReturn(null);

        mockMvc.perform(post("/bulletin-posts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readReturnsPostWhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(bulletinPostService.read(id)).thenReturn(buildPost(id));

        mockMvc.perform(get("/bulletin-posts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Books for sale"));
    }

    @Test
    void readReturnsNotFoundWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(bulletinPostService.read(id)).thenReturn(null);

        mockMvc.perform(get("/bulletin-posts/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturnsOkWhenServiceSucceeds() throws Exception {
        BulletinPost post = buildPost(UUID.randomUUID());
        when(bulletinPostService.update(any(BulletinPost.class))).thenReturn(post);

        mockMvc.perform(put("/bulletin-posts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsNotFoundWhenServiceRejects() throws Exception {
        BulletinPost post = buildPost(UUID.randomUUID());
        when(bulletinPostService.update(any(BulletinPost.class))).thenReturn(null);

        mockMvc.perform(put("/bulletin-posts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturnsNoContentWhenDeleted() throws Exception {
        UUID id = UUID.randomUUID();
        when(bulletinPostService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/bulletin-posts/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturnsNotFoundWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(bulletinPostService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/bulletin-posts/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllReturnsListOfPosts() throws Exception {
        when(bulletinPostService.getAll()).thenReturn(List.of(buildPost(UUID.randomUUID())));

        mockMvc.perform(get("/bulletin-posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Books for sale"));
    }

    @Test
    void findByAuthorIdReturnsPostWhenFound() throws Exception {
        UUID authorId = UUID.randomUUID();
        BulletinPost post = buildPost(UUID.randomUUID());
        when(bulletinPostService.findByAuthorId(authorId)).thenReturn(Optional.of(post));

        mockMvc.perform(get("/bulletin-posts/author/{authorId}", authorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Books for sale"));
    }

    @Test
    void findByAuthorIdReturnsNotFoundWhenMissing() throws Exception {
        UUID authorId = UUID.randomUUID();
        when(bulletinPostService.findByAuthorId(authorId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/bulletin-posts/author/{authorId}", authorId))
                .andExpect(status().isNotFound());
    }
}