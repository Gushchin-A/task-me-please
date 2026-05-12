package dev.gushchin.taskmanager.mapper;

import dev.gushchin.taskmanager.jooq.tables.records.TeamMembersRecord;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamMemberRole;

public final class TeamMemberMapper {

    private TeamMemberMapper() {}

    public static TeamMember toModel(TeamMembersRecord record) {
        if (record == null) {
            return null;
        }

        return new TeamMember(
                record.getId(),
                record.getTeamId(),
                record.getUserId(),
                TeamMemberRole.valueOf(record.getRole()),
                record.getJoinedAt().toInstant(),
                record.getCreatedAt().toInstant(),
                record.getUpdatedAt().toInstant(),
                Boolean.TRUE.equals(record.getIsDeleted()));
    }
}
