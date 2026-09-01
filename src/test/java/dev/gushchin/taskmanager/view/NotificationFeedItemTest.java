package dev.gushchin.taskmanager.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.gushchin.taskmanager.model.NotificationEvent;
import dev.gushchin.taskmanager.model.NotificationEventType;
import dev.gushchin.taskmanager.model.UserNotification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationFeedItemTest {
    private static final UUID RECIPIENT_ID = UUID.randomUUID();

    @Test
    void taskNotificationShouldLinkToTask() {
        NotificationFeedItem item = item(NotificationEventType.TASK_CREATED, 10L, null, 20L, null);

        assertEquals("/tasks/20", item.destinationUrl());
    }

    @Test
    void commentNotificationShouldLinkToCommentOnTask() {
        NotificationFeedItem item = item(NotificationEventType.COMMENT_CREATED, 10L, null, 20L, 30L);

        assertEquals("/tasks/20#comment-30", item.destinationUrl());
    }

    @Test
    void deletedCommentNotificationShouldLinkToTask() {
        NotificationFeedItem item = item(NotificationEventType.COMMENT_DELETED, 10L, null, 20L, 30L);

        assertEquals("/tasks/20", item.destinationUrl());
    }

    @Test
    void teamNotificationShouldLinkToTeam() {
        NotificationFeedItem item = item(NotificationEventType.TEAM_TAG_CREATED, 10L, null, null, null);

        assertEquals("/teams/10", item.destinationUrl());
    }

    @Test
    void removedMemberNotificationShouldNotLinkToUnavailableTeam() {
        NotificationFeedItem item = item(NotificationEventType.TEAM_MEMBER_REMOVED, 10L, RECIPIENT_ID, null, null);

        assertNull(item.destinationUrl());
    }

    @Test
    void deletedTeamNotificationShouldNotHaveDestination() {
        NotificationFeedItem item = item(NotificationEventType.TEAM_DELETED, 10L, null, null, null);

        assertNull(item.destinationUrl());
    }

    @Test
    void messagePartsShouldEmphasizeEveryQuotedEntity() {
        NotificationFeedItem original = item(NotificationEventType.TASK_TITLE_CHANGED, 10L, null, 20L, null);
        NotificationFeedItem item = new NotificationFeedItem(
                original.notification(), original.event(), "Название задачи «Старое» изменено на «Новое»", null);

        assertEquals(
                List.of(
                        new NotificationMessagePart("Название задачи ", false),
                        new NotificationMessagePart("«Старое»", true),
                        new NotificationMessagePart(" изменено на ", false),
                        new NotificationMessagePart("«Новое»", true)),
                item.messageParts());
    }

    private NotificationFeedItem item(
            NotificationEventType type, Long teamId, UUID subjectUserId, Long taskId, Long commentId) {
        Instant createdAt = Instant.now();
        NotificationEvent event = new NotificationEvent(
                1L, type, UUID.randomUUID(), teamId, taskId, commentId, null, subjectUserId, "{}", createdAt);
        UserNotification notification = new UserNotification(1L, 1L, RECIPIENT_ID, null, null, createdAt);

        return new NotificationFeedItem(notification, event, "Сообщение", null);
    }
}
