package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.ac.cput.prm_marketplace.domain.PasswordResetToken;
import za.ac.cput.prm_marketplace.domain.RefreshToken;
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
import za.ac.cput.prm_marketplace.repository.RefreshTokenRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;
import za.ac.cput.prm_marketplace.repository.VendorProfileRepository;
import za.ac.cput.prm_marketplace.repository.VerificationCodeRepository;
import za.ac.cput.prm_marketplace.security.JwtService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VendorProfileRepository vendorProfileRepository;

    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

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
                vendorProfileRepository,
                verificationCodeRepository,
                passwordResetTokenRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                emailService,
                java.util.concurrent.TimeUnit.DAYS.toMillis(30)
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
        when(userRepository.existsByEmail("jane@mycput.ac.za")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(new RegisterRequest(
                "Jane Doe", "jane@mycput.ac.za", "password123", Role.STUDENT, null));

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

        authService.register(new RegisterRequest("Jane", "jane@mycput.ac.za", "password123", Role.STUDENT, null));

        ArgumentCaptor<VerificationCode> captor = ArgumentCaptor.forClass(VerificationCode.class);
        verify(verificationCodeRepository).save(captor.capture());
        assertEquals(6, captor.getValue().getCode().length());
        assertTrue(captor.getValue().getCode().matches("\\d{6}"));
        assertFalse(captor.getValue().isUsed());

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationCode(emailCaptor.capture(), anyString());
        assertEquals("jane@mycput.ac.za", emailCaptor.getValue());
    }

    @Test
    @DisplayName("register: rejects a duplicate email with a conflict")
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("jane@mycput.ac.za")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(
                new RegisterRequest("Jane", "jane@mycput.ac.za", "password123", Role.STUDENT, null)));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register: normalises the email to lower case")
    void register_normalisesEmail() {
        stubSharedDependencies();
        when(userRepository.existsByEmail("jane@mycput.ac.za")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(new RegisterRequest("Jane", "Jane@MyCPUT.AC.ZA", "password123", Role.STUDENT, null));

        verify(userRepository).existsByEmail("jane@mycput.ac.za");
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
    @DisplayName("login: stores a refresh token next to the access token it hands back")
    void login_storesRefreshToken() {
        stubSharedDependencies();
        User user = buildUser("jane@example.com", true);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "stored-hash")).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(new LoginRequest("jane@example.com", "password123"));

        assertThat(response.refreshToken()).hasSize(64);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo(response.refreshToken());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
    }

    @Test
    @DisplayName("refresh: spends the presented token and returns a new pair")
    void refresh_validToken_spendsItAndIssuesNewCredentials() {
        stubSharedDependencies();
        User user = buildUser("jane@example.com", true);
        RefreshToken presented = new RefreshToken.Builder()
                .setUser(user)
                .setToken("presented-token")
                .setExpiresAt(LocalDateTime.now().plusDays(30))
                .build();
        when(refreshTokenRepository.findByTokenAndUsedFalse("presented-token"))
                .thenReturn(Optional.of(presented));
        when(refreshTokenRepository.spendIfUnused(eq("presented-token"), any(LocalDateTime.class)))
                .thenReturn(1);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.refresh("presented-token");

        assertEquals("jwt-token", response.token());
        assertThat(response.refreshToken()).isNotBlank().isNotEqualTo("presented-token");
        verify(refreshTokenRepository).spendIfUnused(eq("presented-token"), any(LocalDateTime.class));
        // One save, and it is the replacement token: the presented one is spent, not rewritten.
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("refresh: a token somebody else has just spent answers like an unknown one")
    void refresh_tokenSpentByAConcurrentExchange_throwsUnauthorized() {
        User user = buildUser("jane@example.com", true);
        RefreshToken presented = new RefreshToken.Builder()
                .setUser(user)
                .setToken("presented-token")
                .setExpiresAt(LocalDateTime.now().plusDays(30))
                .build();
        when(refreshTokenRepository.findByTokenAndUsedFalse("presented-token"))
                .thenReturn(Optional.of(presented));
        // The row looked live when it was read; by the time the database is asked to spend it,
        // the other request has already got there.
        when(refreshTokenRepository.spendIfUnused(eq("presented-token"), any(LocalDateTime.class)))
                .thenReturn(0);

        UnauthorizedException caught = assertThrows(UnauthorizedException.class,
                () -> authService.refresh("presented-token"));

        assertEquals("Session expired. Please sign in again", caught.getMessage());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("refresh: an unknown token answers with the sign-in message")
    void refresh_unknownToken_throwsUnauthorized() {
        when(refreshTokenRepository.findByTokenAndUsedFalse("nope")).thenReturn(Optional.empty());

        UnauthorizedException caught = assertThrows(UnauthorizedException.class,
                () -> authService.refresh("nope"));

        assertEquals("Session expired. Please sign in again", caught.getMessage());
    }

    @Test
    @DisplayName("refresh: an expired but unused token is refused rather than renewed")
    void refresh_expiredToken_throwsUnauthorized() {
        RefreshToken expired = new RefreshToken.Builder()
                .setUser(buildUser("jane@example.com", true))
                .setToken("stale-token")
                .setExpiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(refreshTokenRepository.findByTokenAndUsedFalse("stale-token"))
                .thenReturn(Optional.of(expired));

        assertThrows(UnauthorizedException.class, () -> authService.refresh("stale-token"));

        assertThat(expired.isUsed()).isFalse();
        verify(refreshTokenRepository, never()).spendIfUnused(anyString(), any(LocalDateTime.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("refresh: a blank token is a bad request rather than a lookup")
    void refresh_blankToken_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> authService.refresh("  "));

        verify(refreshTokenRepository, never()).findByTokenAndUsedFalse(anyString());
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
        UUID callerId = UUID.randomUUID();
        User user = buildUser("jane@example.com", true);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("stored-hash", "stored-hash")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.changePassword(callerId, "stored-hash", "brandNew123");

        assertEquals("encoded-hash", user.getPasswordHash());
    }

    @Test
    @DisplayName("changePassword: drops every refresh token the account holds")
    void changePassword_revokesOutstandingSessions() {
        stubSharedDependencies();
        UUID callerId = UUID.randomUUID();
        User user = buildUser("jane@example.com", true);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("stored-hash", "stored-hash")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.changePassword(callerId, "stored-hash", "brandNew123");

        verify(refreshTokenRepository).deleteByUserId(user.getId());
    }

    @Test
    @DisplayName("changePassword: rejects a wrong current password with unauthorized")
    void changePassword_wrongCurrentPassword_throwsUnauthorized() {
        UUID callerId = UUID.randomUUID();
        User user = buildUser("jane@example.com", true);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> authService.changePassword(callerId, "wrong", "brandNew123"));
        verify(userRepository, never()).save(any(User.class));
        // A refused change logs nobody out: the sessions were valid a moment ago and nothing about
        // the attempt has changed that.
        verify(refreshTokenRepository, never()).deleteByUserId(any(UUID.class));
    }

    @Test
    @DisplayName("changePassword: an unknown account fails exactly like a wrong password")
    void changePassword_unknownAccount_doesNotRevealItself() {
        UUID callerId = UUID.randomUUID();
        when(userRepository.findById(callerId)).thenReturn(Optional.empty());

        // The message matches the wrong-password case on purpose. Telling the two apart told an
        // attacker which email addresses were registered.
        UnauthorizedException thrown = assertThrows(UnauthorizedException.class,
                () -> authService.changePassword(callerId, "stored-hash", "brandNew123"));
        assertEquals("Current password is incorrect", thrown.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("register: refuses to grant admin, which supervises every order")
    void register_rejectsAdminSelfService() {
        stubSharedDependencies();
        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> authService.register(new RegisterRequest(
                        "Sneaky", "sneaky@example.com", "password123", Role.ADMIN, null)));

        assertTrue(thrown.getMessage().contains("ADMIN"));
        // Fails closed: the account is never written, so there is nothing to escalate into.
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register: still honours the self-service vendor role")
    void register_allowsVendorSelfService() {
        stubSharedDependencies();
        when(userRepository.existsByEmail("seller@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vendorProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(new RegisterRequest(
                "Seller", "seller@example.com", "password123", Role.VENDOR, null,
                "Seller Store", "REG-123"));

        assertNotNull(response);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.VENDOR, captor.getValue().getRole());
        ArgumentCaptor<za.ac.cput.prm_marketplace.domain.VendorProfile> profileCaptor =
                ArgumentCaptor.forClass(za.ac.cput.prm_marketplace.domain.VendorProfile.class);
        verify(vendorProfileRepository).save(profileCaptor.capture());
        assertEquals("Seller Store", profileCaptor.getValue().getBusinessName());
        assertEquals("REG-123", profileCaptor.getValue().getRegistrationNo());
        assertFalse(profileCaptor.getValue().isVerified());
    }

    @ParameterizedTest
    @ValueSource(strings = {"jane@university.ac.za", "jane@campus.edu.za"})
    @DisplayName("register: accepts student email addresses from academic domains")
    void register_studentEmailOutsideCputDomain_isAccepted(String email) {
        stubSharedDependencies();
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(new RegisterRequest(
                "Jane Doe", email, "password123", Role.STUDENT, null));

        assertNotNull(response);
        verify(userRepository).existsByEmail(email);
        verify(userRepository).save(argThat(user -> email.equals(user.getEmail())
                && user.getRole() == Role.STUDENT));
    }

    @Test
    @DisplayName("register: rejects student addresses outside academic domains")
    void register_studentEmailOutsideAcademicDomains_isRejected() {
        assertThrows(BadRequestException.class, () -> authService.register(new RegisterRequest(
                "Jane Doe", "jane@example.com", "password123", Role.STUDENT, null)));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register: requires seller details for vendor accounts")
    void register_vendorWithoutBusinessName_isRejected() {
        assertThrows(BadRequestException.class, () -> authService.register(new RegisterRequest(
                "Seller", "seller@example.com", "password123", Role.VENDOR, null)));

        verify(userRepository, never()).save(any(User.class));
    }
}