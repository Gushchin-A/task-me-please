package dev.gushchin.taskmanager.mapper;

import dev.gushchin.taskmanager.jooq.tables.records.TeamsRecord;
import dev.gushchin.taskmanager.model.Team;

public final class TeamMapper {

    private TeamMapper() {}

    public static Team toModel(TeamsRecord record) {
        if (record == null) {
            return null;
        }

        return new Team(
                record.getId(),
                record.getName(),
                record.getCreatedBy(),
                record.getCreatedAt().toInstant(),
                record.getUpdatedAt().toInstant(),
                Boolean.TRUE.equals(record.getIsDeleted()));
    }
}
