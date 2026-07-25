package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.InvalidAccountTokenException;
import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import dev.gushchin.taskmanager.model.AccountToken;
import dev.gushchin.taskmanager.model.AccountTokenType;
import dev.gushchin.taskmanager.model.EmailVerificationResendState;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.AccountTokenRepository;
import dev.gushchin.taskmanager.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {
    public static final Duration TOKEN_LIFETIME = Duration.ofHours(24);
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private static final Duration RESEND_ATTEMPT_WINDOW = Duration.ofHours(24);
    private static final int MAX_RESEND_ATTEMPTS = 5;

    private final AccountTokenRepository accountTokenRepository;
    private final AccountTokenService accountTokenService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final VerificationEmailService verificationEmailService;

    @Transactional
    public User register(String email, String name, String password, String invite) {
        User user = userService.create(email, name, password);

        createAndSend(user, invite);

        return user;
    }

    @Transactional
    public void resend(String email, String invite) {
        User user = userRepository.findByEmailForUpdate(email);
        if (user != null && !user.isDeleted() && !user.isEmailVerified()) {
            EmailVerificationResendState resendState = getResendState(user, Instant.now());
            if (resendState.available()) {
                createAndSend(user, invite);
            }
        }
    }

    public EmailVerificationResendState getResendState(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null || user.isDeleted() || user.isEmailVerified()) {
            return new EmailVerificationResendState(0, MAX_RESEND_ATTEMPTS);
        }

        return getResendState(user, Instant.now());
    }

    @Transactional
    public User verify(String token) {
        AccountToken accountToken = accountTokenService.consume(token, AccountTokenType.EMAIL_VERIFICATION);
        User user = userService.findById(accountToken.getUserId());
        if (user.isDeleted()) {
            throw new InvalidAccountTokenException();
        }

        Instant now = Instant.now();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(now);
        user.setUpdatedAt(now);
        User verifiedUser = userRepository.update(user);
        accountTokenRepository.invalidateActiveByUserIdAndType(user.getId(), AccountTokenType.EMAIL_VERIFICATION, now);

        return verifiedUser;
    }

    private void createAndSend(User user, String invite) {
        String token = accountTokenService.create(user.getId(), AccountTokenType.EMAIL_VERIFICATION, TOKEN_LIFETIME);

        try {
            verificationEmailService.sendVerification(user, token, TOKEN_LIFETIME, invite);
        } catch (TransactionalEmailSendingException ex) {
            if (log.isWarnEnabled()) {
                log.warn("Verification email could not be sent for user {}", user.getId());
            }
        }
    }

    private EmailVerificationResendState getResendState(User user, Instant now) {
        List<AccountToken> tokens =
                accountTokenRepository.findByUserIdAndType(user.getId(), AccountTokenType.EMAIL_VERIFICATION);
        int attemptsUsed = (int) tokens.stream()
                .skip(1)
                .filter(token -> token.getCreatedAt().isAfter(now.minus(RESEND_ATTEMPT_WINDOW)))
                .count();
        int remainingAttempts = Math.max(0, MAX_RESEND_ATTEMPTS - attemptsUsed);
        int cooldownSeconds = getCooldownSeconds(tokens, now);

        return new EmailVerificationResendState(cooldownSeconds, remainingAttempts);
    }

    private int getCooldownSeconds(List<AccountToken> tokens, Instant now) {
        if (tokens.isEmpty()) {
            return 0;
        }

        Instant availableAt = tokens.getLast().getCreatedAt().plus(RESEND_COOLDOWN);
        if (!availableAt.isAfter(now)) {
            return 0;
        }

        Duration remaining = Duration.between(now, availableAt);
        long seconds = remaining.getSeconds();
        if (remaining.getNano() > 0) {
            seconds++;
        }

        return Math.toIntExact(seconds);
    }
}
