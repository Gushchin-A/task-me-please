package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.InvalidAccountTokenException;
import dev.gushchin.taskmanager.model.AccountToken;
import dev.gushchin.taskmanager.model.AccountTokenType;
import dev.gushchin.taskmanager.repository.AccountTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountTokenService {
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final AccountTokenRepository accountTokenRepository;

    @Transactional
    public String create(UUID userId, AccountTokenType type, Duration lifetime) {
        Instant now = Instant.now();
        String token = generateToken();
        AccountToken accountToken =
                new AccountToken(null, userId, type, hashToken(token), now.plus(lifetime), null, null, now);

        if (type == AccountTokenType.EMAIL_VERIFICATION) {
            accountTokenRepository.invalidateReplacedByUserIdAndType(userId, type, now);
        } else {
            accountTokenRepository.invalidateActiveByUserIdAndType(userId, type, now);
        }
        accountTokenRepository.save(accountToken);

        return token;
    }

    public AccountToken findValid(String token, AccountTokenType type) {
        AccountToken accountToken = find(token, type);
        if (accountToken == null
                || accountToken.getUsedAt() != null
                || accountToken.getInvalidatedAt() != null
                || !accountToken.getExpiresAt().isAfter(Instant.now())) {
            throw new InvalidAccountTokenException();
        }

        return accountToken;
    }

    public AccountToken find(String token, AccountTokenType type) {
        AccountToken accountToken = accountTokenRepository.findByTokenHash(hashToken(token));
        if (accountToken == null || accountToken.getType() != type) {
            return null;
        }

        return accountToken;
    }

    @Transactional
    public AccountToken consume(String token, AccountTokenType type) {
        AccountToken accountToken = findValid(token, type);
        AccountToken usedToken = accountTokenRepository.markUsedIfActive(accountToken.getId(), Instant.now());
        if (usedToken == null) {
            throw new InvalidAccountTokenException();
        }

        return usedToken;
    }

    @Transactional
    public AccountToken consumeIfActive(String token, AccountTokenType type) {
        AccountToken accountToken = find(token, type);
        if (accountToken == null
                || accountToken.getUsedAt() != null
                || accountToken.getInvalidatedAt() != null
                || !accountToken.getExpiresAt().isAfter(Instant.now())) {
            return null;
        }

        return accountTokenRepository.markUsedIfActive(accountToken.getId(), Instant.now());
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] tokenHash = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(tokenHash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
