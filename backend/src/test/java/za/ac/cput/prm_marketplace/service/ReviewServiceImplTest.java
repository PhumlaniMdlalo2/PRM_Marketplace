package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Review;
import za.ac.cput.prm_marketplace.repository.ReviewRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    private ReviewServiceImpl service;

    private UUID id;
    private Review review;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(reviewRepository);
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
    @DisplayName("create returns null for a null review")
    void create_nullReturnsNull() {
        assertThat(service.create(null)).isNull();

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("create delegates to the repository")
    void create_savesReview() {
        when(reviewRepository.save(review)).thenReturn(review);

        assertThat(service.create(review)).isSameAs(review);

        verify(reviewRepository).save(review);
    }

    @Test
    @DisplayName("read returns null for a null id")
    void read_nullIdReturnsNull() {
        assertThat(service.read(null)).isNull();

        verify(reviewRepository, never()).findById(any());
    }

    @Test
    @DisplayName("read returns the stored review")
    void read_returnsReview() {
        when(reviewRepository.findById(id)).thenReturn(Optional.of(review));

        assertThat(service.read(id)).isSameAs(review);
    }

    @Test
    @DisplayName("read returns null when the review is absent")
    void read_missingReturnsNull() {
        when(reviewRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(service.read(id)).isNull();
    }

    @Test
    @DisplayName("update returns null for a null review")
    void update_nullReturnsNull() {
        assertThat(service.update(null)).isNull();

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("update returns null when the review has no id")
    void update_nullIdReturnsNull() {
        Review noId = new Review.Builder()
                .setProductId(UUID.randomUUID())
                .setReviewerId(UUID.randomUUID())
                .setRating(3)
                .build();

        assertThat(service.update(noId)).isNull();

        verify(reviewRepository, never()).existsById(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("update returns null when the review does not exist")
    void update_missingReturnsNull() {
        when(reviewRepository.existsById(id)).thenReturn(false);

        assertThat(service.update(review)).isNull();

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("update saves an existing review")
    void update_savesReview() {
        when(reviewRepository.existsById(id)).thenReturn(true);
        when(reviewRepository.save(review)).thenReturn(review);

        assertThat(service.update(review)).isSameAs(review);

        verify(reviewRepository).save(review);
    }

    @Test
    @DisplayName("delete returns false for a null id")
    void delete_nullIdReturnsFalse() {
        assertThat(service.delete(null)).isFalse();

        verify(reviewRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete returns false when the review does not exist")
    void delete_missingReturnsFalse() {
        when(reviewRepository.existsById(id)).thenReturn(false);

        assertThat(service.delete(id)).isFalse();

        verify(reviewRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete removes an existing review")
    void delete_removesReview() {
        when(reviewRepository.existsById(id)).thenReturn(true);

        assertThat(service.delete(id)).isTrue();

        verify(reviewRepository).deleteById(id);
    }

    @Test
    @DisplayName("getAll returns every review")
    void getAll_returnsList() {
        when(reviewRepository.findAll()).thenReturn(List.of(review));

        assertThat(service.getAll()).containsExactly(review);
    }
}
