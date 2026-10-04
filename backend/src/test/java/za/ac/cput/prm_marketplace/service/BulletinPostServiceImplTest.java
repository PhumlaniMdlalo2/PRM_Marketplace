package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.BulletinPost;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.BulletinPostRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulletinPostServiceImplTest {

    @Mock
    private BulletinPostRepository bulletinPostRepository;

    @Mock
    private UserRepository userRepository;

    private BulletinPostServiceImpl service;

    private UUID authorId;
    private UUID intruderId;
    private UUID postId;

    @BeforeEach
    void setUp() {
        service = new BulletinPostServiceImpl(bulletinPostRepository, userRepository);
        authorId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        postId = UUID.randomUUID();
    }

    @Test
    @DisplayName("create assigns the caller as author and ignores any author in the body")
    void create_takesAuthorFromTheRequester() {
        when(userRepository.findById(authorId)).thenReturn(Optional.of(buildUser(authorId)));
        when(bulletinPostRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        BulletinPost submitted = new BulletinPost.Builder()
                .setId(UUID.randomUUID())
                .setAuthor(buildUser(intruderId))
                .setTitle("Water outage")
                .setBody("Supply is interrupted.")
                .setCategory("Maintenance")
                .build();

        BulletinPost created = service.create(submitted, authorId);

        assertThat(created).isNotNull();
        assertThat(created.getAuthor().getId()).isEqualTo(authorId);
        // A client-supplied id must not turn the insert into an overwrite.
        assertThat(created.getId()).isNull();
    }

    @Test
    @DisplayName("create starts the counters at zero rather than the body's values")
    void create_doesNotTrustTheBodyCounters() {
        when(userRepository.findById(authorId)).thenReturn(Optional.of(buildUser(authorId)));
        when(bulletinPostRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        BulletinPost submitted = new BulletinPost.Builder()
                .setAuthor(buildUser(intruderId))
                .setTitle("Water outage")
                .setBody("Supply is interrupted.")
                .setCommentCount(500)
                .setLikeCount(999)
                .build();

        BulletinPost created = service.create(submitted, authorId);

        assertThat(created.getCommentCount()).isZero();
        assertThat(created.getLikeCount()).isZero();
    }

    @Test
    @DisplayName("create returns null when the caller has no user row")
    void create_unknownAuthorReturnsNull() {
        when(userRepository.findById(authorId)).thenReturn(Optional.empty());

        assertThat(service.create(buildPost(authorId), authorId)).isNull();
        verify(bulletinPostRepository, never()).save(any());
    }

    @Test
    @DisplayName("create returns null for a null payload or author")
    void create_rejectsNulls() {
        assertThat(service.create(null, authorId)).isNull();
        assertThat(service.create(buildPost(authorId), null)).isNull();
        verifyNoInteractions(bulletinPostRepository);
    }

    @Test
    @DisplayName("update saves the caller's post but keeps the stored author and counters")
    void update_preservesStoredOwnership() {
        BulletinPost stored = buildPost(authorId);
        stored.incrementCommentCount();
        stored.incrementCommentCount();
        stored.incrementLikeCount();
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(stored));
        when(bulletinPostRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        BulletinPost body = new BulletinPost.Builder()
                .copy(buildPost(intruderId))
                .setTitle("Edited title")
                .setCommentCount(0)
                .setLikeCount(0)
                .build();

        BulletinPost updated = service.update(body, authorId);

        assertThat(updated).isNotNull();
        assertThat(updated.getAuthor().getId()).isEqualTo(authorId);
        assertThat(updated.getTitle()).isEqualTo("Edited title");
        assertThat(updated.getCommentCount()).isEqualTo(2);
        assertThat(updated.getLikeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("update refuses to touch another account's post")
    void update_foreignPostIsRefused() {
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(buildPost(intruderId)));

        assertThat(service.update(buildPost(intruderId), authorId)).isNull();
        verify(bulletinPostRepository, never()).save(any());
    }

    @Test
    @DisplayName("update returns null for a missing post or a body without an id")
    void update_invalidInputReturnsNull() {
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.empty());

        assertThat(service.update(buildPost(authorId), authorId)).isNull();
        assertThat(service.update(new BulletinPost.Builder().build(), authorId)).isNull();
        assertThat(service.update(null, authorId)).isNull();
        verify(bulletinPostRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete removes the caller's own post")
    void delete_ownedPostIsRemoved() {
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(buildPost(authorId)));

        assertThat(service.delete(postId, authorId)).isTrue();
        verify(bulletinPostRepository).deleteById(postId);
    }

    @Test
    @DisplayName("delete refuses to remove another account's post")
    void delete_foreignPostIsRefused() {
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(buildPost(intruderId)));

        assertThat(service.delete(postId, authorId)).isFalse();
        verify(bulletinPostRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete returns false for a missing post or a null id")
    void delete_missingReturnsFalse() {
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.empty());

        assertThat(service.delete(postId, authorId)).isFalse();
        assertThat(service.delete(null, authorId)).isFalse();
        verify(bulletinPostRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("read returns the stored post or null when it is missing")
    void read_returnsStoredPost() {
        BulletinPost stored = buildPost(authorId);
        when(bulletinPostRepository.findById(postId)).thenReturn(Optional.of(stored));

        assertThat(service.read(postId)).isSameAs(stored);
        assertThat(service.read(null)).isNull();
    }

    @Test
    @DisplayName("getByAuthor scopes to the given account and rejects a null id")
    void getByAuthor_scopesToTheAuthor() {
        BulletinPost stored = buildPost(authorId);
        when(bulletinPostRepository.findByAuthorId(authorId)).thenReturn(List.of(stored));

        assertThat(service.getByAuthor(authorId)).containsExactly(stored);
        assertThat(service.getByAuthor(null)).isEmpty();
    }

    @Test
    @DisplayName("getAll returns every post")
    void getAll_returnsEveryPost() {
        BulletinPost stored = buildPost(authorId);
        when(bulletinPostRepository.findAll()).thenReturn(List.of(stored));

        assertThat(service.getAll()).containsExactly(stored);
    }

    private User buildUser(UUID id) {
        return new User.Builder().setId(id).setEmail(id + "@example.com").build();
    }

    private BulletinPost buildPost(UUID author) {
        return new BulletinPost.Builder()
                .setId(postId)
                .setAuthor(buildUser(author))
                .setTitle("Water outage")
                .setBody("Supply is interrupted until Friday.")
                .setCategory("Maintenance")
                .build();
    }
}