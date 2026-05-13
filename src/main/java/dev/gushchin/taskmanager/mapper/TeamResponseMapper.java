package dev.gushchin.taskmanager.mapper;

import dev.gushchin.taskmanager.dto.TeamResponse;
import dev.gushchin.taskmanager.model.Team;

public final class TeamResponseMapper {

    private TeamResponseMapper() {
    }

    public static TeamResponse toResponse(Team team) {
        if (team == null) {
            return null;
        }

        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getCreatedBy(),
                team.getCreatedAt(),
                team.getUpdatedAt(),
                team.isDeleted()
        );
    }
}
