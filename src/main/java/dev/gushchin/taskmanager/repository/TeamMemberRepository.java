package dev.gushchin.taskmanager.repository;

import static dev.gushchin.taskmanager.jooq.Tables.TEAM_MEMBERS;

import dev.gushchin.taskmanager.jooq.tables.records.TeamMembersRecord;
import dev.gushchin.taskmanager.mapper.TeamMemberMapper;
import dev.gushchin.taskmanager.model.TeamMember;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TeamMemberRepository {
    private final DSLContext dsl;

    public List<TeamMember> findByTeamId(Long teamId) {
        return dsl.selectFrom(TEAM_MEMBERS)
                .where(TEAM_MEMBERS.TEAM_ID.eq(teamId))
                .fetch()
                .map(TeamMemberMapper::toModel);
    }

    public TeamMember findByTeamIdAndUserId(Long teamId, UUID userId) {
        TeamMembersRecord record = dsl.selectFrom(TEAM_MEMBERS)
                .where(TEAM_MEMBERS.TEAM_ID.eq(teamId))
                .and(TEAM_MEMBERS.USER_ID.eq(userId))
                .fetchOne();

        return TeamMemberMapper.toModel(record);
    }

    public TeamMember save(TeamMember teamMember) {
        TeamMembersRecord record = dsl.insertInto(TEAM_MEMBERS)
                .set(TEAM_MEMBERS.TEAM_ID, teamMember.getTeamId())
                .set(TEAM_MEMBERS.USER_ID, teamMember.getUserId())
                .set(TEAM_MEMBERS.ROLE, teamMember.getRole().name())
                .set(TEAM_MEMBERS.JOINED_AT, teamMember.getJoinedAt().atOffset(ZoneOffset.UTC))
                .set(TEAM_MEMBERS.CREATED_AT, teamMember.getCreatedAt().atOffset(ZoneOffset.UTC))
                .set(TEAM_MEMBERS.UPDATED_AT, teamMember.getUpdatedAt().atOffset(ZoneOffset.UTC))
                .set(TEAM_MEMBERS.IS_DELETED, teamMember.isDeleted())
                .returning()
                .fetchOne();

        return TeamMemberMapper.toModel(record);
    }
}
