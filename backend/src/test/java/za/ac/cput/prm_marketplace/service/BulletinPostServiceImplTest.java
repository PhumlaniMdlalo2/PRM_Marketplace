package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.BulletinPostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulletinPostServiceImplTest {

    @Mock
    private BulletinPostRepository bulletinPostRepository;

    @InjectMocks
    private BulletinPostServiceImpl bulletinPostService;

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
    void createReturnsNullWhenPostIsNull() {
        BulletinPost result = bulletinPostService.create(null);

        assertThat(result).isNull();
        verifyNoInteractions(bulletinPostRepository);
    }

    @Test
    void createSavesAndReturnsPost() {
        BulletinPost post = buildPost(null);
        BulletinPost saved = buildPost(UUID.randomUUID());
        when(bulletinPostRepository.save(post)).thenReturn(saved);

        BulletinPost result = bulletinPostService.create(post);

        assertThat(result).isEqualTo(saved);
        verify(bulletinPostRepository).save(post);
    }

    @Test
    void readReturnsNullWhenIdIsNull() {
        BulletinPost result = bulletinPostService.read(null);

        assertThat(result).isNull();
        verifyNoInteractions(bulletinPostRepository);
    }

    @Test
    void readReturnsPostWhenFound() {
        UUID id = UUID.randomUUID();
        BulletinPost post = buildPost(id);
        when(bulletinPostRepository.findById(id)).thenReturn(Optional.of(post));

        BulletinPost result = bulletinPostService.read(id);

        assertThat(result).isEqualTo(post);
    }

    @Test
    void readReturnsNullWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(bulletinPostRepository.findById(id)).thenReturn(Optional.empty());

        BulletinPost result = bulletinPostService.read(id);

        assertThat(result).isNull();
    }

    @Test
    void updateReturnsNullWhenPostIsNull() {
        BulletinPost result = bulletinPostService.update(null);

        assertThat(result).isNull();
        verifyNoInteractions(bulletinPostRepository);
    }

    @Test
    void updateReturnsNullWhenIdIsNull() {
        BulletinPost post = buildPost(null);

        BulletinPost result = bulletinPostService.update(post);

        assertThat(result).isNull();
        verifyNoInteractions(bulletinPostRepository);
    }

    @Test
    void updateReturnsNullWhenPostDoesNotExist() {
        UUID id = UUID.randomUUID();
        BulletinPost post = buildPost(id);
        when(bulletinPostRepository.existsById(id)).thenReturn(false);

        BulletinPost result = bulletinPostService.update(post);

        assertThat(result).isNull();
        verify(bulletinPostRepository, never()).save(any());
    }

    @Test
    void updateSavesAndReturnsPostWhenExists() {
        UUID id = UUID.randomUUID();
        BulletinPost post = buildPost(id);
        when(bulletinPostRepository.existsById(id)).thenReturn(true);
        when(bulletinPostRepository.save(post)).thenReturn(post);

        BulletinPost result = bulletinPostService.update(post);

        assertThat(result).isEqualTo(post);
        verify(bulletinPostRepository).save(post);
    }

    @Test
    void deleteReturnsFalseWhenIdIsNull() {
        boolean result = bulletinPostService.delete(null);

        assertThat(result).isFalse();
        verifyNoInteractions(bulletinPostRepository);
    }

    @Test
    void deleteReturnsFalseWhenPostDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(bulletinPostRepository.existsById(id)).thenReturn(false);

        boolean result = bulletinPostService.delete(id);

        assertThat(result).isFalse();
        verify(bulletinPostRepository, never()).deleteById(any());
    }

    @Test
    void deleteReturnsTrueAndDeletesWhenExists() {
        UUID id = UUID.randomUUID();
        when(bulletinPostRepository.existsById(id)).thenReturn(true);

        boolean result = bulletinPostService.delete(id);

        assertThat(result).isTrue();
        verify(bulletinPostRepository).deleteById(id);
    }

    @Test
    void getAllReturnsAllPosts() {
        List<BulletinPost> posts = List.of(buildPost(UUID.randomUUID()), buildPost(UUID.randomUUID()));
        when(bulletinPostRepository.findAll()).thenReturn(posts);

        List<BulletinPost> result = bulletinPostService.getAll();

        assertThat(result).isEqualTo(posts);
    }

    @Test
    void findByAuthorIdReturnsFirstPostWhenFound() {
        UUID authorId = UUID.randomUUID();
        BulletinPost post = buildPost(UUID.randomUUID());
        when(bulletinPostRepository.findByAuthorId(authorId)).thenReturn(List.of(post));

        Optional<BulletinPost> result = bulletinPostService.findByAuthorId(authorId);

        assertThat(result).contains(post);
    }

    @Test
    void findByAuthorIdReturnsEmptyWhenNoneFound() {
        UUID authorId = UUID.randomUUID();
        when(bulletinPostRepository.findByAuthorId(authorId)).thenReturn(List.of());

        Optional<BulletinPost> result = bulletinPostService.findByAuthorId(authorId);

        assertThat(result).isEmpty();
    }
}