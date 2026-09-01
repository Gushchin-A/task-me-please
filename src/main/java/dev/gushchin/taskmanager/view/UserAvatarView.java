package dev.gushchin.taskmanager.view;

import java.util.Locale;

public record UserAvatarView(
        String displayName, String email, String initial, boolean emailAsDisplayName, int unreadNotifications) {
    private static final int MAX_COMMENT_DISPLAY_NAME_LENGTH = 40;
    private static final int TRUNCATED_COMMENT_DISPLAY_NAME_LENGTH = 37;

    public static UserAvatarView from(String name, String email) {
        boolean useEmail = name == null || name.isBlank() || name.equalsIgnoreCase(email);
        String displayName = useEmail ? email : name.strip();
        String initial = displayName.substring(0, 1).toUpperCase(Locale.ROOT);

        return new UserAvatarView(displayName, email, initial, useEmail, 0);
    }

    public UserAvatarView withUnreadNotifications(int unreadNotifications) {
        return new UserAvatarView(displayName, email, initial, emailAsDisplayName, unreadNotifications);
    }

    public boolean hasUnreadNotifications() {
        return unreadNotifications > 0;
    }

    public String commentDisplayName() {
        if (displayName.length() > MAX_COMMENT_DISPLAY_NAME_LENGTH) {
            return displayName.substring(0, TRUNCATED_COMMENT_DISPLAY_NAME_LENGTH) + "...";
        }

        return displayName;
    }

    public String commentDisplayNameTooltip() {
        return displayName.length() > MAX_COMMENT_DISPLAY_NAME_LENGTH ? displayName : null;
    }
}
