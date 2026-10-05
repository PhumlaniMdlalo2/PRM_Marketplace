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
 * Grants the FACULTY role at startup, from configuration.
 *
 * <p>This exists because faculty is deliberately not self-serviceable. {@code AuthServiceImpl}
 * rejects a signup asking for FACULTY, and that rejection is correct: faculty supervises every
 * order on the platform, refund any payment, and moderate reports, so a self-registered faculty
 * account is an escalation into other people's data rather than a role choice. But refusing to
 * create them is only half the job — if nothing ever grants the role, then report moderation,
 * refunds, faculty supervision of orders and cross-user reads are unreachable code that looks
 * implemented. This closes that half.
 *
 * <p>Two design choices are worth stating, because the obvious alternatives are worse:
 *
 * <ul>
 *   <li><b>No default.</b> The property resolves to an empty string unless an operator sets it, so a
 *       fresh clone starts with no faculty account at all rather than one whose credentials are
 *       published in version control. A seed migration with a known password hash would hand a
 *       working privileged login to anyone who can read the repository.</li>
 *   <li><b>Promote, never demote.</b> A configured address that is already faculty is left
 *       untouched, so repeatedly pointing the variable at an existing account is harmless and does
 *       not fail startup.</li>
 * </ul>
 *
 * <p>The account is created already verified, because it is provisioned by the operator rather
 * than by someone going through the emailed-code flow, and a privileged account that could not log
 * in would just get in the way.
 *
 * <p>When the account does not exist yet, it is given a random password that is never logged. The
 * operator claims it through the ordinary {@code POST /api/auth/forgot-password} flow instead. That
 * is the reason to create the account at all rather than to fail loudly and require a manual
 * database insert: the reset path already exists and is already the audited way to set a password.
 */
@Component
public class FacultyBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FacultyBootstrap.class);

    /** Enough entropy that the value cannot be guessed even if it leaked through a log. */
    private static final int UNCLAIMED_PASSWORD_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String configuredEmail;
    private final SecureRandom random = new SecureRandom();

    public FacultyBootstrap(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.bootstrap.faculty.email:}") String configuredEmail) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuredEmail = configuredEmail == null ? "" : configuredEmail.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (configuredEmail.isEmpty()) {
            log.info("No app.bootstrap.faculty.email configured; no faculty account provisioned.");
            return;
        }
        if (!configuredEmail.contains("@")) {
            // Not fatal. A typo in an env var should be loud, but refusing to boot would take the
            // whole marketplace down over a value only one account depends on.
            log.error("app.bootstrap.faculty.email is not an email address; no faculty account provisioned.");
            return;
        }

        userRepository.findByEmail(configuredEmail).ifPresentOrElse(
                this::promoteExisting,
                this::createFaculty);
    }

    private void promoteExisting(User existing) {
        if (existing.getRole() == Role.FACULTY) {
            log.info("Faculty account {} is already faculty; nothing to do.", configuredEmail);
            return;
        }
        User promoted = new User.Builder()
                .copy(existing)
                .setRole(Role.FACULTY)
                .build();
        userRepository.save(promoted);
        log.warn("Promoted {} from {} to FACULTY as configured by app.bootstrap.faculty.email.",
                configuredEmail, existing.getRole());
    }

    private void createFaculty() {
        User created = new User.Builder()
                .setName("Faculty")
                .setEmail(configuredEmail)
                // Deliberately unusable and deliberately not logged. Claiming the account goes
                // through the password-reset flow, which is itself emailed and audited.
                .setPasswordHash(passwordEncoder.encode(unclaimablePassword()))
                .setRole(Role.FACULTY)
                .setVerified(true)
                .build();
        userRepository.save(created);
        log.warn("Provisioned FACULTY account {}. Use the forgot-password flow to set its password.",
                configuredEmail);
    }

    private String unclaimablePassword() {
        byte[] bytes = new byte[UNCLAIMED_PASSWORD_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}