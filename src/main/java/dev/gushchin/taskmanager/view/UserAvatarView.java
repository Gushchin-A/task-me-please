package dev.gushchin.taskmanager.view;

import java.util.Locale;

public record UserAvatarView(String displayName, String email, String initial, boolean emailAsDisplayName) {
    public static UserAvatarView from(String name, String email) {
        boolean useEmail = name == null || name.isBlank() || name.equalsIgnoreCase(email);
        String displayName = useEmail ? email : name.strip();
        String initial = displayName.substring(0, 1).toUpperCase(Locale.ROOT);

        return new UserAvatarView(displayName, email, initial, useEmail);
    }
}
