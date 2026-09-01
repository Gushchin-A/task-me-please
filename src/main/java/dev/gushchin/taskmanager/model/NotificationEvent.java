package dev.gushchin.taskmanager.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class NotificationEvent {
    private Long id;
    private NotificationEventType type;
    private UUID actorUserId;
    private Long teamId;
    private Long taskId;
    private Long commentId;
    private Long invitationId;
    private UUID subjectUserId;
    private String payload;
    private Instant createdAt;
}
