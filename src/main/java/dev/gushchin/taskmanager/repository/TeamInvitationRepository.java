package dev.gushchin.taskmanager.repository;

import static dev.gushchin.taskmanager.jooq.Tables.TEAM_INVITATIONS;

import dev.gushchin.taskmanager.jooq.tables.records.TeamInvitationsRecord;
import dev.gushchin.taskmanager.mapper.TeamInvitationMapper;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TeamInvitationRepository {
    private final DSLContext dsl;

    public List<TeamInvitation> findByTeamId(Long teamId) {
        return dsl.selectFrom(TEAM_INVITATIONS)
                .where(TEAM_INVITATIONS.TEAM_ID.eq(teamId))
                .and(TEAM_INVITATIONS.IS_DELETED.eq(false))
                .orderBy(TEAM_INVITATIONS.ID.asc())
                .fetch()
                .map(TeamInvitationMapper::toModel);
    }

    public TeamInvitation findPendingByTeamIdAndEmail(Long teamId, String invitedEmail) {
        TeamInvitationsRecord record = dsl.selectFrom(TEAM_INVITATIONS)
                .where(TEAM_INVITATIONS.TEAM_ID.eq(teamId))
                .and(TEAM_INVITATIONS.INVITED_EMAIL.eq(invitedEmail))
                .and(TEAM_INVITATIONS.STATUS.eq(TeamInvitationStatus.PENDING.name()))
                .and(TEAM_INVITATIONS.IS_DELETED.eq(false))
                .fetchOne();

        return TeamInvitationMapper.toModel(record);
    }

    public TeamInvitation findByToken(String token) {
        TeamInvitationsRecord record = dsl.selectFrom(TEAM_INVITATIONS)
                .where(TEAM_INVITATIONS.TOKEN.eq(token))
                .and(TEAM_INVITATIONS.IS_DELETED.eq(false))
                .fetchOne();

        return TeamInvitationMapper.toModel(record);
    }

    public boolean existsByToken(String token) {
        return dsl.fetchExists(dsl.selectFrom(TEAM_INVITATIONS).where(TEAM_INVITATIONS.TOKEN.eq(token)));
    }

    public TeamInvitation save(TeamInvitation invitation) {
        TeamInvitationsRecord record = dsl.insertInto(TEAM_INVITATIONS)
                .set(TEAM_INVITATIONS.TEAM_ID, invitation.getTeamId())
                .set(TEAM_INVITATIONS.INVITED_BY, invitation.getInvitedBy())
                .set(TEAM_INVITATIONS.INVITED_EMAIL, invitation.getInvitedEmail())
                .set(TEAM_INVITATIONS.TOKEN, invitation.getToken())
                .set(TEAM_INVITATIONS.STATUS, invitation.getStatus().name())
                .set(TEAM_INVITATIONS.EXPIRES_AT, invitation.getExpiresAt().atOffset(ZoneOffset.UTC))
                .set(TEAM_INVITATIONS.CREATED_AT, invitation.getCreatedAt().atOffset(ZoneOffset.UTC))
                .set(TEAM_INVITATIONS.UPDATED_AT, invitation.getUpdatedAt().atOffset(ZoneOffset.UTC))
                .set(TEAM_INVITATIONS.IS_DELETED, invitation.isDeleted())
                .returning()
                .fetchOne();

        return TeamInvitationMapper.toModel(record);
    }

    public TeamInvitation updateStatus(Long id, TeamInvitationStatus status, Instant updatedAt) {
        TeamInvitationsRecord record = dsl.update(TEAM_INVITATIONS)
                .set(TEAM_INVITATIONS.STATUS, status.name())
                .set(TEAM_INVITATIONS.UPDATED_AT, updatedAt.atOffset(ZoneOffset.UTC))
                .where(TEAM_INVITATIONS.ID.eq(id))
                .returning()
                .fetchOne();

        return TeamInvitationMapper.toModel(record);
    }

    public TeamInvitation updateDelivery(Long id, String token, Instant expiresAt, Instant updatedAt) {
        TeamInvitationsRecord record = dsl.update(TEAM_INVITATIONS)
                .set(TEAM_INVITATIONS.TOKEN, token)
                .set(TEAM_INVITATIONS.EXPIRES_AT, expiresAt.atOffset(ZoneOffset.UTC))
                .set(TEAM_INVITATIONS.UPDATED_AT, updatedAt.atOffset(ZoneOffset.UTC))
                .where(TEAM_INVITATIONS.ID.eq(id))
                .returning()
                .fetchOne();

        return TeamInvitationMapper.toModel(record);
    }

    public void deleteAll() {
        dsl.deleteFrom(TEAM_INVITATIONS).execute();
    }
}
