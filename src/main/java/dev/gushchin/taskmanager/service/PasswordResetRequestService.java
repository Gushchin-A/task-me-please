package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import dev.gushchin.taskmanager.model.AccountToken;
import dev.gushchin.taskmanager.model.AccountTokenType;
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
public class PasswordResetRequestService {
    public static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    public static final Duration REQUEST_COOLDOWN = Duration.ofSeconds(60);
    public static final Duration REQUEST_LIMIT_WINDOW = Duration.ofHours(24);
    public static final int MAX_REQUEST_ATTEMPTS = 5;

    private final AccountTokenRepository accountTokenRepository;
    private final AccountTokenService accountTokenService;
    private final PasswordResetEmailService passwordResetEmailService;
    private final UserRepository userRepository;

    @Transactional
    public void request(String email) {
        if (email != null && !email.isBlank()) {
            User user = userRepository.findByEmailForUpdate(email);
            if (isEligible(user) && isRequestAllowed(user, Instant.now())) {
                createAndSend(user);
            }
        }
    }

    private boolean isEligible(User user) {
        return user != null && !user.isDeleted() && user.isEmailVerified();
    }

    private boolean isRequestAllowed(User user, Instant now) {
        List<AccountToken> tokens =
                accountTokenRepository.findByUserIdAndType(user.getId(), AccountTokenType.PASSWORD_RESET);
        long attempts = tokens.stream()
                .filter(token -> token.getCreatedAt().isAfter(now.minus(REQUEST_LIMIT_WINDOW)))
                .count();
        boolean cooldownFinished = tokens.isEmpty()
                || !tokens.getLast().getCreatedAt().plus(REQUEST_COOLDOWN).isAfter(now);

        return cooldownFinished && attempts < MAX_REQUEST_ATTEMPTS;
    }

    private void createAndSend(User user) {
        String token = accountTokenService.create(user.getId(), AccountTokenType.PASSWORD_RESET, TOKEN_LIFETIME);

        try {
            passwordResetEmailService.sendPasswordReset(user, token, TOKEN_LIFETIME);
        } catch (TransactionalEmailSendingException ex) {
            if (log.isWarnEnabled()) {
                log.warn("Password reset email could not be sent for user {}", user.getId());
            }
        }
    }
}
