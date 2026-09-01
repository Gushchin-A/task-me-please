package dev.gushchin.taskmanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gushchin.taskmanager.model.CommentNotificationPayload;
import dev.gushchin.taskmanager.model.InvitationNotificationPayload;
import dev.gushchin.taskmanager.model.NotificationEvent;
import dev.gushchin.taskmanager.model.TaskNotificationPayload;
import dev.gushchin.taskmanager.model.TeamNotificationPayload;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationMessageFormatter {
    private static final DateTimeFormatter DEADLINE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"));
    private static final String ARROW = " → ";
    private static final String CHANGE_SEPARATOR = "»: ";
    private static final String COMPLETED_STATUS = "DONE";
    private static final String COMPLETED_STATUS_DISPLAY_NAME = "Готово";
    private static final String IN_TEAM_PREFIX = " в команду «";
    private static final String WITHIN_TEAM_PREFIX = " в команде «";
    private static final String NEW_NAME_PREFIX = "». Новое название «";
    private static final String RESTORED_SUFFIX = "» из архива";
    private static final String TASK_ASSIGNEE_PREFIX = "Вас назначили исполнителем задачи «";
    private static final String TASK_PREFIX = "В задаче «";
    private static final String TASK_SUBJECT_PREFIX = "Задача «";
    private static final String TEAM_PREFIX = "Команда «";
    private static final String TEAM_REMOVAL_PREFIX = "Из команды «";
    private static final String USER_PREFIX = "Пользователь ";

    private final ObjectMapper objectMapper;

    public String format(NotificationEvent event, UUID recipientUserId) {
        return switch (event.getType()) {
            case COMMENT_CREATED, COMMENT_UPDATED, COMMENT_DELETED ->
                formatComment(event, recipientUserId, readPayload(event, CommentNotificationPayload.class));
            case TEAM_INVITATION_CREATED,
                    TEAM_INVITATION_RESENT,
                    TEAM_INVITATION_ACCEPTED,
                    TEAM_INVITATION_DECLINED,
                    TEAM_INVITATION_CANCELED,
                    TEAM_INVITATION_EXPIRED ->
                formatInvitation(event, recipientUserId, readPayload(event, InvitationNotificationPayload.class));
            case TASK_CREATED,
                    TASK_STATUS_CHANGED,
                    TASK_AUTHOR_CHANGED,
                    TASK_ASSIGNEE_CHANGED,
                    TASK_TITLE_CHANGED,
                    TASK_DESCRIPTION_CHANGED,
                    TASK_DEADLINE_CHANGED,
                    TASK_TAG_CHANGED,
                    TASK_ARCHIVED,
                    TASK_RESTORED ->
                formatTask(event, recipientUserId, readPayload(event, TaskNotificationPayload.class));
            case TEAM_CREATED,
                    TEAM_RENAMED,
                    TEAM_DELETED,
                    TEAM_MEMBER_JOINED,
                    TEAM_MEMBER_REMOVED,
                    TEAM_MEMBER_LEFT,
                    TEAM_TASK_VISIBILITY_GRANTED,
                    TEAM_TASK_VISIBILITY_RESTRICTED,
                    TEAM_TAG_CREATED,
                    TEAM_TAG_RENAMED,
                    TEAM_TAG_DELETED ->
                formatTeam(event, recipientUserId, readPayload(event, TeamNotificationPayload.class));
        };
    }

    private String formatComment(NotificationEvent event, UUID recipientUserId, CommentNotificationPayload payload) {
        boolean actor = isActor(event, recipientUserId);

        return switch (event.getType()) {
            case COMMENT_CREATED ->
                actor
                        ? "Вы оставили комментарий к задаче «" + payload.taskTitle() + "»"
                        : USER_PREFIX + payload.actorName() + " оставил комментарий к задаче «" + payload.taskTitle()
                                + "»";
            case COMMENT_UPDATED ->
                actor
                        ? "Вы изменили комментарий к задаче «" + payload.taskTitle() + "»"
                        : USER_PREFIX + payload.actorName() + " изменил комментарий к задаче «" + payload.taskTitle()
                                + "»";
            case COMMENT_DELETED ->
                actor
                        ? "Вы удалили комментарий к задаче «" + payload.taskTitle() + "»"
                        : USER_PREFIX + payload.actorName() + " удалил комментарий к задаче «" + payload.taskTitle()
                                + "»";
            default -> throw new IllegalArgumentException("Unsupported comment notification type");
        };
    }

    private String formatInvitation(
            NotificationEvent event, UUID recipientUserId, InvitationNotificationPayload payload) {
        boolean actor = isActor(event, recipientUserId);

        return switch (event.getType()) {
            case TEAM_INVITATION_CREATED ->
                actor
                        ? "Вы пригласили " + payload.invitedEmail() + IN_TEAM_PREFIX + payload.teamName() + "»"
                        : "Вас пригласили" + IN_TEAM_PREFIX + payload.teamName() + "»";
            case TEAM_INVITATION_RESENT ->
                "Вы повторно отправили приглашение " + payload.invitedEmail() + IN_TEAM_PREFIX + payload.teamName()
                        + "»";
            case TEAM_INVITATION_ACCEPTED ->
                actor
                        ? "Вы приняли приглашение" + IN_TEAM_PREFIX + payload.teamName() + "»"
                        : payload.invitedEmail() + " принял приглашение" + IN_TEAM_PREFIX + payload.teamName() + "»";
            case TEAM_INVITATION_DECLINED ->
                actor
                        ? "Вы отклонили приглашение" + IN_TEAM_PREFIX + payload.teamName() + "»"
                        : payload.invitedEmail() + " отклонил приглашение" + IN_TEAM_PREFIX + payload.teamName() + "»";
            case TEAM_INVITATION_CANCELED ->
                "Вы отменили приглашение " + payload.invitedEmail() + IN_TEAM_PREFIX + payload.teamName() + "»";
            case TEAM_INVITATION_EXPIRED ->
                "Срок действия приглашения " + payload.invitedEmail() + IN_TEAM_PREFIX + payload.teamName() + "» истек";
            default -> throw new IllegalArgumentException("Unsupported invitation notification type");
        };
    }

    private String formatTask(NotificationEvent event, UUID recipientUserId, TaskNotificationPayload payload) {
        return switch (event.getType()) {
            case TASK_CREATED -> formatTaskCreated(event, recipientUserId, payload);
            case TASK_STATUS_CHANGED -> formatTaskStatus(event, recipientUserId, payload);
            case TASK_AUTHOR_CHANGED -> formatAuthorChange(event, recipientUserId, payload);
            case TASK_ASSIGNEE_CHANGED -> formatAssigneeChange(event, recipientUserId, payload);
            case TASK_TITLE_CHANGED -> formatTaskTitle(event, recipientUserId, payload);
            case TASK_DESCRIPTION_CHANGED -> formatTaskDescription(event, recipientUserId, payload);
            case TASK_DEADLINE_CHANGED -> formatTaskDeadline(event, recipientUserId, payload);
            case TASK_TAG_CHANGED -> formatTaskTag(event, recipientUserId, payload);
            case TASK_ARCHIVED -> formatTaskArchived(event, recipientUserId, payload);
            case TASK_RESTORED -> formatTaskRestored(event, recipientUserId, payload);
            default -> throw new IllegalArgumentException("Unsupported task notification type");
        };
    }

    private String formatTaskCreated(NotificationEvent event, UUID recipientUserId, TaskNotificationPayload payload) {
        if (isActor(event, recipientUserId)) {
            return "Вы создали задачу «" + payload.taskTitle() + "»" + WITHIN_TEAM_PREFIX + payload.teamName() + "»";
        }
        if (payload.participants() != null
                && recipientUserId.equals(payload.participants().assigneeUserId())) {
            return TASK_ASSIGNEE_PREFIX + payload.taskTitle() + "»";
        }

        return payload.actorName() + " создал задачу «" + payload.taskTitle() + "»";
    }

    private String formatTaskStatus(NotificationEvent event, UUID recipientUserId, TaskNotificationPayload payload) {
        String previousStatus = formatLegacyStatus(payload.previousValue());
        String newStatus = formatLegacyStatus(payload.newValue());
        if (isActor(event, recipientUserId)) {
            return "Вы изменили статус задачи «" + payload.taskTitle() + CHANGE_SEPARATOR + previousStatus + ARROW
                    + newStatus;
        }

        return payload.actorName() + " изменил статус задачи «" + payload.taskTitle() + CHANGE_SEPARATOR
                + previousStatus + ARROW + newStatus;
    }

    private String formatAuthorChange(NotificationEvent event, UUID recipientUserId, TaskNotificationPayload payload) {
        if (isActor(event, recipientUserId)) {
            return "Вы изменили автора в задаче «" + payload.taskTitle() + CHANGE_SEPARATOR + payload.previousValue()
                    + ARROW + payload.newValue();
        }
        if (recipientUserId.equals(payload.newUserId())) {
            return "Вы назначены автором задачи «" + payload.taskTitle() + "»";
        }
        if (recipientUserId.equals(payload.previousUserId())) {
            return "Вы больше не являетесь автором задачи «" + payload.taskTitle() + "»";
        }

        return TASK_PREFIX + payload.taskTitle() + "» изменен автор: " + payload.previousValue() + ARROW
                + payload.newValue();
    }

    private String formatAssigneeChange(
            NotificationEvent event, UUID recipientUserId, TaskNotificationPayload payload) {
        if (isActor(event, recipientUserId)) {
            return "Вы изменили исполнителя в задаче «" + payload.taskTitle() + CHANGE_SEPARATOR
                    + payload.previousValue() + ARROW + payload.newValue();
        }
        if (recipientUserId.equals(payload.newUserId())) {
            return TASK_ASSIGNEE_PREFIX + payload.taskTitle() + "»";
        }
        if (recipientUserId.equals(payload.previousUserId())) {
            return "Вы больше не являетесь исполнителем задачи «" + payload.taskTitle() + "»";
        }

        return TASK_PREFIX + payload.taskTitle() + "» изменен исполнитель: " + payload.previousValue() + ARROW
                + payload.newValue();
    }

    private String formatTaskTitle(NotificationEvent event, UUID recipientUserId, TaskNotificationPayload payload) {
        if (isActor(event, recipientUserId)) {
            return "Вы изменили название задачи «" + payload.previousValue() + NEW_NAME_PREFIX + payload.newValue()
                    + "»";
        }

        return "Название задачи «" + payload.previousValue() + "» изменено на «" + payload.newValue() + "»";
    }

    private String formatTaskDescription(
            NotificationEvent event, UUID recipientUserId, TaskNotificationPayload payload) {
        return isActor(event, recipientUserId)
                ? "Вы изменили описание задачи «" + payload.taskTitle() + "»"
                : "Описание задачи «" + payload.taskTitle() + "» изменено";
    }

    private String formatTaskDeadline(NotificationEvent event, UUID recipientUserId, TaskNotificationPayload payload) {
        String previousDeadline = formatLegacyDeadline(payload.previousValue());
        String newDeadline = formatLegacyDeadline(payload.newValue());
        return isActor(event, recipientUserId)
                ? "Вы изменили дедлайн задачи «" + payload.taskTitle() + CHANGE_SEPARATOR + newDeadline
                : "Дедлайн задачи «" + payload.taskTitle() + "» изменен: " + previousDeadline + ARROW + newDeadline;
    }

    private String formatLegacyStatus(String status) {
        return switch (status) {
            case "OPEN" -> "Открыто";
            case "IN_PROGRESS" -> "В работе";
            case COMPLETED_STATUS -> COMPLETED_STATUS_DISPLAY_NAME;
            case "NOT_RELEVANT" -> "Неактуально";
            default -> status;
        };
    }

    private String formatLegacyDeadline(String deadline) {
        if (deadline == null) {
            return null;
        }

        try {
            return LocalDate.parse(deadline).format(DEADLINE_FORMATTER);
        } catch (DateTimeParseException exception) {
            return deadline;
        }
    }

    private String formatTaskTag(NotificationEvent event, UUID recipientUserId, TaskNotificationPayload payload) {
        String change = payload.previousValue() + ARROW + payload.newValue();
        return isActor(event, recipientUserId)
                ? "Вы изменили тег у задачи «" + payload.taskTitle() + CHANGE_SEPARATOR + change
                : "Тег у задачи «" + payload.taskTitle() + "» был изменен: " + change;
    }

    private String formatTaskArchived(NotificationEvent event, UUID recipientUserId, TaskNotificationPayload payload) {
        if (isCompletedStatus(payload.previousValue())) {
            return isActor(event, recipientUserId)
                    ? "Вы перенесли в архив задачу «" + payload.taskTitle() + "»"
                    : TASK_SUBJECT_PREFIX + payload.taskTitle() + "» перенесена в архив пользователем "
                            + payload.actorName();
        }

        return isActor(event, recipientUserId)
                ? "Вы удалили задачу «" + payload.taskTitle() + "»"
                : TASK_SUBJECT_PREFIX + payload.taskTitle() + "» удалена пользователем " + payload.actorName();
    }

    private boolean isCompletedStatus(String status) {
        return COMPLETED_STATUS.equals(status) || COMPLETED_STATUS_DISPLAY_NAME.equals(status);
    }

    private String formatTaskRestored(NotificationEvent event, UUID recipientUserId, TaskNotificationPayload payload) {
        return isActor(event, recipientUserId)
                ? "Вы восстановили задачу «" + payload.taskTitle() + RESTORED_SUFFIX
                : payload.actorName() + " восстановил задачу «" + payload.taskTitle() + RESTORED_SUFFIX;
    }

    private String formatTeam(NotificationEvent event, UUID recipientUserId, TeamNotificationPayload payload) {
        return switch (event.getType()) {
            case TEAM_CREATED -> "Вы создали команду «" + payload.teamName() + "»";
            case TEAM_RENAMED -> formatTeamRenamed(event, recipientUserId, payload);
            case TEAM_DELETED -> formatTeamDeleted(event, recipientUserId, payload);
            case TEAM_MEMBER_JOINED -> formatTeamMemberJoined(event, recipientUserId, payload);
            case TEAM_MEMBER_REMOVED -> formatTeamMemberRemoved(event, recipientUserId, payload);
            case TEAM_MEMBER_LEFT -> formatTeamMemberLeft(event, recipientUserId, payload);
            case TEAM_TASK_VISIBILITY_GRANTED -> formatVisibilityGranted(event, recipientUserId, payload);
            case TEAM_TASK_VISIBILITY_RESTRICTED -> formatVisibilityRestricted(event, recipientUserId, payload);
            case TEAM_TAG_CREATED -> formatTagCreated(event, recipientUserId, payload);
            case TEAM_TAG_RENAMED -> formatTagRenamed(event, recipientUserId, payload);
            case TEAM_TAG_DELETED -> formatTagDeleted(event, recipientUserId, payload);
            default -> throw new IllegalArgumentException("Unsupported team notification type");
        };
    }

    private String formatTeamRenamed(NotificationEvent event, UUID recipientUserId, TeamNotificationPayload payload) {
        return isActor(event, recipientUserId)
                ? "Вы переименовали команду «" + payload.previousValue() + NEW_NAME_PREFIX + payload.newValue() + "»"
                : TEAM_PREFIX + payload.previousValue() + "» переименована пользователем " + payload.actorName()
                        + " в «" + payload.newValue() + "»";
    }

    private String formatTeamDeleted(NotificationEvent event, UUID recipientUserId, TeamNotificationPayload payload) {
        return isActor(event, recipientUserId)
                ? "Вы удалили команду «" + payload.teamName() + "»"
                : TEAM_PREFIX + payload.teamName() + "» удалена. У вас больше нет доступа к ней";
    }

    private String formatTeamMemberJoined(
            NotificationEvent event, UUID recipientUserId, TeamNotificationPayload payload) {
        return recipientUserId.equals(event.getSubjectUserId())
                ? "Вы присоединились к команде «" + payload.teamName() + "»"
                : payload.subjectName() + " присоединился к команде «" + payload.teamName() + "»";
    }

    private String formatTeamMemberRemoved(
            NotificationEvent event, UUID recipientUserId, TeamNotificationPayload payload) {
        if (isActor(event, recipientUserId)) {
            return "Вы удалили " + payload.subjectName() + " из команды «" + payload.teamName() + "»";
        }
        if (recipientUserId.equals(event.getSubjectUserId())) {
            return "Вы удалены из команды «" + payload.teamName() + "»";
        }

        return payload.subjectName() + " удален из команды «" + payload.teamName() + "»";
    }

    private String formatTeamMemberLeft(
            NotificationEvent event, UUID recipientUserId, TeamNotificationPayload payload) {
        return recipientUserId.equals(event.getSubjectUserId())
                ? "Вы покинули команду «" + payload.teamName() + "»"
                : payload.subjectName() + " покинул команду «" + payload.teamName() + "»";
    }

    private String formatVisibilityGranted(
            NotificationEvent event, UUID recipientUserId, TeamNotificationPayload payload) {
        return recipientUserId.equals(event.getSubjectUserId())
                ? "Вам открыли видимость всех задач в команде «" + payload.teamName() + "»"
                : "Вы открыли пользователю " + payload.subjectName() + " видимость всех задач в команде «"
                        + payload.teamName() + "»";
    }

    private String formatVisibilityRestricted(
            NotificationEvent event, UUID recipientUserId, TeamNotificationPayload payload) {
        return recipientUserId.equals(event.getSubjectUserId())
                ? "Вам ограничили видимость задач. Теперь вам видны только ваши задачи в команде «" + payload.teamName()
                        + "»"
                : "Вы ограничили пользователю " + payload.subjectName() + " видимость задач в команде «"
                        + payload.teamName() + "»";
    }

    private String formatTagCreated(NotificationEvent event, UUID recipientUserId, TeamNotificationPayload payload) {
        return isActor(event, recipientUserId)
                ? "Вы создали тег «" + payload.newValue() + "» в команде «" + payload.teamName() + "»"
                : "В команде «" + payload.teamName() + "» создан новый тег «" + payload.newValue() + "»";
    }

    private String formatTagRenamed(NotificationEvent event, UUID recipientUserId, TeamNotificationPayload payload) {
        return isActor(event, recipientUserId)
                ? "Вы переименовали тег «" + payload.previousValue() + "» в «" + payload.newValue() + "»"
                : "Тег «" + payload.previousValue() + "» был переименован в «" + payload.newValue() + "»";
    }

    private String formatTagDeleted(NotificationEvent event, UUID recipientUserId, TeamNotificationPayload payload) {
        return isActor(event, recipientUserId)
                ? "Вы удалили тег «" + payload.previousValue() + "» из команды «" + payload.teamName() + "»"
                : TEAM_REMOVAL_PREFIX + payload.teamName() + "» удален тег «" + payload.previousValue() + "»";
    }

    private <T> T readPayload(NotificationEvent event, Class<T> type) {
        try {
            return objectMapper.readValue(event.getPayload(), type);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Notification payload cannot be read", ex);
        }
    }

    private boolean isActor(NotificationEvent event, UUID recipientUserId) {
        return Objects.equals(event.getActorUserId(), recipientUserId);
    }
}
