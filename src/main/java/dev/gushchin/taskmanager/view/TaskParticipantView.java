package dev.gushchin.taskmanager.view;

import java.util.UUID;

public record TaskParticipantView(UUID id, String name, boolean removedFromTeam) {
    private static final String REMOVED_FROM_TEAM_TITLE = "Пользователь был удалён из команды";
    private static final String REMOVED_FROM_TEAM_STYLE = "color: red;";

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
