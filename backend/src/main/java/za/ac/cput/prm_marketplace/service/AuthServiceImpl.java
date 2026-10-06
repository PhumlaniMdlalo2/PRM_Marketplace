package za.ac.cput.prm_marketplace.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import za.ac.cput.prm_marketplace.mapper.UserMapper;
import za.ac.cput.prm_marketplace.repository.PasswordResetTokenRepository;
import za.ac.cput.prm_marketplace.repository.RefreshTokenRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;
import za.ac.cput.prm_marketplace.repository.VerificationCodeRepository;
import za.ac.cput.prm_marketplace.security.JwtService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthServiceImpl implements IAuthService {

    private static final int CODE_LENGTH = 6;
    private static final long CODE_VALID_MINUTES = 15;
    private static final long RESET_TOKEN_VALID_MINUTES = 60;

    /** What every failed exchange answers, whichever of the reasons below it turned out to be. */
    private static final String SESSION_EXPIRED_MESSAGE = "Session expired. Please sign in again";

    /** Bytes behind a refresh token. 48 of them become 64 characters of base64url. */
    private static final int REFRESH_TOKEN_BYTES = 48;

    /** Roles an account may pick for itself at signup. Faculty is granted out of band. */
    private static final Set<Role> SELF_SERVICE_ROLES = EnumSet.of(
            Role.STUDENT, Role.VENDOR, Role.RESIDENT);

    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final IEmailService emailService;

    private final long refreshExpirationMillis;
    private final SecureRandom random = new SecureRandom();

    public AuthServiceImpl(UserRepository userRepository,
                           VerificationCodeRepository verificationCodeRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           IEmailService emailService,
                           @Value("${app.jwt.refresh-expiration-ms:2592000000}") long refreshExpirationMillis) {
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.refreshExpirationMillis = refreshExpirationMillis;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request == null) {
            throw new BadRequestException("Registration request is required");
        }
        String email = normalise(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account already exists for " + email);
        }

        User user = new User.Builder()
                .setName(request.name().trim())
                .setEmail(email)
                .setPasswordHash(passwordEncoder.encode(request.password()))
                .setRole(resolveRegistrationRole(request.role()))
                .setPhone(request.phone())
                .setVerified(false)
                .build();

        User saved = userRepository.save(user);
        userRepository.flush();

        String code = generateCode();
        verificationCodeRepository.save(new VerificationCode.Builder()
                .setUser(saved)
                .setCode(code)
                .setExpiresAt(LocalDateTime.now().plusMinutes(CODE_VALID_MINUTES))
                .build());
        emailService.sendVerificationCode(saved.getEmail(), code);

        return buildAuthResponse(saved);
    }

    /**
     * Read-write on purpose, where it used to be read-only: a successful login now also writes the
     * refresh token that lets the session outlive the access token it just minted.
     */
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new BadRequestException("Login request is required");
        }
        String email = normalise(request.email());
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (!user.isVerified()) {
            throw new UnauthorizedException("Account is not verified yet");
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }

        RefreshToken token = refreshTokenRepository.findByTokenAndUsedFalse(refreshToken.trim())
                .orElseThrow(() -> new UnauthorizedException(SESSION_EXPIRED_MESSAGE));

        if (!token.isValid()) {
            // Reached when the row exists and its clock has run out: spent rows never get this far
            // because the lookup only returns unused ones.
            throw new UnauthorizedException(SESSION_EXPIRED_MESSAGE);
        }

        User user = token.getUser();

        // Spent under a predicate rather than by writing the row back, so a copy of this token being
        // presented at the same moment — from a second tab that read it a little too late, or from
        // whoever is holding a stolen one — finds nothing to exchange rather than a second working
        // session. Zero rows means somebody else won.
        if (refreshTokenRepository.spendIfUnused(token.getToken(), LocalDateTime.now()) == 0) {
            throw new UnauthorizedException(SESSION_EXPIRED_MESSAGE);
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public UserResponse verifyCode(String email, String code) {
        User user = userRepository.findByEmail(normalise(email))
                .orElseThrow(() -> new BadRequestException("No account found for " + email));

        VerificationCode verificationCode = verificationCodeRepository
                .findByUserIdAndCodeAndUsedFalse(user.getId(), code)
                .orElseThrow(() -> new BadRequestException("Verification code is invalid"));

        if (verificationCode.isExpired()) {
            throw new BadRequestException("Verification code has expired");
        }

        verificationCode.markUsed();
        verificationCodeRepository.save(verificationCode);

        user.setVerified(true);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void resendCode(String email) {
        User user = userRepository.findByEmail(normalise(email))
                .orElseThrow(() -> new BadRequestException("No account found for " + email));

        verificationCodeRepository.deleteByUserIdAndUsedFalse(user.getId());

        String code = generateCode();
        verificationCodeRepository.save(new VerificationCode.Builder()
                .setUser(user)
                .setCode(code)
                .setExpiresAt(LocalDateTime.now().plusMinutes(CODE_VALID_MINUTES))
                .build());
        emailService.sendVerificationCode(user.getEmail(), code);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(normalise(email)).orElse(null);
        if (user == null) {
            return;
        }

        String token = UUID.randomUUID().toString();
        passwordResetTokenRepository.save(new PasswordResetToken.Builder()
                .setUser(user)
                .setToken(token)
                .setExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_VALID_MINUTES))
                .build());
        emailService.sendPasswordResetToken(user.getEmail(), token);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenAndUsedFalse(request.token())
                .orElseThrow(() -> new BadRequestException("Reset token is invalid"));

        if (token.isExpired()) {
            throw new BadRequestException("Reset token has expired");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        token.markUsed();
        passwordResetTokenRepository.save(token);
    }

    /**
     * Every refresh token the account holds is dropped along with the password, so the old
     * credential stops working on every device at once rather than only on the one whose access
     * token happens to expire first. The caller whose password this is gets a fresh pair at its next
     * refresh only after signing in again, which is the point.
     */
    @Override
    @Transactional
    public void changePassword(UUID requesterId, String currentPassword, String newPassword) {
        User user = userRepository.findById(requesterId)
                .orElseThrow(() -> new UnauthorizedException("Current password is incorrect"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenRepository.deleteByUserId(user.getId());
    }

    /**
     * The role a new account is allowed to sign up with.
     *
     * <p>Previously whatever the request body asked for was granted, so anybody could register as
     * faculty. That matters here specifically because faculty supervises every order on the
     * platform, not just their own: a self-registered faculty account is an escalation into other
     * people's orders.
     *
     * <p>Faculty accounts are granted out of band, so a self-service signup asking for one is
     * rejected rather than quietly downgraded. Silently substituting a lesser role would hand the
     * caller a working account whose permissions are not what they asked for, which hides both the
     * attempt and any client bug that caused it.
     */
    private static Role resolveRegistrationRole(Role requested) {
        if (requested == null) {
            return Role.STUDENT;
        }
        if (!SELF_SERVICE_ROLES.contains(requested)) {
            throw new BadRequestException(
                    "Role " + requested + " cannot be self-registered");
        }
        return requested;
    }

    /**
     * The whole credential set a client is given: a short-lived access token, and the refresh
     * token that renews it once the short one is gone.
     *
     * <p>Both are minted together and neither is useful alone, which is why every path that ends a
     * caller's registration, sign-in or renewal comes through here.
     */
    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                issueRefreshToken(user),
                "Bearer",
                jwtService.getExpirationMillis() / 1000,
                UserMapper.toResponse(user)
        );
    }

    /**
     * Stores a new refresh token for the account and returns the value the client must keep.
     *
     * <p>Random rather than signed for the reason the table exists: a stored value can be looked up,
     * found spent and dropped, where a second signed JWT would answer "valid" until its own clock
     * ran out no matter how many times it had already been exchanged.
     */
    private String issueRefreshToken(User user) {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        refreshTokenRepository.save(new RefreshToken.Builder()
                .setUser(user)
                .setToken(value)
                .setExpiresAt(LocalDateTime.now().plus(refreshExpirationMillis, ChronoUnit.MILLIS))
                .build());

        return value;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String normalise(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}