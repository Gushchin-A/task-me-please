package dev.gushchin.taskmanager.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class NotificationEventCommand {
    private final NotificationEventType type;
    private final UUID actorUserId;
    private final Long teamId;
    private final Long taskId;
    private final Long commentId;
    private final Long invitationId;
    private final UUID subjectUserId;
    private final NotificationPayload payload;
    private final Instant createdAt;
}
