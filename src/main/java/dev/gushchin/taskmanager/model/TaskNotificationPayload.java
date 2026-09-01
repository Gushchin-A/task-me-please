package dev.gushchin.taskmanager.model;

import java.util.UUID;

public record TaskNotificationPayload(
        String actorName,
        String teamName,
        String taskTitle,
        String previousValue,
        String newValue,
        UUID previousUserId,
        UUID newUserId,
        TaskParticipantIds participants)
        implements NotificationPayload {}
