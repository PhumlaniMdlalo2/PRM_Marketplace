package za.ac.cput.prm_marketplace.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import za.ac.cput.prm_marketplace.mapper.UserMapper;
import za.ac.cput.prm_marketplace.repository.PasswordResetTokenRepository;
import za.ac.cput.prm_marketplace.repository.UserRepository;
import za.ac.cput.prm_marketplace.repository.VerificationCodeRepository;
import za.ac.cput.prm_marketplace.security.JwtService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements IAuthService {

    private static final int CODE_LENGTH = 6;
    private static final long CODE_VALID_MINUTES = 15;
    private static final long RESET_TOKEN_VALID_MINUTES = 60;

    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final IEmailService emailService;

    private final SecureRandom random = new SecureRandom();

    public AuthServiceImpl(UserRepository userRepository,
                           VerificationCodeRepository verificationCodeRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           IEmailService emailService) {
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
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
                .setRole(request.role() == null ? Role.STUDENT : request.role())
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

    @Override
    @Transactional(readOnly = true)
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

    @Override
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(normalise(email))
                .orElseThrow(() -> new UnauthorizedException("Account not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationMillis() / 1000,
                UserMapper.toResponse(user)
        );
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