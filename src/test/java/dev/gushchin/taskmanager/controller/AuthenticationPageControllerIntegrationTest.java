package dev.gushchin.taskmanager.controller;

import static dev.gushchin.taskmanager.jooq.Tables.ACCOUNT_TOKENS;
import static dev.gushchin.taskmanager.jooq.Tables.COMMENTS;
import static dev.gushchin.taskmanager.jooq.Tables.PERSISTENT_LOGINS;
import static dev.gushchin.taskmanager.jooq.Tables.TASKS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAMS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_INVITATIONS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_MEMBERS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_TAGS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.UserRepository;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.TeamInvitationService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.TransactionalEmailSender;
import dev.gushchin.taskmanager.service.UserService;
import jakarta.servlet.http.Cookie;
import java.time.OffsetDateTime;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class AuthenticationPageControllerIntegrationTest extends IntegrationTestBase {
    private static final String EMAIL = "auth@test.com";
    private static final String PASSWORD = "qwerty";

    @Autowired
    private DSLContext dsl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private TeamInvitationService teamInvitationService;

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
    void loginPageShouldBeAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Task Me Please")))
                .andExpect(content().string(containsString("action=\"/login\"")))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"remember-me\"")))
                .andExpect(content().string(containsString("Запомнить меня")));
    }

    @Test
    void registrationPageShouldBeAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/registration"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Task Me Please")))
                .andExpect(content().string(containsString("action=\"/registration\"")))
                .andExpect(content().string(containsString("Создать аккаунт")));
    }

    @Test
    void loginShouldRedirectToTasksAfterSuccess() throws Exception {
        createVerifiedUser(EMAIL);

        mockMvc.perform(post("/login").with(csrf()).param("username", EMAIL).param("password", PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/tasks"));
    }

    @Test
    void loginWithoutRememberMeShouldCreateOnlySession() throws Exception {
        createVerifiedUser(EMAIL);

        mockMvc.perform(post("/login").with(csrf()).param("username", EMAIL).param("password", PASSWORD))
                .andExpect(status().isFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                        .doesNotExist("remember-me"));

        assertEquals(0, dsl.fetchCount(PERSISTENT_LOGINS));
    }

    @Test
    void loginWithRememberMeShouldCreatePersistentLoginAndRestoreAuthentication() throws Exception {
        createVerifiedUser(EMAIL);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", EMAIL)
                        .param("password", PASSWORD)
                        .param("remember-me", "on"))
                .andExpect(status().isFound())
                .andReturn();
        Cookie rememberMeCookie = loginResult.getResponse().getCookie("remember-me");

        assertNotNull(rememberMeCookie);
        assertTrue(rememberMeCookie.isHttpOnly());
        assertEquals("Lax", rememberMeCookie.getAttribute("SameSite"));
        assertEquals(30 * 24 * 60 * 60, rememberMeCookie.getMaxAge());
        assertEquals(1, dsl.fetchCount(PERSISTENT_LOGINS));

        mockMvc.perform(get("/tasks").cookie(rememberMeCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Выйти из профиля")));
    }

    @Test
    void rememberMeShouldNotRestoreDeletedUser() throws Exception {
        User user = createVerifiedUser(EMAIL);
        Cookie rememberMeCookie = loginWithRememberMe();
        user.setDeleted(true);
        userRepository.update(user);

        mockMvc.perform(get("/tasks").cookie(rememberMeCookie))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void expiredRememberMeTokenShouldNotRestoreAuthentication() throws Exception {
        createVerifiedUser(EMAIL);
        Cookie rememberMeCookie = loginWithRememberMe();
        dsl.update(PERSISTENT_LOGINS)
                .set(PERSISTENT_LOGINS.LAST_USED, OffsetDateTime.now().minusDays(31))
                .execute();

        mockMvc.perform(get("/tasks").cookie(rememberMeCookie))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void missingPersistentTokenShouldNotRestoreAuthentication() throws Exception {
        createVerifiedUser(EMAIL);
        Cookie rememberMeCookie = loginWithRememberMe();
        dsl.deleteFrom(PERSISTENT_LOGINS).execute();

        mockMvc.perform(get("/tasks").cookie(rememberMeCookie))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void modifiedRememberMeCookieShouldNotRestoreAuthentication() throws Exception {
        createVerifiedUser(EMAIL);
        loginWithRememberMe();
        Cookie modifiedCookie = new Cookie("remember-me", "invalid-cookie");

        mockMvc.perform(get("/tasks").cookie(modifiedCookie))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void logoutWithRememberMeShouldDeletePersistentLogin() throws Exception {
        createVerifiedUser(EMAIL);
        Cookie rememberMeCookie = loginWithRememberMe();
        MvcResult restoredResult = mockMvc.perform(get("/tasks").cookie(rememberMeCookie))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession restoredSession =
                (MockHttpSession) restoredResult.getRequest().getSession();

        mockMvc.perform(post("/logout").with(csrf()).session(restoredSession).cookie(rememberMeCookie))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                        .maxAge("remember-me", 0));

        assertEquals(0, dsl.fetchCount(PERSISTENT_LOGINS));
    }

    @Test
    void loginShouldShowFlashMessageAfterInvalidCredentials() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "missing@test.com")
                        .param("password", "bad-password"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"))
                .andReturn();

        mockMvc.perform(get("/login")
                        .session((MockHttpSession) result.getRequest().getSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Неверный email или пароль.")));
    }

    @Test
    void loginShouldShowSeparateMessageForUnverifiedUser() throws Exception {
        userService.create(EMAIL, "Auth user", PASSWORD);

        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", EMAIL)
                        .param("password", PASSWORD)
                        .param("remember-me", "on"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                        .maxAge("remember-me", 0))
                .andReturn();

        assertEquals(0, dsl.fetchCount(PERSISTENT_LOGINS));

        mockMvc.perform(get("/login")
                        .session((MockHttpSession) result.getRequest().getSession()))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string(containsString("Email не подтверждён. Проверьте почту или отправьте письмо повторно.")))
                .andExpect(content().string(containsString("href=\"/verification-pending\"")))
                .andExpect(content().string(containsString("Перейти к повторной отправке")));
    }

    @Test
    void registrationShouldCreateUser() throws Exception {
        final MvcResult result = mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("email", EMAIL)
                        .param("name", "Auth user")
                        .param("password", PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/verification-pending"))
                .andReturn();

        User user = userRepository.findByEmail(EMAIL);
        List<AccountTokensRecord> tokens = dsl.selectFrom(ACCOUNT_TOKENS)
                .where(ACCOUNT_TOKENS.USER_ID.eq(user.getId()))
                .fetch();

        assertNotNull(user);
        assertFalse(user.isEmailVerified());
        assertEquals(1, tokens.size());
        assertEquals(
                AccountTokenType.EMAIL_VERIFICATION.name(), tokens.getFirst().getType());
        verify(emailSender)
                .send(
                        org.mockito.ArgumentMatchers.eq(EMAIL),
                        org.mockito.ArgumentMatchers.eq("Подтвердите email в Task Me Please"),
                        anyString());

        mockMvc.perform(get("/verification-pending")
                        .session((MockHttpSession) result.getRequest().getSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Мы отправили письмо на " + EMAIL)))
                .andExpect(content().string(containsString("Отправить письмо повторно")))
                .andExpect(content().string(containsString("Повторных попыток осталось:")))
                .andExpect(content().string(containsString("data-remaining-attempts>5</span>")))
                .andExpect(content().string(containsString("data-resend-countdown=")))
                .andExpect(content().string(containsString("<button class=\"auth-submit\" type=\"submit\" disabled>")))
                .andExpect(content().string(containsString("Почему количество попыток в сутки ограничено")))
                .andExpect(content().string(containsString("support@example.com")));
    }

    @Test
    void registrationShouldKeepSafeReturnPathOnVerificationPage() throws Exception {
        String redirect = "/invitations/token-123";

        MvcResult result = mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("email", EMAIL)
                        .param("name", "Auth user")
                        .param("password", PASSWORD)
                        .param("redirect", redirect)
                        .param("invite", "token-123"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/verification-pending"))
                .andReturn();

        mockMvc.perform(get("/verification-pending")
                        .session((MockHttpSession) result.getRequest().getSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"redirect\" value=\"" + redirect + "\"")));
    }

    @Test
    void registrationShouldShowFlashMessageWhenEmailAlreadyExists() throws Exception {
        userService.create(EMAIL, "Auth user", PASSWORD);

        mockMvc.perform(post("/registration").with(csrf()).param("email", EMAIL).param("password", PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/registration"))
                .andExpect(flash().attribute("errorMessage", "Пользователь с таким email уже зарегистрирован."));
    }

    @Test
    void registrationShouldShowFlashMessageWhenPasswordIsBlank() throws Exception {
        mockMvc.perform(post("/registration").with(csrf()).param("email", EMAIL).param("password", " "))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/registration"))
                .andExpect(flash().attribute("errorMessage", "Пароль не заполнен."));
    }

    @Test
    void registrationShouldShowFlashMessageWhenEmailFormatIsInvalid() throws Exception {
        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("email", "wrong-email")
                        .param("password", PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/registration"))
                .andExpect(flash().attribute("errorMessage", "Email имеет неправильный формат."));
    }

    @Test
    void registrationShouldRequireCsrf() throws Exception {
        mockMvc.perform(post("/registration").param("email", EMAIL).param("password", PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserShouldBeRedirectedFromAuthPages() throws Exception {
        mockMvc.perform(get("/login").with(user(EMAIL)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/tasks"));

        mockMvc.perform(get("/registration").with(user(EMAIL)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/tasks"));
    }

    @Test
    void authPagesShouldKeepReturnAndInviteContext() throws Exception {
        TeamInvitation invitation = createInvitation("invited-auth@test.com");
        String redirect = "/invitations/" + invitation.getToken();

        mockMvc.perform(get("/login").param("redirect", redirect).param("invite", invitation.getToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"redirect\" value=\"" + redirect + "\"")))
                .andExpect(content().string(containsString("name=\"invite\" value=\"" + invitation.getToken() + "\"")))
                .andExpect(content().string(not(containsString("Owner"))))
                .andExpect(content().string(containsString("invite-owner@test.com")))
                .andExpect(content().string(containsString("Вас пригласил invite-owner@test.com")))
                .andExpect(content().string(containsString("Invite Team")))
                .andExpect(content().string(containsString("href=\"/registration?redirect=")))
                .andExpect(content().string(containsString("invite=" + invitation.getToken())));
    }

    @Test
    void loginShouldRedirectToSafeReturnPathAfterSuccess() throws Exception {
        String redirect = "/invitations/token-123";
        createVerifiedUser(EMAIL);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", EMAIL)
                        .param("password", PASSWORD)
                        .param("redirect", redirect)
                        .param("invite", "token-123"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(redirect));
    }

    @Test
    void loginFailureShouldKeepInvitationContext() throws Exception {
        TeamInvitation invitation = createInvitation("login-failure@test.com");
        String redirect = "/invitations/" + invitation.getToken();

        String loginRedirect =
                "/login?redirect=/invitations/" + invitation.getToken() + "&invite=" + invitation.getToken();
        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "missing@test.com")
                        .param("password", "bad-password")
                        .param("redirect", redirect)
                        .param("invite", invitation.getToken()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(loginRedirect))
                .andReturn();

        mockMvc.perform(get(loginRedirect)
                        .session((MockHttpSession) result.getRequest().getSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Неверный email или пароль.")))
                .andExpect(content().string(not(containsString("Owner"))))
                .andExpect(content().string(containsString("invite-owner@test.com")))
                .andExpect(content().string(containsString("Вас пригласил invite-owner@test.com")))
                .andExpect(content().string(containsString("Invite Team")));
    }

    @Test
    void registrationPageShouldShowInvitationContext() throws Exception {
        TeamInvitation invitation = createInvitation("registration-context@test.com");
        String redirect = "/invitations/" + invitation.getToken();

        mockMvc.perform(get("/registration").param("redirect", redirect).param("invite", invitation.getToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Owner"))))
                .andExpect(content().string(containsString("invite-owner@test.com")))
                .andExpect(content().string(containsString("Вас пригласил invite-owner@test.com")))
                .andExpect(content().string(containsString("Invite Team")))
                .andExpect(content().string(containsString("href=\"/login?redirect=")))
                .andExpect(content().string(containsString("invite=" + invitation.getToken())));
    }

    @Test
    void registrationValidationErrorShouldKeepInvitationContext() throws Exception {
        TeamInvitation invitation = createInvitation("registration-error@test.com");
        String redirect = "/invitations/" + invitation.getToken();

        String registrationRedirect =
                "/registration?redirect=/invitations/" + invitation.getToken() + "&invite=" + invitation.getToken();
        MvcResult result = mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("email", "wrong-email")
                        .param("password", PASSWORD)
                        .param("redirect", redirect)
                        .param("invite", invitation.getToken()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(registrationRedirect))
                .andExpect(flash().attribute("errorMessage", "Email имеет неправильный формат."))
                .andReturn();

        mockMvc.perform(get(registrationRedirect)
                        .session((MockHttpSession) result.getRequest().getSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Email имеет неправильный формат.")))
                .andExpect(content().string(not(containsString("Owner"))))
                .andExpect(content().string(containsString("invite-owner@test.com")))
                .andExpect(content().string(containsString("Вас пригласил invite-owner@test.com")))
                .andExpect(content().string(containsString("Invite Team")));
    }

    @Test
    void invitationContextShouldSurviveRegistrationVerificationAndLogin() throws Exception {
        TeamInvitation invitation = createInvitation("new-invited-user@test.com");
        String redirect = "/invitations/" + invitation.getToken();

        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("email", invitation.getInvitedEmail())
                        .param("name", "New invited user")
                        .param("password", PASSWORD)
                        .param("redirect", redirect)
                        .param("invite", invitation.getToken()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/verification-pending"));

        User createdUser = userRepository.findByEmail(invitation.getInvitedEmail());
        final String verificationToken = captureVerificationToken();

        assertNotNull(createdUser);
        assertFalse(createdUser.isEmailVerified());
        assertFalse(teamMemberService.isActiveMember(invitation.getTeamId(), createdUser.getId()));
        assertEquals(
                TeamInvitationStatus.PENDING,
                teamInvitationService.findByToken(invitation.getToken()).getStatus());

        String loginUrl = "/login?redirect=/invitations/" + invitation.getToken() + "&invite=" + invitation.getToken();
        mockMvc.perform(get("/verify-email/" + verificationToken).param("invite", invitation.getToken()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(loginUrl))
                .andExpect(flash().attribute("successMessage", "Email подтверждён. Теперь вы можете войти."));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", invitation.getInvitedEmail())
                        .param("password", PASSWORD)
                        .param("redirect", redirect)
                        .param("invite", invitation.getToken()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(redirect));

        assertFalse(teamMemberService.isActiveMember(invitation.getTeamId(), createdUser.getId()));
        assertEquals(
                TeamInvitationStatus.PENDING,
                teamInvitationService.findByToken(invitation.getToken()).getStatus());
    }

    @Test
    void verificationShouldConfirmUserAndRejectRepeatedUse() throws Exception {
        registerUser(EMAIL);
        String verificationToken = captureVerificationToken();

        mockMvc.perform(get("/verify-email/" + verificationToken))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("successMessage", "Email подтверждён. Теперь вы можете войти."));

        User verifiedUser = userRepository.findByEmail(EMAIL);

        assertTrue(verifiedUser.isEmailVerified());
        assertNotNull(verifiedUser.getEmailVerifiedAt());

        mockMvc.perform(get("/verify-email/" + verificationToken))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("errorMessage", "Ссылка подтверждения недействительна или устарела."));
    }

    @Test
    void verificationShouldRejectUnknownAndExpiredTokens() throws Exception {
        registerUser(EMAIL);
        String verificationToken = captureVerificationToken();
        dsl.update(ACCOUNT_TOKENS)
                .set(ACCOUNT_TOKENS.EXPIRES_AT, OffsetDateTime.now().minusMinutes(1))
                .execute();

        mockMvc.perform(get("/verify-email/" + verificationToken))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("errorMessage", "Ссылка подтверждения недействительна или устарела."));

        mockMvc.perform(get("/verify-email/unknown-token"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("errorMessage", "Ссылка подтверждения недействительна или устарела."));
    }

    @Test
    void resendShouldInvalidatePreviousTokenAfterCooldown() throws Exception {
        registerUser(EMAIL);
        dsl.update(ACCOUNT_TOKENS)
                .set(ACCOUNT_TOKENS.CREATED_AT, OffsetDateTime.now().minusMinutes(2))
                .execute();
        reset(emailSender);

        mockMvc.perform(post("/resend-verification").with(csrf()).param("email", EMAIL))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/verification-pending"))
                .andExpect(flash().attribute(
                                "successMessage",
                                "Если аккаунт с таким email существует, мы отправили письмо для подтверждения."));

        List<AccountTokensRecord> tokens =
                dsl.selectFrom(ACCOUNT_TOKENS).orderBy(ACCOUNT_TOKENS.ID.asc()).fetch();

        assertEquals(2, tokens.size());
        assertNotNull(tokens.getFirst().getUsedAt());
        assertFalse(tokens.getLast().getTokenHash().isBlank());
        verify(emailSender)
                .send(
                        org.mockito.ArgumentMatchers.eq(EMAIL),
                        org.mockito.ArgumentMatchers.eq("Подтвердите email в Task Me Please"),
                        anyString());
    }

    @Test
    void resendShouldNotCreateAnotherTokenDuringCooldown() throws Exception {
        registerUser(EMAIL);
        reset(emailSender);

        mockMvc.perform(post("/resend-verification").with(csrf()).param("email", EMAIL))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/verification-pending"))
                .andExpect(flash().attribute(
                                "successMessage",
                                "Если аккаунт с таким email существует, мы отправили письмо для подтверждения."));

        assertEquals(1, dsl.fetchCount(ACCOUNT_TOKENS));
        verify(emailSender, org.mockito.Mockito.never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void resendShouldAllowOnlyFiveAttemptsWithinTwentyFourHours() throws Exception {
        MvcResult registrationResult = mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("email", EMAIL)
                        .param("name", "Auth user")
                        .param("password", PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/verification-pending"))
                .andReturn();
        MockHttpSession session =
                (MockHttpSession) registrationResult.getRequest().getSession();

        for (int attempt = 0; attempt < 5; attempt++) {
            dsl.update(ACCOUNT_TOKENS)
                    .set(ACCOUNT_TOKENS.CREATED_AT, OffsetDateTime.now().minusMinutes(2))
                    .execute();
            mockMvc.perform(post("/resend-verification")
                            .with(csrf())
                            .session(session)
                            .param("email", EMAIL))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("/verification-pending"));
        }

        dsl.update(ACCOUNT_TOKENS)
                .set(ACCOUNT_TOKENS.CREATED_AT, OffsetDateTime.now().minusMinutes(2))
                .execute();
        mockMvc.perform(post("/resend-verification")
                        .with(csrf())
                        .session(session)
                        .param("email", EMAIL))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/verification-pending"));

        assertEquals(6, dsl.fetchCount(ACCOUNT_TOKENS));

        mockMvc.perform(get("/verification-pending").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-remaining-attempts>0</span>")))
                .andExpect(content().string(containsString("<button class=\"auth-submit\" type=\"submit\" disabled>")));
    }

    @Test
    void resendShouldBeNeutralForUnknownAndVerifiedEmails() throws Exception {
        createVerifiedUser(EMAIL);

        mockMvc.perform(post("/resend-verification").with(csrf()).param("email", "unknown@test.com"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/verification-pending"))
                .andExpect(flash().attribute(
                                "successMessage",
                                "Если аккаунт с таким email существует, мы отправили письмо для подтверждения."));

        mockMvc.perform(post("/resend-verification").with(csrf()).param("email", EMAIL))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/verification-pending"))
                .andExpect(flash().attribute(
                                "successMessage",
                                "Если аккаунт с таким email существует, мы отправили письмо для подтверждения."));

        verify(emailSender, org.mockito.Mockito.never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void resendShouldRequireCsrf() throws Exception {
        mockMvc.perform(post("/resend-verification").param("email", EMAIL)).andExpect(status().isForbidden());
    }

    @Test
    void smtpFailureShouldKeepRegisteredUserAndVerificationToken() throws Exception {
        doThrow(new TransactionalEmailSendingException(new RuntimeException()))
                .when(emailSender)
                .send(anyString(), anyString(), anyString());

        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("email", EMAIL)
                        .param("name", "Auth user")
                        .param("password", PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/verification-pending"));

        User user = userRepository.findByEmail(EMAIL);
        AccountTokensRecord token = dsl.selectFrom(ACCOUNT_TOKENS)
                .where(ACCOUNT_TOKENS.USER_ID.eq(user.getId()))
                .fetchOne();

        assertNotNull(user);
        assertFalse(user.isEmailVerified());
        assertNotNull(token);
    }

    @Test
    void authPagesShouldIgnoreUnsafeReturnPath() throws Exception {
        mockMvc.perform(get("/login").param("redirect", "//evil.test/path").param("invite", "token-123"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"redirect\""))))
                .andExpect(content().string(containsString("name=\"invite\" value=\"token-123\"")));
    }

    @Test
    void authenticatedPageShouldShowLogoutButton() throws Exception {
        User user = userService.create(EMAIL, "Auth user", PASSWORD);

        mockMvc.perform(get("/tasks").with(user(new AuthUser(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("title=\"Ваш профиль\"")))
                .andExpect(content().string(containsString("data-profile-initial>A</span>")))
                .andExpect(content().string(containsString("Auth user")))
                .andExpect(content().string(containsString(EMAIL)))
                .andExpect(content().string(containsString("Настройки пока не реализованы")))
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(containsString("Выйти из профиля")));
    }

    @Test
    void authenticatedPageShouldUseEmailForProfileFallback() throws Exception {
        User user = userService.create(EMAIL, null, PASSWORD);

        mockMvc.perform(get("/tasks").with(user(new AuthUser(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-profile-initial>A</span>")))
                .andExpect(content().string(containsString("data-profile-display-name")))
                .andExpect(content().string(containsString(EMAIL)))
                .andExpect(content().string(not(containsString("data-profile-email"))));
    }

    @Test
    void staticImagesShouldBeAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/images/favicon.svg")).andExpect(status().isOk());
    }

    @Test
    void logoutShouldRedirectToLogin() throws Exception {
        User user = userService.create(EMAIL, "Auth user", PASSWORD);

        mockMvc.perform(post("/logout").with(user(new AuthUser(user))).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    private TeamInvitation createInvitation(String invitedEmail) {
        User owner = userService.create("invite-owner@test.com", "Owner", PASSWORD);
        Team team = teamService.create("Invite Team", owner.getId());

        return teamInvitationService.create(team.getId(), invitedEmail, owner.getId());
    }

    private void registerUser(String email) throws Exception {
        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("email", email)
                        .param("name", "Auth user")
                        .param("password", PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/verification-pending"));
    }

    private String captureVerificationToken() {
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(anyString(), anyString(), textCaptor.capture());
        String text = textCaptor.getValue();
        String pathPrefix = "/verify-email/";
        int tokenStart = text.indexOf(pathPrefix) + pathPrefix.length();
        int tokenEnd = text.indexOf('?', tokenStart);
        if (tokenEnd < 0) {
            tokenEnd = text.indexOf('\n', tokenStart);
        }

        return text.substring(tokenStart, tokenEnd);
    }

    private User createVerifiedUser(String email) {
        User user = userService.create(email, "Auth user", PASSWORD);
        OffsetDateTime verifiedAt = OffsetDateTime.now();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(verifiedAt.toInstant());

        return userRepository.update(user);
    }

    private Cookie loginWithRememberMe() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", EMAIL)
                        .param("password", PASSWORD)
                        .param("remember-me", "on"))
                .andExpect(status().isFound())
                .andReturn();

        return loginResult.getResponse().getCookie("remember-me");
    }

    private void cleanDatabase() {
        dsl.deleteFrom(PERSISTENT_LOGINS).execute();
        dsl.deleteFrom(ACCOUNT_TOKENS).execute();
        dsl.deleteFrom(COMMENTS).execute();
        dsl.deleteFrom(TASKS).execute();
        dsl.deleteFrom(TEAM_INVITATIONS).execute();
        dsl.deleteFrom(TEAM_TAGS).execute();
        dsl.deleteFrom(TEAM_MEMBERS).execute();
        dsl.deleteFrom(TEAMS).execute();
        dsl.deleteFrom(USERS).execute();
    }
}
