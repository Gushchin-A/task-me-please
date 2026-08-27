package dev.gushchin.taskmanager.controller;

import static dev.gushchin.taskmanager.jooq.Tables.ACCOUNT_TOKENS;
import static dev.gushchin.taskmanager.jooq.Tables.PERSISTENT_LOGINS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.jooq.tables.records.AccountTokensRecord;
import dev.gushchin.taskmanager.model.AccountTokenType;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.UserRepository;
import dev.gushchin.taskmanager.service.AccountTokenService;
import dev.gushchin.taskmanager.service.UserService;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class PasswordResetControllerIntegrationTest extends IntegrationTestBase {
    private static final String EMAIL = "complete-reset@test.com";
    private static final String NEW_PASSWORD = "new-qwerty";
    private static final String OLD_PASSWORD = "old-qwerty";

    @Autowired
    private AccountTokenService accountTokenService;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        user = createVerifiedUser(EMAIL);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void validTokenShouldOpenPasswordResetForm() throws Exception {
        String token = createResetToken();

        mockMvc.perform(get("/reset-password/" + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/reset-password/" + token + "\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("name=\"passwordConfirmation\"")));
    }

    @Test
    void invalidExpiredUsedAndWrongTypeTokensShouldShowInvalidPage() throws Exception {
        String expiredToken =
                accountTokenService.create(user.getId(), AccountTokenType.PASSWORD_RESET, Duration.ofSeconds(-1));
        String usedToken = createResetToken();
        accountTokenService.consume(usedToken, AccountTokenType.PASSWORD_RESET);
        final String wrongTypeToken =
                accountTokenService.create(user.getId(), AccountTokenType.EMAIL_VERIFICATION, Duration.ofHours(24));

        assertInvalidLink("unknown-token");
        assertInvalidLink(expiredToken);
        assertInvalidLink(usedToken);
        assertInvalidLink(wrongTypeToken);
    }

    @Test
    void passwordMismatchShouldKeepTokenActive() throws Exception {
        String token = createResetToken();

        mockMvc.perform(post("/reset-password/" + token)
                        .with(csrf())
                        .param("password", NEW_PASSWORD)
                        .param("passwordConfirmation", "another-password"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/reset-password/" + token))
                .andExpect(flash().attribute("errorMessage", "Passwords не совпадают"));

        AccountTokensRecord accountToken = dsl.selectFrom(ACCOUNT_TOKENS)
                .where(ACCOUNT_TOKENS.USER_ID.eq(user.getId()))
                .fetchOne();

        assertNotNull(accountToken);
        assertNull(accountToken.getUsedAt());
    }

    @Test
    void blankPasswordShouldKeepTokenActive() throws Exception {
        String token = createResetToken();

        mockMvc.perform(post("/reset-password/" + token)
                        .with(csrf())
                        .param("password", " ")
                        .param("passwordConfirmation", " "))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/reset-password/" + token))
                .andExpect(flash().attribute("errorMessage", "Пароль не заполнен"));

        assertNotNull(accountTokenService.findValid(token, AccountTokenType.PASSWORD_RESET));
    }

    @Test
    void resetShouldUpdatePasswordConsumeTokenAndDeletePersistentLogins() throws Exception {
        String token = createResetToken();
        dsl.insertInto(PERSISTENT_LOGINS)
                .set(PERSISTENT_LOGINS.USERNAME, user.getEmail())
                .set(PERSISTENT_LOGINS.SERIES, "remember-series")
                .set(PERSISTENT_LOGINS.TOKEN, "remember-token")
                .set(PERSISTENT_LOGINS.LAST_USED, OffsetDateTime.now())
                .execute();

        mockMvc.perform(post("/reset-password/" + token)
                        .with(csrf())
                        .param("password", NEW_PASSWORD)
                        .param("passwordConfirmation", NEW_PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("successMessage", "Password изменён. Войдите с новым password"));

        User updatedUser = userRepository.findById(user.getId());
        AccountTokensRecord usedToken = dsl.selectFrom(ACCOUNT_TOKENS)
                .where(ACCOUNT_TOKENS.USER_ID.eq(user.getId()))
                .fetchOne();

        assertFalse(passwordEncoder.matches(OLD_PASSWORD, updatedUser.getPasswordHash()));
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, updatedUser.getPasswordHash()));
        assertNotNull(usedToken.getUsedAt());
        assertEquals(0, dsl.fetchCount(PERSISTENT_LOGINS));

        mockMvc.perform(post("/login").with(csrf()).param("username", EMAIL).param("password", OLD_PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(post("/login").with(csrf()).param("username", EMAIL).param("password", NEW_PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/tasks"));
    }

    @Test
    void deletedAndUnverifiedUsersShouldNotResetPassword() throws Exception {
        String deletedUserToken = createResetToken();
        user.setDeleted(true);
        userRepository.update(user);

        assertInvalidLink(deletedUserToken);

        User unverifiedUser = userService.create("unverified-reset@test.com", "Unverified", OLD_PASSWORD);
        String unverifiedUserToken = accountTokenService.create(
                unverifiedUser.getId(), AccountTokenType.PASSWORD_RESET, Duration.ofMinutes(30));

        assertInvalidLink(unverifiedUserToken);
    }

    @Test
    void passwordResetShouldRequireCsrf() throws Exception {
        String token = createResetToken();

        mockMvc.perform(post("/reset-password/" + token)
                        .param("password", NEW_PASSWORD)
                        .param("passwordConfirmation", NEW_PASSWORD))
                .andExpect(status().isForbidden());
    }

    private void assertInvalidLink(String token) throws Exception {
        mockMvc.perform(get("/reset-password/" + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ссылка недействительна или устарела")));
    }

    private String createResetToken() {
        return accountTokenService.create(user.getId(), AccountTokenType.PASSWORD_RESET, Duration.ofMinutes(30));
    }

    private User createVerifiedUser(String email) {
        User verifiedUser = userService.create(email, "Reset user", OLD_PASSWORD);
        verifiedUser.setEmailVerified(true);
        verifiedUser.setEmailVerifiedAt(Instant.now());

        return userRepository.update(verifiedUser);
    }

    private void cleanDatabase() {
        dsl.deleteFrom(PERSISTENT_LOGINS).execute();
        dsl.deleteFrom(ACCOUNT_TOKENS).execute();
        dsl.deleteFrom(USERS).execute();
    }
}
