package dev.gushchin.taskmanager.view;

import java.util.Locale;
import java.util.UUID;

public record TaskParticipantView(UUID id, String name, boolean removedFromTeam) {
    private static final int MAX_DISPLAY_NAME_LENGTH = 18;
    private static final int TRUNCATED_DISPLAY_NAME_LENGTH = 15;
    private static final int MAX_COMMENT_DISPLAY_NAME_LENGTH = 40;
    private static final int TRUNCATED_COMMENT_DISPLAY_NAME_LENGTH = 37;
    private static final int MAX_FILTER_DISPLAY_NAME_LENGTH = 15;
    private static final String ELLIPSIS = "...";
    private static final String REMOVED_FROM_TEAM_TITLE = "Пользователь был удалён из команды";
    private static final String REMOVED_FROM_TEAM_STYLE = "color: red;";

    public String displayName() {
        int emailSeparatorIndex = name.indexOf('@');

        if (emailSeparatorIndex > 0) {
            return name.substring(0, emailSeparatorIndex);
        }

        return name;
    }

    public String shortDisplayName() {
        String displayName = displayName();

        if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            return displayName.substring(0, TRUNCATED_DISPLAY_NAME_LENGTH) + ELLIPSIS;
        }

        return displayName;
    }

    public String commentDisplayName() {
        String displayName = displayName();

        if (displayName.length() > MAX_COMMENT_DISPLAY_NAME_LENGTH) {
            return displayName.substring(0, TRUNCATED_COMMENT_DISPLAY_NAME_LENGTH) + ELLIPSIS;
        }

        return displayName;
    }

    public String commentDisplayNameTooltip() {
        String displayName = displayName();

        return displayName.length() > MAX_COMMENT_DISPLAY_NAME_LENGTH ? displayName : null;
    }

    public String filterDisplayName() {
        String displayName = displayName();

        if (displayName.length() > MAX_FILTER_DISPLAY_NAME_LENGTH) {
            return displayName.substring(0, MAX_FILTER_DISPLAY_NAME_LENGTH) + ELLIPSIS;
        }

        return displayName;
    }

    public String filterDisplayNameTooltip() {
        String displayName = displayName();

        return displayName.length() > MAX_FILTER_DISPLAY_NAME_LENGTH ? displayName : null;
    }

    public String initial() {
        return displayName().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    public String removedFromTeamTitle() {
        if (removedFromTeam) {
            return REMOVED_FROM_TEAM_TITLE;
        }

        return null;
    }

    public String removedFromTeamStyle() {
        if (removedFromTeam) {
            return REMOVED_FROM_TEAM_STYLE;
        }

        return null;
    }
}
