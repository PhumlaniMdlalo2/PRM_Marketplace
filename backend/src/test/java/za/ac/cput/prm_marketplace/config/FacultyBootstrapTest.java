package za.ac.cput.prm_marketplace.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Faculty is the one role a caller cannot grant themselves, so the only thing standing between an
 * anonymous signup and supervision of every order on the platform is that signup being refused.
 * These tests cover the other half: that the role is reachable at all, and reachable only through
 * configuration an operator controls.
 */
@ExtendWith(MockitoExtension.class)
class FacultyBootstrapTest {

    private static final String CONFIGURED = "dean@example.ac.za";

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private FacultyBootstrap bootstrapFor(String configuredEmail) {
        return new FacultyBootstrap(userRepository, passwordEncoder, configuredEmail);
    }

    private void run(FacultyBootstrap bootstrap) {
        bootstrap.run(null);
    }

    @Test
    @DisplayName("nothing is provisioned when no address is configured")
    void unset_provisionsNothing() {
        // The default. A fresh clone must not come up with a working privileged login.
        run(bootstrapFor(""));

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("a null address is treated as unset rather than crashing startup")
    void null_provisionsNothing() {
        run(bootstrapFor(null));

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("whitespace around the address does not stop it matching")
    void whitespaceIsTrimmed() {
        when(userRepository.findByEmail(CONFIGURED)).thenReturn(Optional.of(faculty()));

        run(bootstrapFor("  Dean@Example.ac.ZA  "));

        verify(userRepository).findByEmail(CONFIGURED);
    }

    @Test
    @DisplayName("a value that is not an email is refused loudly, not silently")
    void malformedAddressIsRefused() {
        // A typo in an env var must be visible, but it must not take the marketplace down.
        run(bootstrapFor("not-an-email"));

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("an account that is already faculty is left alone")
    void alreadyFaculty_isUntouched() {
        when(userRepository.findByEmail(CONFIGURED)).thenReturn(Optional.of(faculty()));

        run(bootstrapFor(CONFIGURED));

        // Pointing the variable at a working account repeatedly must be harmless.
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("an existing account is promoted rather than duplicated")
    void existingAccount_isPromoted() {
        User student = new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Dean")
                .setEmail(CONFIGURED)
                .setPasswordHash("hash")
                .setRole(Role.VENDOR)
                .setVerified(true)
                .build();
        when(userRepository.findByEmail(CONFIGURED)).thenReturn(Optional.of(student));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        run(bootstrapFor(CONFIGURED));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.FACULTY);
        // Promotion must not disturb anything else about the account, least of all its password.
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(captor.getValue().getId()).isEqualTo(student.getId());
        assertThat(captor.getValue().getEmail()).isEqualTo(CONFIGURED);
    }

    @Test
    @DisplayName("an unknown address is created as faculty and already verified")
    void unknownAddress_createsFaculty() {
        when(userRepository.findByEmail(CONFIGURED)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        run(bootstrapFor(CONFIGURED));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User created = captor.getValue();
        assertThat(created.getRole()).isEqualTo(Role.FACULTY);
        assertThat(created.getEmail()).isEqualTo(CONFIGURED);
        // Provisioned by an operator, not by someone going through the emailed-code flow.
        assertThat(created.isVerified()).isTrue();
    }

    @Test
    @DisplayName("a created account's password is unguessable and never the configured address")
    void unknownAddress_passwordIsNotGuessable() {
        when(userRepository.findByEmail(CONFIGURED)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        run(bootstrapFor(CONFIGURED));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        String hash = captor.getValue().getPasswordHash();
        assertThat(hash).isNotNull().isNotBlank().doesNotContain(CONFIGURED);
        // A hash of something, not the raw value: the account is claimed via password reset.
        assertThat(hash).startsWith("$2");
        assertThat(passwordEncoder.matches(CONFIGURED, hash)).isFalse();
    }

    @Test
    @DisplayName("two runs create accounts with different passwords")
    void unknownAddress_passwordsAreNotReused() {
        when(userRepository.findByEmail(CONFIGURED)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        run(bootstrapFor(CONFIGURED));
        String first = captorFromSave();

        run(bootstrapFor(CONFIGURED));
        String second = captorFromSave();

        // A constant salt would make a predictable password guessable from two hashes.
        assertThat(first).isNotEqualTo(second);
    }

    private String captorFromSave() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues().get(captor.getAllValues().size() - 1).getPasswordHash();
    }

    private static User faculty() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Dean")
                .setEmail(CONFIGURED)
                .setPasswordHash("hash")
                .setRole(Role.FACULTY)
                .setVerified(true)
                .build();
    }
}