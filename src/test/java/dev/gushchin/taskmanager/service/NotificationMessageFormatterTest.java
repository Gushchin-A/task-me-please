package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gushchin.taskmanager.model.CommentNotificationPayload;
import dev.gushchin.taskmanager.model.InvitationNotificationPayload;
import dev.gushchin.taskmanager.model.NotificationEvent;
import dev.gushchin.taskmanager.model.NotificationEventType;
import dev.gushchin.taskmanager.model.TaskNotificationPayload;
import dev.gushchin.taskmanager.model.TeamNotificationPayload;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationMessageFormatterTest {
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID RECIPIENT_ID = UUID.randomUUID();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationMessageFormatter formatter = new NotificationMessageFormatter(objectMapper);

    @Test
    void taskCreatedShouldUseAgreedTextForOtherRecipients() throws Exception {
        TaskNotificationPayload payload =
                new TaskNotificationPayload("Анна", "Команда", "Подготовить отчет", null, null, null, null, null);
        NotificationEvent event = event(NotificationEventType.TASK_CREATED, payload);

        assertEquals("Анна создал задачу «Подготовить отчет»", formatter.format(event, RECIPIENT_ID));
    }

    @Test
    void taskCreatedShouldUseLocativeCaseForTeam() throws Exception {
        TaskNotificationPayload payload = new TaskNotificationPayload(
                "Анна", "Редакция продуктового блога", "Написать заметку", null, null, null, null, null);
        NotificationEvent event = event(NotificationEventType.TASK_CREATED, payload);

        assertEquals(
                "Вы создали задачу «Написать заметку» в команде «Редакция продуктового блога»",
                formatter.format(event, ACTOR_ID));
    }

    @Test
    void taskStatusChangedShouldNameActorForOtherRecipients() throws Exception {
        TaskNotificationPayload payload = new TaskNotificationPayload(
                "Анна", "Команда", "Подготовить отчет", "Новая", "В работе", null, null, null);
        NotificationEvent event = event(NotificationEventType.TASK_STATUS_CHANGED, payload);

        assertEquals(
                "Анна изменил статус задачи «Подготовить отчет»: Новая → В работе",
                formatter.format(event, RECIPIENT_ID));
    }

    @Test
    void taskStatusChangedShouldTranslateLegacySnapshotValues() throws Exception {
        TaskNotificationPayload payload = new TaskNotificationPayload(
                "Анна", "Команда", "Подготовить отчет", "OPEN", "IN_PROGRESS", null, null, null);
        NotificationEvent event = event(NotificationEventType.TASK_STATUS_CHANGED, payload);

        assertEquals(
                "Анна изменил статус задачи «Подготовить отчет»: Открыто → В работе",
                formatter.format(event, RECIPIENT_ID));
    }

    @Test
    void taskDeadlineChangedShouldTranslateLegacySnapshotDates() throws Exception {
        TaskNotificationPayload payload = new TaskNotificationPayload(
                "Анна", "Команда", "Подготовить отчет", "2026-09-08", "2026-09-09", null, null, null);
        NotificationEvent event = event(NotificationEventType.TASK_DEADLINE_CHANGED, payload);

        assertEquals(
                "Дедлайн задачи «Подготовить отчет» изменен: 8 сентября 2026 → 9 сентября 2026",
                formatter.format(event, RECIPIENT_ID));
    }

    @Test
    void taskArchivedShouldDescribeUserFacingDeletionForActor() throws Exception {
        TaskNotificationPayload payload =
                new TaskNotificationPayload("Анна", "Команда", "Подготовить отчет", null, null, null, null, null);
        NotificationEvent event = event(NotificationEventType.TASK_ARCHIVED, payload);

        assertEquals("Вы удалили задачу «Подготовить отчет»", formatter.format(event, ACTOR_ID));
    }

    @Test
    void taskArchivedShouldDescribeUserFacingDeletionForOtherRecipients() throws Exception {
        TaskNotificationPayload payload =
                new TaskNotificationPayload("Анна", "Команда", "Подготовить отчет", null, null, null, null, null);
        NotificationEvent event = event(NotificationEventType.TASK_ARCHIVED, payload);

        assertEquals("Задача «Подготовить отчет» удалена пользователем Анна", formatter.format(event, RECIPIENT_ID));
    }

    @Test
    void completedTaskArchivedShouldDescribeArchiveForActor() throws Exception {
        TaskNotificationPayload payload =
                new TaskNotificationPayload("Анна", "Команда", "Подготовить отчет", "Готово", null, null, null, null);
        NotificationEvent event = event(NotificationEventType.TASK_ARCHIVED, payload);

        assertEquals("Вы перенесли в архив задачу «Подготовить отчет»", formatter.format(event, ACTOR_ID));
    }

    @Test
    void completedTaskArchivedShouldDescribeArchiveForOtherRecipients() throws Exception {
        TaskNotificationPayload payload =
                new TaskNotificationPayload("Анна", "Команда", "Подготовить отчет", "DONE", null, null, null, null);
        NotificationEvent event = event(NotificationEventType.TASK_ARCHIVED, payload);

        assertEquals(
                "Задача «Подготовить отчет» перенесена в архив пользователем Анна",
                formatter.format(event, RECIPIENT_ID));
    }

    @Test
    void commentUpdatedShouldUseAgreedTextForOtherRecipients() throws Exception {
        CommentNotificationPayload payload = new CommentNotificationPayload("Анна", "Команда", "Задача");
        NotificationEvent event = event(NotificationEventType.COMMENT_UPDATED, payload);

        assertEquals("Пользователь Анна изменил комментарий к задаче «Задача»", formatter.format(event, RECIPIENT_ID));
    }

    @Test
    void invitationAcceptedShouldUseEmailForOwner() throws Exception {
        InvitationNotificationPayload payload =
                new InvitationNotificationPayload("Анна", "Команда", "anna@example.com");
        NotificationEvent event = event(NotificationEventType.TEAM_INVITATION_ACCEPTED, payload);

        assertEquals("anna@example.com принял приглашение в команду «Команда»", formatter.format(event, RECIPIENT_ID));
    }

    @Test
    void invitationDeclinedShouldUseEmailForOwner() throws Exception {
        InvitationNotificationPayload payload =
                new InvitationNotificationPayload("Анна", "Команда", "anna@example.com");
        NotificationEvent event = event(NotificationEventType.TEAM_INVITATION_DECLINED, payload);

        assertEquals(
                "anna@example.com отклонил приглашение в команду «Команда»", formatter.format(event, RECIPIENT_ID));
    }

    @Test
    void teamMemberJoinedShouldUseAgreedTextForOtherMembers() throws Exception {
        TeamNotificationPayload payload = new TeamNotificationPayload("Анна", "Команда", "Анна", null, null);
        NotificationEvent event = event(NotificationEventType.TEAM_MEMBER_JOINED, payload, ACTOR_ID);

        assertEquals("Анна присоединился к команде «Команда»", formatter.format(event, RECIPIENT_ID));
    }

    @Test
    void teamMemberJoinedShouldUseAgreedTextForJoinedMember() throws Exception {
        TeamNotificationPayload payload = new TeamNotificationPayload("Анна", "Команда", "Анна", null, null);
        NotificationEvent event = event(NotificationEventType.TEAM_MEMBER_JOINED, payload, RECIPIENT_ID);

        assertEquals("Вы присоединились к команде «Команда»", formatter.format(event, RECIPIENT_ID));
    }

    @Test
    void teamMemberRemovedShouldUseAgreedTextForOtherMembers() throws Exception {
        UUID subjectUserId = UUID.randomUUID();
        TeamNotificationPayload payload = new TeamNotificationPayload("Владелец", "Команда", "Анна", null, null);
        NotificationEvent event = event(NotificationEventType.TEAM_MEMBER_REMOVED, payload, subjectUserId);

        assertEquals("Анна удален из команды «Команда»", formatter.format(event, RECIPIENT_ID));
    }

    @Test
    void visibilityGrantedShouldUseVisibilityTextForOwner() throws Exception {
        UUID subjectUserId = UUID.randomUUID();
        TeamNotificationPayload payload = new TeamNotificationPayload("Владелец", "Команда", "Чукарипа", null, null);
        NotificationEvent event = event(NotificationEventType.TEAM_TASK_VISIBILITY_GRANTED, payload, subjectUserId);

        assertEquals(
                "Вы открыли пользователю Чукарипа видимость всех задач в команде «Команда»",
                formatter.format(event, ACTOR_ID));
    }

    @Test
    void visibilityGrantedShouldUseVisibilityTextForMember() throws Exception {
        TeamNotificationPayload payload = new TeamNotificationPayload("Владелец", "Команда", "Чукарипа", null, null);
        NotificationEvent event = event(NotificationEventType.TEAM_TASK_VISIBILITY_GRANTED, payload, RECIPIENT_ID);

        assertEquals("Вам открыли видимость всех задач в команде «Команда»", formatter.format(event, RECIPIENT_ID));
    }

    @Test
    void visibilityRestrictedShouldUseVisibilityTextForOwner() throws Exception {
        UUID subjectUserId = UUID.randomUUID();
        TeamNotificationPayload payload = new TeamNotificationPayload("Владелец", "Команда", "Чукарипа", null, null);
        NotificationEvent event = event(NotificationEventType.TEAM_TASK_VISIBILITY_RESTRICTED, payload, subjectUserId);

        assertEquals(
                "Вы ограничили пользователю Чукарипа видимость задач в команде «Команда»",
                formatter.format(event, ACTOR_ID));
    }

    @Test
    void visibilityRestrictedShouldUseVisibilityTextForMember() throws Exception {
        TeamNotificationPayload payload = new TeamNotificationPayload("Владелец", "Команда", "Чукарипа", null, null);
        NotificationEvent event = event(NotificationEventType.TEAM_TASK_VISIBILITY_RESTRICTED, payload, RECIPIENT_ID);

        assertEquals(
                "Вам ограничили видимость задач. Теперь вам видны только ваши задачи в команде «Команда»",
                formatter.format(event, RECIPIENT_ID));
    }

    private NotificationEvent event(NotificationEventType type, Object payload) throws Exception {
        return event(type, payload, null);
    }

    private NotificationEvent event(NotificationEventType type, Object payload, UUID subjectUserId) throws Exception {
        return new NotificationEvent(
                1L,
                type,
                ACTOR_ID,
                1L,
                1L,
                1L,
                1L,
                subjectUserId,
                objectMapper.writeValueAsString(payload),
                Instant.now());
    }
}
