package dev.gushchin.taskmanager.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserNotification {
    private Long id;
    private Long notificationEventId;
    private UUID recipientUserId;
    private String recipientEmail;
    private Instant readAt;
    private Instant createdAt;

    public boolean isRead() {
        return readAt != null;
    }
}
