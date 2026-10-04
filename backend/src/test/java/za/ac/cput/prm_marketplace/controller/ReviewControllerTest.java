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
import za.ac.cput.prm_marketplace.domain.Review;
import za.ac.cput.prm_marketplace.service.IReviewService;

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
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IReviewService reviewService;

    private UUID reviewerId;
    private UUID intruderId;
    private UUID productId;
    private UUID reviewId;
    private Review review;

    @BeforeEach
    void setUp() {
        reviewerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        productId = UUID.randomUUID();
        reviewId = UUID.randomUUID();
        review = new Review.Builder()
                .setId(reviewId)
                .setProductId(productId)
                .setReviewerId(reviewerId)
                .setRating(4)
                .setComment("Good value")
                .build();
    }

    @Test
    @DisplayName("writing a review attributes it to the caller, not the reviewerId in the body")
    void create_takesTheReviewerFromTheToken() throws Exception {
        when(reviewService.create(any(), eq(reviewerId))).thenReturn(review);

        // The body claims another account. The controller must not forward that id.
        Review hostile = new Review.Builder()
                .copy(review)
                .setReviewerId(intruderId)
                .build();

        mockMvc.perform(post("/api/reviews")
                        .with(asStudent(reviewerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hostile)))
                .andExpect(status().isCreated());

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewService).create(captor.capture(), eq(reviewerId));
        assertThat(captor.getValue().getProductId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("writing a review returns 400 when it is rejected")
    void create_returnsBadRequestWhenServiceReturnsNull() throws Exception {
        when(reviewService.create(any(), eq(reviewerId))).thenReturn(null);

        mockMvc.perform(post("/api/reviews")
                        .with(asStudent(reviewerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("reading a review that does not exist returns 404")
    void read_returnsNotFoundWhenMissing() throws Exception {
        when(reviewService.read(reviewId)).thenReturn(null);

        mockMvc.perform(get("/api/reviews/" + reviewId).with(asStudent(reviewerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("reading a review returns it")
    void read_returnsTheReview() throws Exception {
        when(reviewService.read(reviewId)).thenReturn(review);

        mockMvc.perform(get("/api/reviews/" + reviewId).with(asStudent(reviewerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId.toString()));
    }

    @Test
    @DisplayName("updating a review that is not the caller's returns 404")
    void update_returnsNotFoundForAnotherReviewersReview() throws Exception {
        when(reviewService.update(any(), eq(reviewerId))).thenReturn(null);

        mockMvc.perform(put("/api/reviews")
                        .with(asStudent(reviewerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isNotFound());

        verify(reviewService).update(any(), eq(reviewerId));
    }

    @Test
    @DisplayName("updating the caller's own review succeeds")
    void update_returnsUpdatedReview() throws Exception {
        when(reviewService.update(any(), eq(reviewerId))).thenReturn(review);

        mockMvc.perform(put("/api/reviews")
                        .with(asStudent(reviewerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId.toString()));
    }

    @Test
    @DisplayName("deleting a review that is not the caller's returns 404")
    void delete_returnsNotFoundForAnotherReviewersReview() throws Exception {
        when(reviewService.delete(reviewId, reviewerId)).thenReturn(false);

        mockMvc.perform(delete("/api/reviews/" + reviewId).with(asStudent(reviewerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deleting the caller's own review succeeds")
    void delete_returnsNoContent() throws Exception {
        when(reviewService.delete(reviewId, reviewerId)).thenReturn(true);

        mockMvc.perform(delete("/api/reviews/" + reviewId).with(asStudent(reviewerId)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("reviews can be listed for a product or for an author")
    void listings_areAvailable() throws Exception {
        when(reviewService.getAll()).thenReturn(List.of(review));
        when(reviewService.getByProduct(productId)).thenReturn(List.of(review));
        when(reviewService.getByReviewer(reviewerId)).thenReturn(List.of(review));

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reviewId.toString()));
        mockMvc.perform(get("/api/reviews/product/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reviewId.toString()));
        mockMvc.perform(get("/api/reviews/reviewer/" + reviewerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reviewId.toString()));
    }

    @Test
    @DisplayName("review writes require authentication")
    void writeEndpointsRejectAnonymousCallers() throws Exception {
        String body = objectMapper.writeValueAsString(review);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/reviews/" + reviewId))
                .andExpect(status().isUnauthorized());

        verify(reviewService, never()).delete(any(), any());
    }
}