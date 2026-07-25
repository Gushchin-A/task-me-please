package dev.gushchin.taskmanager.service;

import static dev.gushchin.taskmanager.jooq.Tables.ACCOUNT_TOKENS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.jooq.tables.records.AccountTokensRecord;
import dev.gushchin.taskmanager.model.AccountTokenType;
import dev.gushchin.taskmanager.model.User;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AccountTokenServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private AccountTokenService accountTokenService;

    @Autowired
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        user = userService.create("account-token@test.com", "Account token", "qwerty");
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void createShouldStoreOnlyTokenHashWithTypeAndExpiration() {
        final OffsetDateTime beforeCreation = OffsetDateTime.now();

        String token =
                accountTokenService.create(user.getId(), AccountTokenType.EMAIL_VERIFICATION, Duration.ofHours(24));

        AccountTokensRecord record = dsl.selectFrom(ACCOUNT_TOKENS).fetchOne();

        assertNotNull(record);
        assertFalse(token.isBlank());
        assertNotEquals(token, record.getTokenHash());
        assertEquals(64, record.getTokenHash().length());
        assertEquals(AccountTokenType.EMAIL_VERIFICATION.name(), record.getType());
        assertEquals(user.getId(), record.getUserId());
        assertNull(record.getUsedAt());
        assertFalse(record.getExpiresAt().isBefore(beforeCreation.plusHours(24)));
    }

    @Test
    void createShouldInvalidatePreviousActiveTokenOfSameType() {
        String firstToken =
                accountTokenService.create(user.getId(), AccountTokenType.PASSWORD_RESET, Duration.ofMinutes(30));
        String secondToken =
                accountTokenService.create(user.getId(), AccountTokenType.PASSWORD_RESET, Duration.ofMinutes(30));

        List<AccountTokensRecord> records =
                dsl.selectFrom(ACCOUNT_TOKENS).orderBy(ACCOUNT_TOKENS.ID.asc()).fetch();

        assertNotEquals(firstToken, secondToken);
        assertNotNull(records.getFirst().getUsedAt());
        assertNull(records.getLast().getUsedAt());
    }

    @Test
    void createShouldNotInvalidateTokenOfAnotherType() {
        accountTokenService.create(user.getId(), AccountTokenType.EMAIL_VERIFICATION, Duration.ofHours(24));
        accountTokenService.create(user.getId(), AccountTokenType.PASSWORD_RESET, Duration.ofMinutes(30));

        List<AccountTokensRecord> records =
                dsl.selectFrom(ACCOUNT_TOKENS).orderBy(ACCOUNT_TOKENS.ID.asc()).fetch();

        assertNull(records.getFirst().getUsedAt());
        assertNull(records.getLast().getUsedAt());
    }

    private void cleanDatabase() {
        dsl.deleteFrom(ACCOUNT_TOKENS).execute();
        dsl.deleteFrom(USERS).execute();
    }
}
