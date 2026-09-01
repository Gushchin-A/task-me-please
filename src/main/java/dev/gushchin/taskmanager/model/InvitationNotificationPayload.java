package dev.gushchin.taskmanager.model;

public record InvitationNotificationPayload(String actorName, String teamName, String invitedEmail)
        implements NotificationPayload {}
