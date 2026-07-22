package dev.gushchin.taskmanager.controller;

import static dev.gushchin.taskmanager.jooq.Tables.COMMENTS;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.UserRepository;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.TeamInvitationService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.UserService;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
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

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @Test
    void loginPageShouldBeAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Task Me Please")))
                .andExpect(content().string(containsString("action=\"/login\"")))
                .andExpect(content().string(containsString("name=\"username\"")));
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
        userService.create(EMAIL, "Auth user", PASSWORD);

        mockMvc.perform(post("/login").with(csrf()).param("username", EMAIL).param("password", PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/tasks"));
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
    void registrationShouldCreateUser() throws Exception {
        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("email", EMAIL)
                        .param("name", "Auth user")
                        .param("password", PASSWORD))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/tasks"));

        User user = userRepository.findByEmail(EMAIL);

        assertNotNull(user);
    }

    @Test
    void registrationShouldRedirectToSafeReturnPathAfterSuccess() throws Exception {
        String redirect = "/invitations/token-123";

        mockMvc.perform(post("/registration")
                        .with(csrf())
                        .param("email", EMAIL)
                        .param("name", "Auth user")
                        .param("password", PASSWORD)
                        .param("redirect", redirect)
                        .param("invite", "token-123"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(redirect));
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
                .andExpect(content().string(containsString("Owner")))
                .andExpect(content().string(containsString("invite-owner@test.com")))
                .andExpect(content().string(containsString("пригласил вас")))
                .andExpect(content().string(containsString("Invite Team")))
                .andExpect(content().string(containsString("href=\"/registration?redirect=")))
                .andExpect(content().string(containsString("invite=" + invitation.getToken())));
    }

    @Test
    void loginShouldRedirectToSafeReturnPathAfterSuccess() throws Exception {
        String redirect = "/invitations/token-123";
        userService.create(EMAIL, "Auth user", PASSWORD);

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
                .andExpect(content().string(containsString("Owner")))
                .andExpect(content().string(containsString("invite-owner@test.com")))
                .andExpect(content().string(containsString("пригласил вас")))
                .andExpect(content().string(containsString("Invite Team")));
    }

    @Test
    void registrationPageShouldShowInvitationContext() throws Exception {
        TeamInvitation invitation = createInvitation("registration-context@test.com");
        String redirect = "/invitations/" + invitation.getToken();

        mockMvc.perform(get("/registration").param("redirect", redirect).param("invite", invitation.getToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Owner")))
                .andExpect(content().string(containsString("invite-owner@test.com")))
                .andExpect(content().string(containsString("пригласил вас")))
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
                .andExpect(content().string(containsString("Owner")))
                .andExpect(content().string(containsString("invite-owner@test.com")))
                .andExpect(content().string(containsString("пригласил вас")))
                .andExpect(content().string(containsString("Invite Team")));
    }

    @Test
    void registrationShouldReturnToInvitationWithoutJoiningTeam() throws Exception {
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
                .andExpect(redirectedUrl(redirect));

        User createdUser = userRepository.findByEmail(invitation.getInvitedEmail());

        assertNotNull(createdUser);
        assertFalse(teamMemberService.isActiveMember(invitation.getTeamId(), createdUser.getId()));
        assertEquals(
                TeamInvitationStatus.PENDING,
                teamInvitationService.findByToken(invitation.getToken()).getStatus());
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
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(containsString("Выйти из профиля")));
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

    private void cleanDatabase() {
        dsl.deleteFrom(COMMENTS).execute();
        dsl.deleteFrom(TASKS).execute();
        dsl.deleteFrom(TEAM_INVITATIONS).execute();
        dsl.deleteFrom(TEAM_TAGS).execute();
        dsl.deleteFrom(TEAM_MEMBERS).execute();
        dsl.deleteFrom(TEAMS).execute();
        dsl.deleteFrom(USERS).execute();
    }
}
