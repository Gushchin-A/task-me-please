package dev.gushchin.taskmanager.repository;

import static dev.gushchin.taskmanager.jooq.Tables.TASKS;

import dev.gushchin.taskmanager.jooq.tables.records.TasksRecord;
import dev.gushchin.taskmanager.mapper.TaskMapper;
import dev.gushchin.taskmanager.model.Task;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TaskRepository {
    private final DSLContext dsl;

    public List<Task> findByTeamId(Long teamId) {
        return dsl.selectFrom(TASKS).where(TASKS.TEAM_ID.eq(teamId)).fetch().map(TaskMapper::toModel);
    }

    public Task save(Task task) {
        TasksRecord record = dsl.insertInto(TASKS)
                .set(TASKS.TEAM_ID, task.getTeamId())
                .set(TASKS.AUTHOR_ID, task.getAuthorId())
                .set(TASKS.ASSIGNEE_ID, task.getAssigneeId())
                .set(TASKS.TITLE, task.getTitle())
                .set(TASKS.DESCRIPTION, task.getDescription())
                .set(
                        TASKS.DEADLINE_AT,
                        task.getDeadlineAt() == null
                                ? null
                                : task.getDeadlineAt().atOffset(ZoneOffset.UTC))
                .set(TASKS.STATUS, task.getStatus().name())
                .set(TASKS.CATEGORY, task.getCategory().name())
                .set(TASKS.IS_ARCHIVED, task.isArchived())
                .set(TASKS.IS_DELETED, task.isDeleted())
                .set(TASKS.CREATED_AT, task.getCreatedAt().atOffset(ZoneOffset.UTC))
                .set(TASKS.UPDATED_AT, task.getUpdatedAt().atOffset(ZoneOffset.UTC))
                .returning()
                .fetchOne();

        return TaskMapper.toModel(record);
    }
}
