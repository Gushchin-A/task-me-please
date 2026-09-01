package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.NotificationEvent;
import dev.gushchin.taskmanager.model.NotificationEventType;
import dev.gushchin.taskmanager.model.UserNotification;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record NotificationFeedItem(
        UserNotification notification,
        NotificationEvent event,
        String message,
        NotificationInvitationAction invitationAction) {
    private static final long MINUTES_IN_HOUR = 60;
    private static final long MINUTES_IN_DAY = MINUTES_IN_HOUR * 24;
    private static final long ONE_MINUTE = 1;
    private static final String TASK_URL_PREFIX = "/tasks/";

    public NotificationFeedItem(UserNotification notification, NotificationEvent event) {
        this(notification, event, null, null);
    }

    public String relativeTime() {
        long minutes =
                Duration.between(notification.getCreatedAt(), Instant.now()).toMinutes();
        if (minutes < ONE_MINUTE) {
            return "только что";
        }
        if (minutes < MINUTES_IN_HOUR) {
            return minutes + " мин назад";
        }

        long hours = minutes / MINUTES_IN_HOUR;
        if (minutes < MINUTES_IN_DAY) {
            return hours + " ч назад";
        }

        return minutes / MINUTES_IN_DAY + " дн назад";
    }

    public String destinationUrl() {
        NotificationEventType type = event.getType();
        return switch (type) {
            case COMMENT_CREATED, COMMENT_UPDATED -> commentDestinationUrl();
            case COMMENT_DELETED -> taskDestinationUrl();
            case TASK_CREATED,
                    TASK_STATUS_CHANGED,
                    TASK_AUTHOR_CHANGED,
                    TASK_ASSIGNEE_CHANGED,
                    TASK_TITLE_CHANGED,
                    TASK_DESCRIPTION_CHANGED,
                    TASK_DEADLINE_CHANGED,
                    TASK_TAG_CHANGED,
                    TASK_ARCHIVED,
                    TASK_RESTORED -> taskDestinationUrl();
            case TEAM_INVITATION_CREATED,
                    TEAM_INVITATION_RESENT,
                    TEAM_INVITATION_ACCEPTED,
                    TEAM_INVITATION_DECLINED,
                    TEAM_INVITATION_CANCELED,
                    TEAM_INVITATION_EXPIRED,
                    TEAM_DELETED -> null;
            case TEAM_MEMBER_REMOVED, TEAM_MEMBER_LEFT ->
                unavailableTeamForRecipient(type) ? null : teamDestinationUrl();
            case TEAM_CREATED,
                    TEAM_RENAMED,
                    TEAM_MEMBER_JOINED,
                    TEAM_TASK_VISIBILITY_GRANTED,
                    TEAM_TASK_VISIBILITY_RESTRICTED,
                    TEAM_TAG_CREATED,
                    TEAM_TAG_RENAMED,
                    TEAM_TAG_DELETED -> teamDestinationUrl();
        };
    }

    public List<NotificationMessagePart> messageParts() {
        List<NotificationMessagePart> parts = new ArrayList<>();
        int position = 0;

        while (position < message.length()) {
            int openingQuote = message.indexOf('«', position);
            if (openingQuote < 0) {
                parts.add(new NotificationMessagePart(message.substring(position), false));
                break;
            }

            int closingQuote = message.indexOf('»', openingQuote + 1);
            if (closingQuote < 0) {
                parts.add(new NotificationMessagePart(message.substring(position), false));
                break;
            }
            if (openingQuote > position) {
                parts.add(new NotificationMessagePart(message.substring(position, openingQuote), false));
            }
            parts.add(new NotificationMessagePart(message.substring(openingQuote, closingQuote + 1), true));
            position = closingQuote + 1;
        }

        return List.copyOf(parts);
    }

    private String commentDestinationUrl() {
        String taskUrl = taskDestinationUrl();
        return taskUrl == null || event.getCommentId() == null ? taskUrl : taskUrl + "#comment-" + event.getCommentId();
    }

    private String taskDestinationUrl() {
        return event.getTaskId() == null ? null : TASK_URL_PREFIX + event.getTaskId();
    }

    private String teamDestinationUrl() {
        return event.getTeamId() == null ? null : "/teams/" + event.getTeamId();
    }

    private boolean unavailableTeamForRecipient(NotificationEventType type) {
        return (type == NotificationEventType.TEAM_MEMBER_REMOVED || type == NotificationEventType.TEAM_MEMBER_LEFT)
                && notification.getRecipientUserId().equals(event.getSubjectUserId());
    }
}
