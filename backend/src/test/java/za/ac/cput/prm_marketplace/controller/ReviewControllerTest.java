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
import za.ac.cput.prm_marketplace.domain.Review;
import za.ac.cput.prm_marketplace.service.IReviewService;

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

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IReviewService reviewService;

    private UUID id;
    private Review review;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        review = new Review.Builder()
                .setId(id)
                .setProductId(UUID.randomUUID())
                .setReviewerId(UUID.randomUUID())
                .setRating(4)
                .setComment("Exactly as described.")
                .build();
    }

    @Test
    @DisplayName("create returns 201 with the review")
    void create_returnsCreated() throws Exception {
        when(reviewService.create(any(Review.class))).thenReturn(review);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    @DisplayName("create returns 400 when the service refuses")
    void create_returnsBadRequest() throws Exception {
        when(reviewService.create(any(Review.class))).thenReturn(null);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("read returns the review")
    void read_returnsReview() throws Exception {
        when(reviewService.read(id)).thenReturn(review);

        mockMvc.perform(get("/api/reviews/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("read returns 404 for an unknown review")
    void read_returnsNotFound() throws Exception {
        when(reviewService.read(id)).thenReturn(null);

        mockMvc.perform(get("/api/reviews/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update returns 200 on success")
    void update_returnsOk() throws Exception {
        when(reviewService.update(any(Review.class))).thenReturn(review);

        mockMvc.perform(put("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("update returns 404 when the service cannot update")
    void update_returnsNotFound() throws Exception {
        when(reviewService.update(any(Review.class))).thenReturn(null);

        mockMvc.perform(put("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isNotFound());

        verify(reviewService).update(any());
    }

    @Test
    @DisplayName("delete returns 204 on success")
    void delete_returnsNoContent() throws Exception {
        when(reviewService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/api/reviews/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete returns 404 for an unknown review")
    void delete_returnsNotFound() throws Exception {
        when(reviewService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/api/reviews/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getAll returns every review")
    void getAll_returnsList() throws Exception {
        when(reviewService.getAll()).thenReturn(List.of(review));

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }
}
