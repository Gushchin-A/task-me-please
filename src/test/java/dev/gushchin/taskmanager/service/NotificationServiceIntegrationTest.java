package dev.gushchin.taskmanager.service;

import static dev.gushchin.taskmanager.jooq.Tables.NOTIFICATION_EVENTS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static dev.gushchin.taskmanager.jooq.Tables.USER_NOTIFICATIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.model.CommentNotificationPayload;
import dev.gushchin.taskmanager.model.InvitationNotificationPayload;
import dev.gushchin.taskmanager.model.NotificationEvent;
import dev.gushchin.taskmanager.model.NotificationEventCommand;
import dev.gushchin.taskmanager.model.NotificationEventType;
import dev.gushchin.taskmanager.model.NotificationPayload;
import dev.gushchin.taskmanager.model.NotificationRecipients;
import dev.gushchin.taskmanager.model.NotificationSort;
import dev.gushchin.taskmanager.model.TaskNotificationPayload;
import dev.gushchin.taskmanager.model.TeamNotificationPayload;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.model.UserNotification;
import dev.gushchin.taskmanager.repository.UserNotificationRepository;
import dev.gushchin.taskmanager.view.NotificationCounts;
import dev.gushchin.taskmanager.view.NotificationPage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class NotificationServiceIntegrationTest extends IntegrationTestBase {
    private static final String PASSWORD = "qwerty";

    @Autowired
    private DSLContext dsl;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

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
    void findPageShouldUseStableCursorForNewestAndOldestSorting() {
        User recipient = createUser("recipient@test.com", "Recipient");
        Instant firstCreatedAt = Instant.now().minusSeconds(100).truncatedTo(ChronoUnit.MICROS);

        for (int index = 0; index < 27; index++) {
            notificationService.publish(
                    createEvent(NotificationEventType.TASK_CREATED, firstCreatedAt.plusSeconds(index)),
                    recipients(Set.of(recipient.getId()), Set.of()));
        }

        NotificationPage newestFirstPage =
                notificationService.findPage(recipient.getId(), false, NotificationSort.NEWEST, null);
        NotificationPage newestSecondPage = notificationService.findPage(
                recipient.getId(), false, NotificationSort.NEWEST, newestFirstPage.nextCursor());
        NotificationPage oldestFirstPage =
                notificationService.findPage(recipient.getId(), false, NotificationSort.OLDEST, null);
        final NotificationPage oldestSecondPage = notificationService.findPage(
                recipient.getId(), false, NotificationSort.OLDEST, oldestFirstPage.nextCursor());
        long firstNotificationId =
                oldestFirstPage.items().getFirst().notification().getId();

        assertEquals(NotificationService.PAGE_SIZE, newestFirstPage.items().size());
        assertEquals(2, newestSecondPage.items().size());
        assertEquals(
                firstNotificationId + 26,
                newestFirstPage.items().getFirst().notification().getId());
        assertEquals(
                firstNotificationId + 2,
                newestFirstPage.items().getLast().notification().getId());
        assertEquals(
                firstNotificationId + 1,
                newestSecondPage.items().getFirst().notification().getId());
        assertNull(newestSecondPage.nextCursor());
        assertEquals(
                firstNotificationId + 24,
                oldestFirstPage.items().getLast().notification().getId());
        assertEquals(
                firstNotificationId + 25,
                oldestSecondPage.items().getFirst().notification().getId());
        assertNull(oldestSecondPage.nextCursor());
    }

    @Test
    void markAsReadShouldUpdateOnlySelectedNotificationsOfCurrentUser() {
        User firstUser = createUser("first@test.com", "First");
        User secondUser = createUser("second@test.com", "Second");
        NotificationEvent event = notificationService.publish(
                createEvent(NotificationEventType.TASK_STATUS_CHANGED, Instant.now()),
                recipients(Set.of(firstUser.getId(), secondUser.getId()), Set.of()));
        List<UserNotification> firstUserNotifications = userNotificationRepository.findByUserId(firstUser.getId());
        List<UserNotification> secondUserNotifications = userNotificationRepository.findByUserId(secondUser.getId());

        int updatedCount = notificationService.markAsRead(
                firstUser.getId(),
                List.of(
                        firstUserNotifications.getFirst().getId(),
                        secondUserNotifications.getFirst().getId()));

        NotificationCounts firstCounts = notificationService.getCounts(firstUser.getId());
        final NotificationCounts secondCounts = notificationService.getCounts(secondUser.getId());
        assertNotNull(event.getId());
        assertEquals(1, updatedCount);
        assertEquals(new NotificationCounts(1, 0), firstCounts);
        assertEquals(new NotificationCounts(1, 1), secondCounts);
        assertTrue(userNotificationRepository
                .findByUserId(firstUser.getId())
                .getFirst()
                .isRead());
        assertFalse(userNotificationRepository
                .findByUserId(secondUser.getId())
                .getFirst()
                .isRead());
        assertEquals(
                0,
                notificationService.markAsRead(
                        firstUser.getId(),
                        List.of(firstUserNotifications.getFirst().getId())));
    }

    @Test
    void claimInvitationsShouldAttachNormalizedEmailDeliveriesToUser() {
        User recipient = createUser("invited@test.com", "Invited");
        notificationService.publish(
                createEvent(NotificationEventType.TEAM_INVITATION_CREATED, Instant.now()),
                recipients(Set.of(), Set.of("  INVITED@test.com  ")));

        assertTrue(userNotificationRepository.findByUserId(recipient.getId()).isEmpty());

        notificationService.claimInvitations(recipient.getId(), "INVITED@test.com");

        List<UserNotification> notifications = userNotificationRepository.findByUserId(recipient.getId());
        assertEquals(1, notifications.size());
        assertEquals(recipient.getId(), notifications.getFirst().getRecipientUserId());
        assertNull(notifications.getFirst().getRecipientEmail());
    }

    @Test
    void publishShouldDeduplicateRecipients() {
        User recipient = createUser("recipient@test.com", "Recipient");
        Set<UUID> recipients = new LinkedHashSet<>();
        recipients.add(recipient.getId());
        recipients.add(recipient.getId());

        notificationService.publish(
                createEvent(NotificationEventType.COMMENT_CREATED, Instant.now()), recipients(recipients, Set.of()));

        assertEquals(
                1, userNotificationRepository.findByUserId(recipient.getId()).size());
    }

    @Test
    void publishShouldRollBackEventAndDeliveriesWhenAnyRecipientIsInvalid() {
        User recipient = createUser("recipient@test.com", "Recipient");
        Set<UUID> recipients = new LinkedHashSet<>();
        recipients.add(recipient.getId());
        recipients.add(UUID.randomUUID());

        assertThrows(
                DataIntegrityViolationException.class,
                () -> notificationService.publish(
                        createEvent(NotificationEventType.TASK_CREATED, Instant.now()),
                        recipients(recipients, Set.of())));

        assertEquals(0, dsl.fetchCount(NOTIFICATION_EVENTS));
        assertEquals(0, dsl.fetchCount(USER_NOTIFICATIONS));
    }

    private NotificationEventCommand createEvent(NotificationEventType type, Instant createdAt) {
        return NotificationEventCommand.builder()
                .type(type)
                .payload(payloadFor(type))
                .createdAt(createdAt)
                .build();
    }

    private NotificationPayload payloadFor(NotificationEventType type) {
        return switch (type) {
            case COMMENT_CREATED, COMMENT_UPDATED, COMMENT_DELETED ->
                new CommentNotificationPayload("Actor", "Team", "Task");
            case TEAM_INVITATION_CREATED,
                    TEAM_INVITATION_RESENT,
                    TEAM_INVITATION_ACCEPTED,
                    TEAM_INVITATION_DECLINED,
                    TEAM_INVITATION_CANCELED,
                    TEAM_INVITATION_EXPIRED -> new InvitationNotificationPayload("Actor", "Team", "user@test.com");
            case TASK_CREATED,
                    TASK_STATUS_CHANGED,
                    TASK_AUTHOR_CHANGED,
                    TASK_ASSIGNEE_CHANGED,
                    TASK_TITLE_CHANGED,
                    TASK_DESCRIPTION_CHANGED,
                    TASK_DEADLINE_CHANGED,
                    TASK_TAG_CHANGED,
                    TASK_ARCHIVED,
                    TASK_RESTORED -> new TaskNotificationPayload("Actor", "Team", "Task", null, null, null, null, null);
            default -> new TeamNotificationPayload("Actor", "Team", null, null, null);
        };
    }

    private NotificationRecipients recipients(Set<UUID> userIds, Set<String> emails) {
        return new NotificationRecipients(userIds, emails);
    }

    private User createUser(String email, String name) {
        return userService.create(email, name, PASSWORD);
    }

    private void cleanDatabase() {
        dsl.deleteFrom(USER_NOTIFICATIONS).execute();
        dsl.deleteFrom(NOTIFICATION_EVENTS).execute();
        dsl.deleteFrom(USERS).execute();
    }
}
