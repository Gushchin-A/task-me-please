package dev.gushchin.taskmanager.controller;

import static dev.gushchin.taskmanager.jooq.Tables.NOTIFICATION_EVENTS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static dev.gushchin.taskmanager.jooq.Tables.USER_NOTIFICATIONS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.model.NotificationEvent;
import dev.gushchin.taskmanager.model.NotificationEventCommand;
import dev.gushchin.taskmanager.model.NotificationEventType;
import dev.gushchin.taskmanager.model.NotificationRecipients;
import dev.gushchin.taskmanager.model.TaskNotificationPayload;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.UserNotificationRepository;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.NotificationService;
import dev.gushchin.taskmanager.service.UserService;
import java.time.Instant;
import java.util.Set;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class NotificationPageControllerIntegrationTest extends IntegrationTestBase {
    private static final String PASSWORD = "qwerty";

    @Autowired
    private DSLContext dsl;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private UserService userService;

    private User firstUser;
    private User secondUser;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        firstUser = userService.create("notifications-first@test.com", "Первый пользователь", PASSWORD);
        secondUser = userService.create("notifications-second@test.com", "Второй пользователь", PASSWORD);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void notificationsPageShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void notificationsPageShouldRenderPersonalizedRowsAndCounts() throws Exception {
        publishTaskNotification(Set.of(firstUser.getId()), firstUser);

        mockMvc.perform(get("/notifications").with(user(new AuthUser(firstUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Уведомления (1)")))
                .andExpect(content().string(containsString("Вы создали задачу «Тестовая задача»")))
                .andExpect(content().string(containsString("form=\"notifications-read-form\"")));
    }

    @Test
    void emptyNotificationsPagesShouldHideCollectionControls() throws Exception {
        mockMvc.perform(get("/notifications").with(user(new AuthUser(firstUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"empty-state notification-empty-state\"")))
                .andExpect(content().string(containsString("Уведомлений нет")))
                .andExpect(content().string(not(containsString("Уведомления (0)"))))
                .andExpect(content().string(not(containsString("Сортировка"))))
                .andExpect(content().string(not(containsString("Настройки уведомлений"))));

        mockMvc.perform(get("/notifications?unread=true").with(user(new AuthUser(firstUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Непрочитанных уведомлений нет")))
                .andExpect(content().string(not(containsString("Уведомления (0)"))))
                .andExpect(content().string(not(containsString("Сортировка"))));
    }

    @Test
    void markAsReadShouldOnlyUpdateCurrentUsersNotification() throws Exception {
        publishTaskNotification(Set.of(firstUser.getId(), secondUser.getId()), firstUser);
        Long firstNotificationId = userNotificationRepository
                .findByUserId(firstUser.getId())
                .getFirst()
                .getId();
        Long secondNotificationId = userNotificationRepository
                .findByUserId(secondUser.getId())
                .getFirst()
                .getId();

        mockMvc.perform(post("/notifications/read")
                        .with(user(new AuthUser(firstUser)))
                        .with(csrf())
                        .param("notificationIds", firstNotificationId.toString(), secondNotificationId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        assertEquals(0, notificationService.getCounts(firstUser.getId()).unread());
        assertEquals(1, notificationService.getCounts(secondUser.getId()).unread());
    }

    @Test
    void markAsReadWithoutSelectionShouldReturnErrorFlash() throws Exception {
        mockMvc.perform(post("/notifications/read")
                        .with(user(new AuthUser(firstUser)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attribute(
                                "errorMessage", "Не удалось отметить уведомления как прочитанные. Попробуйте еще раз"));
    }

    @Test
    void markAsReadShouldPreserveUnreadFilterAndSort() throws Exception {
        publishTaskNotification(Set.of(firstUser.getId()), firstUser);
        Long notificationId = userNotificationRepository
                .findByUserId(firstUser.getId())
                .getFirst()
                .getId();

        mockMvc.perform(post("/notifications/read")
                        .with(user(new AuthUser(firstUser)))
                        .with(csrf())
                        .param("notificationIds", notificationId.toString())
                        .param("unread", "true")
                        .param("sort", "OLDEST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications?unread=true&sort=OLDEST"));
    }

    @Test
    void markAsReadOnOpenShouldOnlyUpdateCurrentUsersNotification() throws Exception {
        publishTaskNotification(Set.of(firstUser.getId(), secondUser.getId()), firstUser);
        Long firstNotificationId = userNotificationRepository
                .findByUserId(firstUser.getId())
                .getFirst()
                .getId();
        Long secondNotificationId = userNotificationRepository
                .findByUserId(secondUser.getId())
                .getFirst()
                .getId();

        mockMvc.perform(multipart("/notifications/read-on-open")
                        .with(user(new AuthUser(firstUser)))
                        .with(csrf())
                        .param("notificationId", firstNotificationId.toString()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/notifications/read-on-open")
                        .with(user(new AuthUser(firstUser)))
                        .with(csrf())
                        .param("notificationId", secondNotificationId.toString()))
                .andExpect(status().isNoContent());

        assertEquals(0, notificationService.getCounts(firstUser.getId()).unread());
        assertEquals(1, notificationService.getCounts(secondUser.getId()).unread());
    }

    private NotificationEvent publishTaskNotification(Set<java.util.UUID> recipients, User actor) {
        NotificationEventCommand command = NotificationEventCommand.builder()
                .type(NotificationEventType.TASK_CREATED)
                .actorUserId(actor.getId())
                .payload(new TaskNotificationPayload(
                        actor.getName(), "Тестовая команда", "Тестовая задача", null, null, null, null, null))
                .createdAt(Instant.now())
                .build();
        return notificationService.publish(command, new NotificationRecipients(recipients, Set.of()));
    }

    private void cleanDatabase() {
        dsl.deleteFrom(USER_NOTIFICATIONS).execute();
        dsl.deleteFrom(NOTIFICATION_EVENTS).execute();
        dsl.deleteFrom(USERS).execute();
    }
}
