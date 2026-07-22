package dev.gushchin.taskmanager.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.UserRepository;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.UserService;
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
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
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
        String redirect = "/teams/invitations/accept?token=token-123";

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
        String redirect = "/teams/invitations/accept?token=token-123";

        mockMvc.perform(get("/login").param("redirect", redirect).param("invite", "token-123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"redirect\" value=\"" + redirect + "\"")))
                .andExpect(content().string(containsString("name=\"invite\" value=\"token-123\"")))
                .andExpect(content().string(containsString("href=\"/registration?redirect=")))
                .andExpect(content().string(containsString("invite=token-123")));
    }

    @Test
    void loginShouldRedirectToSafeReturnPathAfterSuccess() throws Exception {
        String redirect = "/teams/invitations/accept?token=token-123";
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
}
