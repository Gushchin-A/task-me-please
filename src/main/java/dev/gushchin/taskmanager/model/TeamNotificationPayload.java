package dev.gushchin.taskmanager.model;

public record TeamNotificationPayload(
        String actorName, String teamName, String subjectName, String previousValue, String newValue)
        implements NotificationPayload {}
