package dev.gushchin.taskmanager.model;

public sealed interface NotificationPayload
        permits CommentNotificationPayload,
                InvitationNotificationPayload,
                TaskNotificationPayload,
                TeamNotificationPayload {}
