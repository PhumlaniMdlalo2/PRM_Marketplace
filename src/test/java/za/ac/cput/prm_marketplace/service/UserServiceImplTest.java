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
    void createReturnsNullWhenUserIsNull() {
        User result = userService.create(null);

        assertThat(result).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void createSavesAndReturnsUser() {
        User user = buildUser(null);
        User saved = buildUser(UUID.randomUUID());
        when(userRepository.save(user)).thenReturn(saved);

        User result = userService.create(user);

        assertThat(result).isEqualTo(saved);
        verify(userRepository).save(user);
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
    void updateReturnsNullWhenUserIsNull() {
        User result = userService.update(null);

        assertThat(result).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateReturnsNullWhenIdIsNull() {
        User user = buildUser(null);

        User result = userService.update(user);

        assertThat(result).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateReturnsNullWhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        User user = buildUser(id);
        when(userRepository.existsById(id)).thenReturn(false);

        User result = userService.update(user);

        assertThat(result).isNull();
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateSavesAndReturnsUserWhenExists() {
        UUID id = UUID.randomUUID();
        User user = buildUser(id);
        when(userRepository.existsById(id)).thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.update(user);

        assertThat(result).isEqualTo(user);
        verify(userRepository).save(user);
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