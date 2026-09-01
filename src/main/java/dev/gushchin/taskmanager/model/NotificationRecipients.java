package dev.gushchin.taskmanager.model;

import java.util.Set;
import java.util.UUID;

public record NotificationRecipients(Set<UUID> userIds, Set<String> emails) {
    public NotificationRecipients {
        userIds = Set.copyOf(userIds);
        emails = Set.copyOf(emails);
    }
}
