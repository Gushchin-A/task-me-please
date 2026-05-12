package dev.gushchin.taskmanager.mapper;

import dev.gushchin.taskmanager.jooq.tables.records.TeamInvitationsRecord;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;

public final class TeamInvitationMapper {

    private TeamInvitationMapper() {}

    public static TeamInvitation toModel(TeamInvitationsRecord record) {
        if (record == null) {
            return null;
        }

        return new TeamInvitation(
                record.getId(),
                record.getTeamId(),
                record.getInvitedBy(),
                record.getInvitedEmail(),
                record.getToken(),
                TeamInvitationStatus.valueOf(record.getStatus()),
                record.getExpiresAt().toInstant(),
                record.getCreatedAt().toInstant(),
                record.getUpdatedAt().toInstant(),
                Boolean.TRUE.equals(record.getIsDeleted()));
    }
}
