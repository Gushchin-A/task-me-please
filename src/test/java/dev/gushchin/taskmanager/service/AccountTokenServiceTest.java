package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.gushchin.taskmanager.model.AccountToken;
import dev.gushchin.taskmanager.model.AccountTokenType;
import dev.gushchin.taskmanager.repository.AccountTokenRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountTokenServiceTest {
    @Test
    void consumeIfActiveShouldReturnNullWhenAtomicUpdateLosesRace() {
        AccountTokenRepository accountTokenRepository = mock(AccountTokenRepository.class);
        AccountTokenService accountTokenService = new AccountTokenService(accountTokenRepository);
        Instant now = Instant.now();
        AccountToken activeToken = new AccountToken(
                1L,
                UUID.randomUUID(),
                AccountTokenType.EMAIL_VERIFICATION,
                "token-hash",
                now.plusSeconds(60),
                null,
                null,
                now);
        when(accountTokenRepository.findByTokenHash(anyString())).thenReturn(activeToken);
        when(accountTokenRepository.markUsedIfActive(anyLong(), any(Instant.class)))
                .thenReturn(null);

        AccountToken result =
                accountTokenService.consumeIfActive("verification-token", AccountTokenType.EMAIL_VERIFICATION);

        assertNull(result);
    }
}
