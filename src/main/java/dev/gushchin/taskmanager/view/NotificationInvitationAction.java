package dev.gushchin.taskmanager.view;

public record NotificationInvitationAction(String token, boolean expired) {
    public static NotificationInvitationAction active(String token) {
        return new NotificationInvitationAction(token, false);
    }

    public static NotificationInvitationAction expiredState() {
        return new NotificationInvitationAction(null, true);
    }

    public boolean active() {
        return token != null;
    }
}
