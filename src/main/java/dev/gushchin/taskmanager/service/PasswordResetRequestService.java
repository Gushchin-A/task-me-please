package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import dev.gushchin.taskmanager.model.AccountToken;
import dev.gushchin.taskmanager.model.AccountTokenType;
import dev.gushchin.taskmanager.model.PasswordResetRequestResult;
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
    public PasswordResetRequestResult request(String email) {
        if (email == null || email.isBlank()) {
            return PasswordResetRequestResult.INVALID_ACCOUNT;
        }

        User user = userRepository.findByEmailForUpdate(email);
        if (!isEligible(user)) {
            return PasswordResetRequestResult.INVALID_ACCOUNT;
        }

        Instant now = Instant.now();
        List<AccountToken> tokens = getRecentTokens(user, now);
        if (tokens.size() >= MAX_REQUEST_ATTEMPTS) {
            return PasswordResetRequestResult.LIMIT_REACHED;
        }

        if (isCooldownFinished(tokens, now)) {
            createAndSend(user);
        }

        return PasswordResetRequestResult.SENT;
    }

    private boolean isEligible(User user) {
        return user != null && !user.isDeleted() && user.isEmailVerified();
    }

    private List<AccountToken> getRecentTokens(User user, Instant now) {
        return accountTokenRepository.findByUserIdAndType(user.getId(), AccountTokenType.PASSWORD_RESET).stream()
                .filter(token -> token.getCreatedAt().isAfter(now.minus(REQUEST_LIMIT_WINDOW)))
                .toList();
    }

    private boolean isCooldownFinished(List<AccountToken> tokens, Instant now) {
        return tokens.isEmpty()
                || !tokens.getLast().getCreatedAt().plus(REQUEST_COOLDOWN).isAfter(now);
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
