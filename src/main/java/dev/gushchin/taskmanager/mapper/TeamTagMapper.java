package dev.gushchin.taskmanager.mapper;

import dev.gushchin.taskmanager.jooq.tables.records.TeamTagsRecord;
import dev.gushchin.taskmanager.model.TeamTag;

public final class TeamTagMapper {

    private TeamTagMapper() {}

    public static TeamTag toModel(TeamTagsRecord record) {
        if (record == null) {
            return null;
        }

        return new TeamTag(
                record.getId(),
                record.getTeamId(),
                record.getName(),
                record.getNormalizedName(),
                record.getCreatedAt().toInstant(),
                record.getUpdatedAt().toInstant(),
                Boolean.TRUE.equals(record.getIsDeleted()));
    }
}
