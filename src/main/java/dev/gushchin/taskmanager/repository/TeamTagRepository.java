package dev.gushchin.taskmanager.repository;

import static dev.gushchin.taskmanager.jooq.Tables.TEAM_TAGS;

import dev.gushchin.taskmanager.jooq.tables.records.TeamTagsRecord;
import dev.gushchin.taskmanager.mapper.TeamTagMapper;
import dev.gushchin.taskmanager.model.TeamTag;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TeamTagRepository {
    private final DSLContext dsl;

    public TeamTag findById(Long id) {
        TeamTagsRecord record =
                dsl.selectFrom(TEAM_TAGS).where(TEAM_TAGS.ID.eq(id)).fetchOne();

        return TeamTagMapper.toModel(record);
    }

    public List<TeamTag> findByTeamId(Long teamId) {
        return dsl.selectFrom(TEAM_TAGS)
                .where(TEAM_TAGS.TEAM_ID.eq(teamId))
                .orderBy(TEAM_TAGS.ID.asc())
                .fetch()
                .map(TeamTagMapper::toModel);
    }

    public TeamTag findActiveByTeamIdAndNormalizedName(Long teamId, String normalizedName) {
        TeamTagsRecord record = dsl.selectFrom(TEAM_TAGS)
                .where(TEAM_TAGS.TEAM_ID.eq(teamId))
                .and(TEAM_TAGS.NORMALIZED_NAME.eq(normalizedName))
                .and(TEAM_TAGS.IS_DELETED.eq(false))
                .fetchOne();

        return TeamTagMapper.toModel(record);
    }

    public TeamTag save(TeamTag teamTag) {
        TeamTagsRecord record = dsl.insertInto(TEAM_TAGS)
                .set(TEAM_TAGS.TEAM_ID, teamTag.getTeamId())
                .set(TEAM_TAGS.NAME, teamTag.getName())
                .set(TEAM_TAGS.NORMALIZED_NAME, teamTag.getNormalizedName())
                .set(TEAM_TAGS.CREATED_AT, teamTag.getCreatedAt().atOffset(ZoneOffset.UTC))
                .set(TEAM_TAGS.UPDATED_AT, teamTag.getUpdatedAt().atOffset(ZoneOffset.UTC))
                .set(TEAM_TAGS.IS_DELETED, teamTag.isDeleted())
                .returning()
                .fetchOne();

        return TeamTagMapper.toModel(record);
    }

    public void deleteAll() {
        dsl.deleteFrom(TEAM_TAGS).execute();
    }
}
