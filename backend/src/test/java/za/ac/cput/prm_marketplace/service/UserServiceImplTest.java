package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.UserRepository;
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
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User buildUser(UUID id) {
        return new User.Builder()
                .setId(id)
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .build();
    }

    @Test
    void readReturnsNullWhenIdIsNull() {
        User result = userService.read(null);

        assertThat(result).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void readReturnsUserWhenFound() {
        UUID id = UUID.randomUUID();
        User user = buildUser(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User result = userService.read(id);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void readReturnsNullWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        User result = userService.read(id);

        assertThat(result).isNull();
    }

    @Test
    void updateProfileReturnsNullWhenIdIsNull() {
        User result = userService.updateProfile(null, "Jane Doe", null, null);

        assertThat(result).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateProfileReturnsNullWhenAccountDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        User result = userService.updateProfile(id, "Jane Doe", null, null);

        assertThat(result).isNull();
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfileSavesTheThreeEditableFields() {
        UUID id = UUID.randomUUID();
        User existing = buildUser(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateProfile(id, "Jane Roe", "0722222222", "https://cdn/a.png");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Jane Roe");
        assertThat(result.getPhone()).isEqualTo("0722222222");
        assertThat(result.getAvatarUrl()).isEqualTo("https://cdn/a.png");
        assertThat(result.getId()).isEqualTo(id);
    }

    /**
     * The properties that make an account an account — its role, its credential, whether it is
     * verified — are not arguments to this method, so they cannot be changed by it. This is the
     * assertion that replaces the old entity update.
     */
    @Test
    void updateProfileLeavesRoleCredentialAndVerificationUntouched() {
        UUID id = UUID.randomUUID();
        User existing = new User.Builder()
                .setId(id)
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("original-hash")
                .setRole(Role.VENDOR)
                .setVerified(true)
                .setCreatedAt(java.time.LocalDateTime.of(2024, 1, 1, 0, 0))
                .build();
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateProfile(id, "Renamed", null, null);

        assertThat(result.getRole()).isEqualTo(Role.VENDOR);
        assertThat(result.getPasswordHash()).isEqualTo("original-hash");
        assertThat(result.getEmail()).isEqualTo("jane@example.com");
        assertThat(result.isVerified()).isTrue();
        assertThat(result.getCreatedAt()).isEqualTo(java.time.LocalDateTime.of(2024, 1, 1, 0, 0));
    }

    @Test
    void updateProfileClearsOptionalFieldsWhenGivenNull() {
        UUID id = UUID.randomUUID();
        User existing = new User.Builder()
                .setId(id).setName("Jane").setEmail("jane@example.com")
                .setPasswordHash("hash").setRole(Role.STUDENT)
                .setPhone("0710000000").setAvatarUrl("https://cdn/a.png")
                .build();
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateProfile(id, "Jane", null, null);

        assertThat(result.getPhone()).isNull();
        assertThat(result.getAvatarUrl()).isNull();
    }

    @Test
    void deleteReturnsFalseWhenIdIsNull() {
        boolean result = userService.delete(null);

        assertThat(result).isFalse();
        verifyNoInteractions(userRepository);
    }

    @Test
    void deleteReturnsFalseWhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        boolean result = userService.delete(id);

        assertThat(result).isFalse();
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteReturnsTrueAndDeletesWhenExists() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);

        boolean result = userService.delete(id);

        assertThat(result).isTrue();
        verify(userRepository).deleteById(id);
    }

    @Test
    void getAllReturnsAllUsers() {
        List<User> users = List.of(buildUser(UUID.randomUUID()), buildUser(UUID.randomUUID()));
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.getAll();

        assertThat(result).isEqualTo(users);
    }

    @Test
    void findByEmailDelegatesToRepository() {
        User user = buildUser(UUID.randomUUID());
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail("jane@example.com");

        assertThat(result).contains(user);
    }
}