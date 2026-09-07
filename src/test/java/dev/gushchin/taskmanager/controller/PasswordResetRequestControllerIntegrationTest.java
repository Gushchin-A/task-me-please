package dev.gushchin.taskmanager.controller;

import static dev.gushchin.taskmanager.jooq.Tables.ACCOUNT_TOKENS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import dev.gushchin.taskmanager.jooq.tables.records.AccountTokensRecord;
import dev.gushchin.taskmanager.model.AccountTokenType;
import dev.gushchin.taskmanager.model.PasswordResetRequestResult;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.UserRepository;
import dev.gushchin.taskmanager.service.PasswordResetRequestService;
import dev.gushchin.taskmanager.service.TransactionalEmailSender;
import dev.gushchin.taskmanager.service.UserService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class PasswordResetRequestControllerIntegrationTest extends IntegrationTestBase {
    private static final String EMAIL = "reset-request@test.com";
    private static final String PASSWORD = "qwerty";

    @Autowired
    private DSLContext dsl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @MockitoBean
    private TransactionalEmailSender emailSender;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        reset(emailSender);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void forgotPasswordPageShouldBePublicAndLinkedFromLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/forgot-password\"")))
                .andExpect(content().string(containsString("Забыли пароль?")));

        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/forgot-password\"")))
                .andExpect(content().string(containsString("data-submit-loading")))
                .andExpect(content().string(containsString("data-loading-text=\"Отправляем письмо…\"")))
                .andExpect(content().string(containsString("name=\"email\"")))
                .andExpect(content().string(containsString("Сбросить пароль")))
                .andExpect(content()
                        .string(containsString(
                                "Введите адрес электронной почты вашей учетной записи, и мы вышлем вам ссылку")))
                .andExpect(content().string(containsString("Есть аккаунт?")))
                .andExpect(content().string(containsString(">Войти</a>")));
    }

    @Test
    void requestShouldCreateResetTokenAndSendEmailForVerifiedUser() throws Exception {
        User user = createVerifiedUser(EMAIL);
        final Instant beforeRequest = Instant.now();

        requestPasswordReset(EMAIL);

        AccountTokensRecord token = dsl.selectFrom(ACCOUNT_TOKENS)
                .where(ACCOUNT_TOKENS.USER_ID.eq(user.getId()))
                .fetchOne();
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        org.mockito.ArgumentMatchers.eq(EMAIL),
                        org.mockito.ArgumentMatchers.eq("Восстановление пароля в TaskMePlease"),
                        anyString(),
                        textCaptor.capture());

        assertNotNull(token);
        assertEquals(AccountTokenType.PASSWORD_RESET.name(), token.getType());
        assertFalse(token.getTokenHash().isBlank());
        assertTrue(token.getExpiresAt()
                .toInstant()
                .isAfter(beforeRequest
                        .plus(PasswordResetRequestService.TOKEN_LIFETIME)
                        .minusSeconds(1)));
        assertTrue(textCaptor.getValue().contains("/reset-password/"));
        assertTrue(textCaptor.getValue().contains("Ссылка действует 30 минут."));
    }

    @Test
    void requestPageShouldShowSentStateWithoutForm() throws Exception {
        createVerifiedUser(EMAIL);

        mockMvc.perform(post("/forgot-password").with(csrf()).param("email", EMAIL))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attribute("requestResult", PasswordResetRequestResult.SENT));

        mockMvc.perform(get("/forgot-password").flashAttr("requestResult", PasswordResetRequestResult.SENT))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Отправили вам ссылку для сброса пароля")))
                .andExpect(content().string(containsString("Назад на страницу входа")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("action=\"/forgot-password\""))));
    }

    @Test
    void requestShouldReturnSameResultForUnknownUnverifiedAndDeletedUsers() throws Exception {
        requestPasswordReset("unknown@test.com", PasswordResetRequestResult.INVALID_ACCOUNT);
        User unverifiedUser = userService.create("unverified@test.com", "Unverified", PASSWORD);
        requestPasswordReset(unverifiedUser.getEmail(), PasswordResetRequestResult.INVALID_ACCOUNT);
        User deletedUser = createVerifiedUser("deleted@test.com");
        deletedUser.setDeleted(true);
        userRepository.update(deletedUser);
        requestPasswordReset(deletedUser.getEmail(), PasswordResetRequestResult.INVALID_ACCOUNT);

        mockMvc.perform(get("/forgot-password")
                        .flashAttr("requestResult", PasswordResetRequestResult.INVALID_ACCOUNT)
                        .flashAttr("email", "unknown@test.com")
                        .flashAttr(
                                "errorMessage",
                                "Этот адрес электронной почты недействителен, "
                                        + "не подтвержден или не привязан к учетной записи"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("app-flash-stack")))
                .andExpect(content().string(containsString("flash-message-error")))
                .andExpect(content()
                        .string(containsString(
                                "Этот адрес электронной почты недействителен, не подтвержден или не привязан")))
                .andExpect(content().string(containsString("value=\"unknown@test.com\"")))
                .andExpect(content().string(containsString("Введите адрес электронной почты вашей учетной записи")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("auth-inline-error"))));

        assertEquals(0, dsl.fetchCount(ACCOUNT_TOKENS));
        verify(emailSender, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void requestShouldRespectCooldownAndInvalidatePreviousToken() throws Exception {
        createVerifiedUser(EMAIL);
        requestPasswordReset(EMAIL);
        reset(emailSender);

        requestPasswordReset(EMAIL);

        assertEquals(1, dsl.fetchCount(ACCOUNT_TOKENS));
        verify(emailSender, never()).send(anyString(), anyString(), anyString(), anyString());

        dsl.update(ACCOUNT_TOKENS)
                .set(ACCOUNT_TOKENS.CREATED_AT, OffsetDateTime.now().minusMinutes(2))
                .execute();
        requestPasswordReset(EMAIL);

        List<AccountTokensRecord> tokens =
                dsl.selectFrom(ACCOUNT_TOKENS).orderBy(ACCOUNT_TOKENS.ID.asc()).fetch();

        assertEquals(2, tokens.size());
        assertNotNull(tokens.getFirst().getUsedAt());
        assertNull(tokens.getLast().getUsedAt());
        verify(emailSender)
                .send(
                        org.mockito.ArgumentMatchers.eq(EMAIL),
                        org.mockito.ArgumentMatchers.eq("Восстановление пароля в TaskMePlease"),
                        anyString(),
                        anyString());
    }

    @Test
    void requestShouldAllowOnlyFiveAttemptsWithinTwentyFourHours() throws Exception {
        createVerifiedUser(EMAIL);

        for (int attempt = 0; attempt < PasswordResetRequestService.MAX_REQUEST_ATTEMPTS; attempt++) {
            dsl.update(ACCOUNT_TOKENS)
                    .set(ACCOUNT_TOKENS.CREATED_AT, OffsetDateTime.now().minusMinutes(2))
                    .execute();
            requestPasswordReset(EMAIL);
        }

        dsl.update(ACCOUNT_TOKENS)
                .set(ACCOUNT_TOKENS.CREATED_AT, OffsetDateTime.now().minusMinutes(2))
                .execute();
        requestPasswordReset(EMAIL, PasswordResetRequestResult.LIMIT_REACHED);

        mockMvc.perform(get("/forgot-password").flashAttr("requestResult", PasswordResetRequestResult.LIMIT_REACHED))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("В сервисе используется ограниченный лимит писем в день")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<h1 class=\"auth-title\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("action=\"/forgot-password\""))));

        assertEquals(PasswordResetRequestService.MAX_REQUEST_ATTEMPTS, dsl.fetchCount(ACCOUNT_TOKENS));
    }

    @Test
    void smtpFailureShouldDiscardTokenAndReportDeliveryFailure() throws Exception {
        User user = createVerifiedUser(EMAIL);
        doThrow(new TransactionalEmailSendingException(new RuntimeException()))
                .when(emailSender)
                .send(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/forgot-password").with(csrf()).param("email", EMAIL))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attribute("requestResult", PasswordResetRequestResult.DELIVERY_FAILED))
                .andExpect(flash().attribute("email", EMAIL))
                .andExpect(flash().attribute(
                                "errorMessage",
                                "Не удалось отправить письмо. Проблема на нашей стороне, "
                                        + "мы уже работаем над этим. Попробуйте позже"));

        AccountTokensRecord token = dsl.selectFrom(ACCOUNT_TOKENS)
                .where(ACCOUNT_TOKENS.USER_ID.eq(user.getId()))
                .fetchOne();

        assertNull(token);
    }

    @Test
    void deliveryFailureShouldShowTopFlashAndKeepFormDescription() throws Exception {
        mockMvc.perform(get("/forgot-password")
                        .flashAttr("requestResult", PasswordResetRequestResult.DELIVERY_FAILED)
                        .flashAttr("email", EMAIL)
                        .flashAttr("errorMessage", "Не удалось отправить письмо"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("app-flash-stack")))
                .andExpect(content().string(containsString("flash-message-error")))
                .andExpect(content().string(containsString("Не удалось отправить письмо")))
                .andExpect(content().string(containsString("Введите адрес электронной почты вашей учетной записи")))
                .andExpect(content().string(containsString("value=\"" + EMAIL + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("auth-inline-error"))));
    }

    @Test
    void requestShouldRequireCsrf() throws Exception {
        mockMvc.perform(post("/forgot-password").param("email", EMAIL)).andExpect(status().isForbidden());
    }

    private void requestPasswordReset(String email) throws Exception {
        requestPasswordReset(email, PasswordResetRequestResult.SENT);
    }

    private void requestPasswordReset(String email, PasswordResetRequestResult result) throws Exception {
        mockMvc.perform(post("/forgot-password").with(csrf()).param("email", email))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attribute("requestResult", result));
    }

    private User createVerifiedUser(String email) {
        User user = userService.create(email, "Reset user", PASSWORD);
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(Instant.now());

        return userRepository.update(user);
    }

    private void cleanDatabase() {
        dsl.deleteFrom(ACCOUNT_TOKENS).execute();
        dsl.deleteFrom(USERS).execute();
    }
}
