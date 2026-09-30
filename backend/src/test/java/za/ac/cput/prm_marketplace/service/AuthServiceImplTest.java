package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.ac.cput.prm_marketplace.domain.PasswordResetToken;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VerificationCode;
import za.ac.cput.prm_marketplace.dto.AuthResponse;
import za.ac.cput.prm_marketplace.dto.LoginRequest;
import za.ac.cput.prm_marketplace.dto.RegisterRequest;
import za.ac.cput.prm_marketplace.dto.ResetPasswordRequest;
import za.ac.cput.prm_marketplace.dto.UserResponse;
import za.ac.cput.prm_marketplace.exception.BadRequestException;
import za.ac.cput.prm_marketplace.exception.ConflictException;
import za.ac.cput.prm_marketplace.exception.UnauthorizedException;
import za.ac.cput.prm_marketplace.repository.PasswordResetTokenRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;
import za.ac.cput.prm_marketplace.repository.VerificationCodeRepository;
import za.ac.cput.prm_marketplace.security.JwtService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private IEmailService emailService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                verificationCodeRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                jwtService,
                emailService
        );
    }

    private void stubSharedDependencies() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded-hash");
        lenient().when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        lenient().when(jwtService.getExpirationMillis()).thenReturn(86_400_000L);
    }

    private User buildUser(String email, boolean verified) {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail(email)
                .setPasswordHash("stored-hash")
                .setRole(Role.STUDENT)
                .setVerified(verified)
                .build();
    }

    @Test
    @DisplayName("register: persists a verified=false user with an encoded password and returns a token")
    void register_persistsUserAndReturnsToken() {
        stubSharedDependencies();
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(new RegisterRequest(
                "Jane Doe", "jane@example.com", "password123", null, null));

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertFalse(response.user().verified());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("encoded-hash", captor.getValue().getPasswordHash());
        assertEquals(Role.STUDENT, captor.getValue().getRole());
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("register: stores a 6-digit verification code and emails it")
    void register_storesAndMailsVerificationCode() {
        stubSharedDependencies();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(verificationCodeRepository.save(any(VerificationCode.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        authService.register(new RegisterRequest("Jane", "jane@example.com", "password123", null, null));

        ArgumentCaptor<VerificationCode> captor = ArgumentCaptor.forClass(VerificationCode.class);
        verify(verificationCodeRepository).save(captor.capture());
        assertEquals(6, captor.getValue().getCode().length());
        assertTrue(captor.getValue().getCode().matches("\\d{6}"));
        assertFalse(captor.getValue().isUsed());

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationCode(emailCaptor.capture(), anyString());
        assertEquals("jane@example.com", emailCaptor.getValue());
    }

    @Test
    @DisplayName("register: rejects a duplicate email with a conflict")
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(
                new RegisterRequest("Jane", "jane@example.com", "password123", null, null)));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register: normalises the email to lower case")
    void register_normalisesEmail() {
        stubSharedDependencies();
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(new RegisterRequest("Jane", "Jane@Example.COM", "password123", null, null));

        verify(userRepository).existsByEmail("jane@example.com");
    }

    @Test
    @DisplayName("login: returns a token for verified users with the right password")
    void login_verifiedUser_returnsToken() {
        stubSharedDependencies();
        User user = buildUser("jane@example.com", true);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "stored-hash")).thenReturn(true);

        AuthResponse response = authService.login(new LoginRequest("jane@example.com", "password123"));

        assertEquals("jwt-token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(86_400L, response.expiresIn());
    }

    @Test
    @DisplayName("login: rejects a wrong password with unauthorized")
    void login_wrongPassword_throwsUnauthorized() {
        User user = buildUser("jane@example.com", true);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("jane@example.com", "wrong")));
    }

    @Test
    @DisplayName("login: rejects an unknown account with unauthorized")
    void login_unknownUser_throwsUnauthorized() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("nobody@example.com", "password123")));
    }

    @Test
    @DisplayName("login: rejects a verified=false account with unauthorized")
    void login_unverifiedUser_throwsUnauthorized() {
        User user = buildUser("jane@example.com", false);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "stored-hash")).thenReturn(true);

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("jane@example.com", "password123")));
    }

    @Test
    @DisplayName("verifyCode: marks the code used and the user verified")
    void verifyCode_marksCodeUsedAndUserVerified() {
        stubSharedDependencies();
        User user = buildUser("jane@example.com", false);
        VerificationCode code = new VerificationCode.Builder()
                .setUser(user)
                .setCode("123456")
                .setExpiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeRepository.findByUserIdAndCodeAndUsedFalse(any(UUID.class), anyString()))
                .thenReturn(Optional.of(code));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.verifyCode("jane@example.com", "123456");

        assertTrue(response.verified());
        assertTrue(code.isUsed());
        verify(verificationCodeRepository).save(code);
    }

    @Test
    @DisplayName("verifyCode: rejects an unknown code with bad request")
    void verifyCode_unknownCode_throwsBadRequest() {
        User user = buildUser("jane@example.com", false);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeRepository.findByUserIdAndCodeAndUsedFalse(any(UUID.class), anyString()))
                .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authService.verifyCode("jane@example.com", "000000"));
    }

    @Test
    @DisplayName("verifyCode: rejects an expired code with bad request")
    void verifyCode_expiredCode_throwsBadRequest() {
        User user = buildUser("jane@example.com", false);
        VerificationCode code = new VerificationCode.Builder()
                .setUser(user)
                .setCode("123456")
                .setExpiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeRepository.findByUserIdAndCodeAndUsedFalse(any(UUID.class), anyString()))
                .thenReturn(Optional.of(code));

        assertThrows(BadRequestException.class, () -> authService.verifyCode("jane@example.com", "123456"));
        assertFalse(code.isUsed());
    }

    @Test
    @DisplayName("resendCode: clears previous unused codes and issues a fresh one")
    void resendCode_replacesUnusedCodes() {
        User user = buildUser("jane@example.com", false);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeRepository.save(any(VerificationCode.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        authService.resendCode("jane@example.com");

        verify(verificationCodeRepository).deleteByUserIdAndUsedFalse(user.getId());
        ArgumentCaptor<VerificationCode> captor = ArgumentCaptor.forClass(VerificationCode.class);
        verify(verificationCodeRepository).save(captor.capture());
        assertEquals(6, captor.getValue().getCode().length());
        verify(emailService).sendVerificationCode("jane@example.com", captor.getValue().getCode());
    }

    @Test
    @DisplayName("forgotPassword: issues a reset token and emails it")
    void forgotPassword_issuesToken() {
        User user = buildUser("jane@example.com", true);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        authService.forgotPassword("jane@example.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        assertNotNull(captor.getValue().getToken());
        assertFalse(captor.getValue().isUsed());
        verify(emailService).sendPasswordResetToken("jane@example.com", captor.getValue().getToken());
    }

    @Test
    @DisplayName("forgotPassword: stays silent for unknown accounts so emails cannot be enumerated")
    void forgotPassword_unknownUser_isSilent() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.forgotPassword("nobody@example.com"));

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetToken(anyString(), anyString());
    }

    @Test
    @DisplayName("resetPassword: updates the password and burns the token")
    void resetPassword_updatesPasswordAndMarksTokenUsed() {
        stubSharedDependencies();
        User user = buildUser("jane@example.com", true);
        PasswordResetToken token = new PasswordResetToken.Builder()
                .setUser(user)
                .setToken("token-123")
                .setExpiresAt(LocalDateTime.now().plusMinutes(60))
                .build();
        when(passwordResetTokenRepository.findByTokenAndUsedFalse("token-123")).thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.resetPassword(new ResetPasswordRequest("token-123", "newPassword123"));

        assertEquals("encoded-hash", user.getPasswordHash());
        assertTrue(token.isUsed());
        verify(passwordEncoder).encode("newPassword123");
    }

    @Test
    @DisplayName("resetPassword: rejects an expired token with bad request")
    void resetPassword_expiredToken_throwsBadRequest() {
        PasswordResetToken token = new PasswordResetToken.Builder()
                .setUser(buildUser("jane@example.com", true))
                .setToken("token-123")
                .setExpiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(passwordResetTokenRepository.findByTokenAndUsedFalse("token-123")).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("token-123", "newPassword123")));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("resetPassword: rejects an unknown token with bad request")
    void resetPassword_unknownToken_throwsBadRequest() {
        when(passwordResetTokenRepository.findByTokenAndUsedFalse("nope")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("nope", "newPassword123")));
    }

    @Test
    @DisplayName("changePassword: replaces the password when the current one matches")
    void changePassword_correctCurrentPassword_updates() {
        stubSharedDependencies();
        User user = buildUser("jane@example.com", true);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("stored-hash", "stored-hash")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.changePassword("jane@example.com", "stored-hash", "brandNew123");

        assertEquals("encoded-hash", user.getPasswordHash());
    }

    @Test
    @DisplayName("changePassword: rejects a wrong current password with unauthorized")
    void changePassword_wrongCurrentPassword_throwsUnauthorized() {
        User user = buildUser("jane@example.com", true);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> authService.changePassword("jane@example.com", "wrong", "brandNew123"));
        verify(userRepository, never()).save(any(User.class));
    }
}