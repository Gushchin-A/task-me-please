package dev.gushchin.taskmanager.controller;

import static dev.gushchin.taskmanager.jooq.Tables.COMMENTS;
import static dev.gushchin.taskmanager.jooq.Tables.NOTIFICATION_EVENTS;
import static dev.gushchin.taskmanager.jooq.Tables.TASKS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAMS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_INVITATIONS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_MEMBERS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_TAGS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static dev.gushchin.taskmanager.jooq.Tables.USER_NOTIFICATIONS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.model.NotificationEventType;
import dev.gushchin.taskmanager.model.NotificationSort;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamRepository;
import dev.gushchin.taskmanager.repository.UserRepository;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.NotificationService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.UserService;
import dev.gushchin.taskmanager.view.NotificationPage;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class AccountSettingsControllerIntegrationTest extends IntegrationTestBase {
    private static final String PASSWORD = "qwerty";

    @Autowired
    private DSLContext dsl;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        currentUser = userService.create("settings@test.com", "Старое имя", PASSWORD);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void settingsPageShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/settings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void settingsPageShouldRenderProfileFormAndSidebar() throws Exception {
        mockMvc.perform(get("/settings").with(user(new AuthUser(currentUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Настройки профиля")))
                .andExpect(content().string(containsString("Удаление аккаунта")))
                .andExpect(content().string(containsString("value=\"Старое имя\"")))
                .andExpect(content().string(containsString("data-tooltip=\"Начните редактировать\"")))
                .andExpect(content().string(containsString("data-settings-rename-submit")));
    }

    @Test
    void deleteSectionShouldRenderAccountWarning() throws Exception {
        mockMvc.perform(get("/settings?section=delete").with(user(new AuthUser(currentUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("После удаления аккаунта вы потеряете доступ")))
                .andExpect(content().string(containsString("data-confirmation-text=\"я хочу удалить аккаунт\"")))
                .andExpect(content().string(not(containsString("Кроме аккаунта будут также удалены все команды"))));
    }

    @Test
    void updateNameShouldOnlyChangeAuthenticatedUser() throws Exception {
        User otherUser = userService.create("other-settings@test.com", "Другое имя", PASSWORD);

        mockMvc.perform(post("/settings/name")
                        .with(user(new AuthUser(currentUser)))
                        .with(csrf())
                        .param("name", "  Новое имя  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"))
                .andExpect(flash().attribute("successMessage", "Имя успешно изменено"));

        assertEquals("Новое имя", userRepository.findById(currentUser.getId()).getName());
        assertEquals("Другое имя", userRepository.findById(otherUser.getId()).getName());
    }

    @Test
    void updateNameShouldAllowEmptyValueAndRenderEmailFallback() throws Exception {
        mockMvc.perform(post("/settings/name")
                        .with(user(new AuthUser(currentUser)))
                        .with(csrf())
                        .param("name", "   "))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/settings").with(user(new AuthUser(currentUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(currentUser.getEmail())));
    }

    @Test
    void deleteAccountShouldRequireExactConfirmation() throws Exception {
        mockMvc.perform(post("/settings/delete")
                        .with(user(new AuthUser(currentUser)))
                        .with(csrf())
                        .param("confirmationText", "удалить аккаунт"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings?section=delete"))
                .andExpect(flash().attribute("errorMessage", "Проверочный текст введен неверно"));

        assertFalse(userRepository.findById(currentUser.getId()).isDeleted());
    }

    @Test
    void deleteAccountShouldDeleteAllOwnedTeamsAndNotifyEachMember() throws Exception {
        User member = userService.create("account-member@test.com", "Участник", PASSWORD);
        Team firstTeam = teamService.create("Первая команда", currentUser.getId());
        Team secondTeam = teamService.create("Вторая команда", currentUser.getId());
        teamMemberService.addMember(firstTeam.getId(), member.getId());
        teamMemberService.addMember(secondTeam.getId(), member.getId());

        mockMvc.perform(get("/settings?section=delete").with(user(new AuthUser(currentUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Кроме аккаунта будут также удалены все команды")))
                .andExpect(content()
                        .string(containsString("data-confirmation-text=\"я хочу удалить аккаунт и команды\"")));

        mockMvc.perform(post("/settings/delete")
                        .with(user(new AuthUser(currentUser)))
                        .with(csrf())
                        .param("confirmationText", "я хочу удалить аккаунт и команды"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertTrue(userRepository.findById(currentUser.getId()).isDeleted());
        assertTrue(teamRepository.findById(firstTeam.getId()).isDeleted());
        assertTrue(teamRepository.findById(secondTeam.getId()).isDeleted());

        NotificationPage notifications =
                notificationService.findPage(member.getId(), false, NotificationSort.NEWEST, null);
        long deletedTeamNotifications = notifications.items().stream()
                .filter(item -> item.event().getType() == NotificationEventType.TEAM_DELETED)
                .count();
        assertEquals(2, deletedTeamNotifications);
    }

    @Test
    void deleteAccountWithoutOwnedTeamsShouldUseShortConfirmation() throws Exception {
        mockMvc.perform(post("/settings/delete")
                        .with(user(new AuthUser(currentUser)))
                        .with(csrf())
                        .param("confirmationText", "я хочу удалить аккаунт"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertTrue(userRepository.findById(currentUser.getId()).isDeleted());
    }

    @Test
    void deleteAccountShouldAllowRegisteringWithSameEmailAgain() throws Exception {
        mockMvc.perform(post("/settings/delete")
                        .with(user(new AuthUser(currentUser)))
                        .with(csrf())
                        .param("confirmationText", "я хочу удалить аккаунт"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        User registeredAgain = userService.create(currentUser.getEmail(), "Новое имя", PASSWORD);
        User deletedUser = userRepository.findById(currentUser.getId());

        assertEquals(currentUser.getEmail(), registeredAgain.getEmail());
        assertTrue(deletedUser.isDeleted());
        assertTrue(deletedUser.getEmail().endsWith("@deleted.taskmeplease.invalid"));
    }

    @Test
    void deleteAccountShouldRequireCsrf() throws Exception {
        mockMvc.perform(post("/settings/delete")
                        .with(user(new AuthUser(currentUser)))
                        .param("confirmationText", "я хочу удалить аккаунт и команды"))
                .andExpect(status().isForbidden());

        assertFalse(userRepository.findById(currentUser.getId()).isDeleted());
    }

    private void cleanDatabase() {
        dsl.deleteFrom(USER_NOTIFICATIONS).execute();
        dsl.deleteFrom(NOTIFICATION_EVENTS).execute();
        dsl.deleteFrom(COMMENTS).execute();
        dsl.deleteFrom(TASKS).execute();
        dsl.deleteFrom(TEAM_INVITATIONS).execute();
        dsl.deleteFrom(TEAM_TAGS).execute();
        dsl.deleteFrom(TEAM_MEMBERS).execute();
        dsl.deleteFrom(TEAMS).execute();
        dsl.deleteFrom(USERS).execute();
    }
}
