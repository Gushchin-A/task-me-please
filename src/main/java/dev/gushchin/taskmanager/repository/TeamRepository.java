package dev.gushchin.taskmanager.repository;

import dev.gushchin.taskmanager.jooq.tables.records.TeamsRecord;
import dev.gushchin.taskmanager.jooq.tables.records.UsersRecord;
import dev.gushchin.taskmanager.mapper.TeamMapper;
import dev.gushchin.taskmanager.mapper.UserMapper;
import dev.gushchin.taskmanager.model.Team;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.util.List;

import static dev.gushchin.taskmanager.jooq.Tables.TEAMS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;

@Repository
@RequiredArgsConstructor
public class TeamRepository {
    private final DSLContext dsl;

    public Team findById(Long id) {
        TeamsRecord record = dsl.selectFrom(TEAMS)
                .where(TEAMS.ID.eq(id))
                .fetchOne();

        return TeamMapper.toModel(record);
    }

    public List<Team> findAll() {
        return dsl.selectFrom(TEAMS)
                .fetch()
                .map(TeamMapper::toModel);
    }

    public Team save(Team team) {
        TeamsRecord record = dsl.insertInto(TEAMS)
                .set(TEAMS.NAME, team.getName())
                .set(TEAMS.CREATED_BY, team.getCreatedBy())
                .set(TEAMS.CREATED_AT, team.getCreatedAt().atOffset(ZoneOffset.UTC))
                .set(TEAMS.UPDATED_AT, team.getUpdatedAt().atOffset(ZoneOffset.UTC))
                .set(TEAMS.IS_DELETED, team.isDeleted())
                .returning()
                .fetchOne();

        return TeamMapper.toModel(record);
    }
}
