package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TeamMemberRole;
import dev.gushchin.taskmanager.model.TeamTaskVisibility;
import java.util.Locale;
import java.util.UUID;

public record TeamMemberView(
        UUID userId,
        String name,
        String email,
        TeamMemberRole role,
        long authorTasksCount,
        long assigneeTasksCount,
        TeamTaskVisibility taskVisibility) {
    private static final int DISPLAY_NAME_MAX_LENGTH = 35;
    private static final int SINGLE_NAME_PART_COUNT = 1;

    public String displayName() {
        if (name.length() <= DISPLAY_NAME_MAX_LENGTH) {
            return name;
        }

        return name.substring(0, DISPLAY_NAME_MAX_LENGTH) + "...";
    }

    public String initials() {
        String displayName = name == null || name.isBlank() ? email : name.strip();
        String[] parts = displayName.split("\\s+");
        String initials = parts[0].substring(0, 1);

        if (parts.length > SINGLE_NAME_PART_COUNT) {
            initials += parts[parts.length - 1].substring(0, 1);
        }

        return initials.toUpperCase(Locale.ROOT);
    }
}
