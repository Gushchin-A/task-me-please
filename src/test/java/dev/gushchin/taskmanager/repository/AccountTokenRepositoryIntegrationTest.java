package dev.gushchin.taskmanager.repository;

import static dev.gushchin.taskmanager.jooq.Tables.ACCOUNT_TOKENS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.model.AccountToken;
import dev.gushchin.taskmanager.model.AccountTokenType;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.service.UserService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AccountTokenRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private AccountTokenRepository accountTokenRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void saveShouldPersistAccountToken() {
        User user = userService.create("token-owner@test.com", "Owner", "qwerty");
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Instant expiresAt = createdAt.plusSeconds(3600);
        AccountToken accountToken = new AccountToken(
                null,
                user.getId(),
                AccountTokenType.EMAIL_VERIFICATION,
                "b5f9d8478b5ca92dace306bb66912d75c81a480c25f016aae5582e6e7f89bc1d",
                expiresAt,
                null,
                createdAt);

        AccountToken savedToken = accountTokenRepository.save(accountToken);
        AccountToken foundToken = accountTokenRepository.findByTokenHash(savedToken.getTokenHash());

        assertNotNull(savedToken.getId());
        assertEquals(user.getId(), foundToken.getUserId());
        assertEquals(AccountTokenType.EMAIL_VERIFICATION, foundToken.getType());
        assertEquals(accountToken.getTokenHash(), foundToken.getTokenHash());
        assertEquals(expiresAt, foundToken.getExpiresAt());
        assertNull(foundToken.getUsedAt());
        assertEquals(createdAt, foundToken.getCreatedAt());
    }

    private void cleanDatabase() {
        dsl.deleteFrom(ACCOUNT_TOKENS).execute();
        dsl.deleteFrom(USERS).execute();
    }
}
