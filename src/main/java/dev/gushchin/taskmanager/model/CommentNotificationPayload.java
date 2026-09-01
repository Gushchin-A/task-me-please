package dev.gushchin.taskmanager.model;

public record CommentNotificationPayload(String actorName, String teamName, String taskTitle)
        implements NotificationPayload {}
