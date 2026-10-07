package za.ac.cput.prm_marketplace.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.repository.UserRepository;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

/**
 * Grants the ADMIN role at startup, from configuration.
 *
 * <p>This exists because admin is deliberately not self-serviceable. {@code AuthServiceImpl}
 * rejects a signup asking for ADMIN, and that rejection is correct: admin supervises every
 * order on the platform, refund any payment, and moderate reports, so a self-registered admin
 * account is an escalation into other people's data rather than a role choice. But refusing to
 * create them is only half the job — if nothing ever grants the role, then report moderation,
 * refunds, admin supervision of orders and cross-user reads are unreachable code that looks
 * implemented. This closes that half.
 *
 * <p>Two design choices are worth stating, because the obvious alternatives are worse:
 *
 * <ul>
 *   <li><b>No default.</b> The property resolves to an empty string unless an operator sets it, so a
 *       fresh clone starts with no admin account at all rather than one whose credentials are
 *       published in version control. A seed migration with a known password hash would hand a
 *       working privileged login to anyone who can read the repository.</li>
 *   <li><b>Promote, never demote.</b> A configured address that is already admin is left
 *       untouched, so repeatedly pointing the variable at an existing account is harmless and does
 *       not fail startup.</li>
 * </ul>
 *
 * <p>The account is created already verified, because it is provisioned by the operator rather
 * than by someone going through the emailed-code flow, and a privileged account that could not log
 * in would just get in the way.
 *
 * <p>When the account does not exist yet, it receives the configured initial password or a random
 * password that is never logged. If no initial password was supplied, the operator claims it
 * through the ordinary {@code POST /api/auth/forgot-password} flow instead.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    /** Enough entropy that the value cannot be guessed even if it leaked through a log. */
    private static final int UNCLAIMED_PASSWORD_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String configuredEmail;
    private final String configuredPassword;
    private final SecureRandom random = new SecureRandom();

    public AdminBootstrap(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.bootstrap.admin.email:admin.vendra@gmail.com}") String configuredEmail,
                            @Value("${app.bootstrap.admin.password:}") String configuredPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuredEmail = configuredEmail == null ? "" : configuredEmail.trim().toLowerCase(Locale.ROOT);
        this.configuredPassword = configuredPassword == null ? "" : configuredPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (configuredEmail.isEmpty()) {
            log.info("No app.bootstrap.admin.email configured; no admin account provisioned.");
            return;
        }
        if (!configuredEmail.contains("@")) {
            // Not fatal. A typo in an env var should be loud, but refusing to boot would take the
            // whole marketplace down over a value only one account depends on.
            log.error("app.bootstrap.admin.email is not an email address; no admin account provisioned.");
            return;
        }

        userRepository.findByEmail(configuredEmail).ifPresentOrElse(
                this::promoteExisting,
                this::createAdmin);
    }

    private void promoteExisting(User existing) {
        if (existing.getRole() == Role.ADMIN) {
            if (!configuredPassword.isBlank()
                    && (existing.getPasswordHash() == null
                    || !passwordEncoder.matches(configuredPassword, existing.getPasswordHash()))) {
                userRepository.save(new User.Builder()
                        .copy(existing)
                        .setPasswordHash(passwordEncoder.encode(configuredPassword))
                        .build());
                log.info("Applied configured password to admin account {}.", configuredEmail);
                return;
            }
            log.info("Admin account {} is already admin; nothing to do.", configuredEmail);
            return;
        }
        User promoted = new User.Builder()
                .copy(existing)
                .setRole(Role.ADMIN)
                .setPasswordHash(configuredPassword.isBlank()
                        ? existing.getPasswordHash()
                        : passwordEncoder.encode(configuredPassword))
                .build();
        userRepository.save(promoted);
        log.warn("Promoted {} from {} to ADMIN as configured by app.bootstrap.admin.email.",
                configuredEmail, existing.getRole());
    }

    private void createAdmin() {
        User created = new User.Builder()
                .setName("Admin")
                .setEmail(configuredEmail)
                .setPasswordHash(passwordEncoder.encode(configuredPassword.isBlank()
                        ? unclaimablePassword()
                        : configuredPassword))
                .setRole(Role.ADMIN)
                .setVerified(true)
                .build();
        userRepository.save(created);
        log.warn("Provisioned ADMIN account {}.", configuredEmail);
    }

    private String unclaimablePassword() {
        byte[] bytes = new byte[UNCLAIMED_PASSWORD_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}