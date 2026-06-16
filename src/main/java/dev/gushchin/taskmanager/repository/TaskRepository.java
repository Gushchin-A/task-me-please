package dev.gushchin.taskmanager.repository;

import static dev.gushchin.taskmanager.jooq.Tables.TASKS;

import dev.gushchin.taskmanager.jooq.tables.records.TasksRecord;
import dev.gushchin.taskmanager.mapper.TaskMapper;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskCategory;
import dev.gushchin.taskmanager.model.TaskStatus;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
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

    public Task findById(Long id) {
        TasksRecord record = dsl.selectFrom(TASKS).where(TASKS.ID.eq(id)).fetchOne();

        return TaskMapper.toModel(record);
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

    public Task updateStatus(Long id, TaskStatus status, Instant updatedAt) {
        TasksRecord record = dsl.update(TASKS)
                .set(TASKS.STATUS, status.name())
                .set(TASKS.UPDATED_AT, updatedAt.atOffset(ZoneOffset.UTC))
                .where(TASKS.ID.eq(id))
                .returning()
                .fetchOne();

        return TaskMapper.toModel(record);
    }

    public Task updateCategory(Long id, TaskCategory category, Instant updatedAt) {
        TasksRecord record = dsl.update(TASKS)
                .set(TASKS.CATEGORY, category.name())
                .set(TASKS.UPDATED_AT, updatedAt.atOffset(ZoneOffset.UTC))
                .where(TASKS.ID.eq(id))
                .returning()
                .fetchOne();

        return TaskMapper.toModel(record);
    }

    public Task updateAuthor(Long id, UUID authorId, Instant updatedAt) {
        TasksRecord record = dsl.update(TASKS)
                .set(TASKS.AUTHOR_ID, authorId)
                .set(TASKS.UPDATED_AT, updatedAt.atOffset(ZoneOffset.UTC))
                .where(TASKS.ID.eq(id))
                .returning()
                .fetchOne();

        return TaskMapper.toModel(record);
    }

    public Task updateAssignee(Long id, UUID assigneeId, Instant updatedAt) {
        TasksRecord record = dsl.update(TASKS)
                .set(TASKS.ASSIGNEE_ID, assigneeId)
                .set(TASKS.UPDATED_AT, updatedAt.atOffset(ZoneOffset.UTC))
                .where(TASKS.ID.eq(id))
                .returning()
                .fetchOne();

        return TaskMapper.toModel(record);
    }

    public Task updateDeadline(Long id, Instant deadlineAt, Instant updatedAt) {
        TasksRecord record = dsl.update(TASKS)
                .set(TASKS.DEADLINE_AT, deadlineAt == null ? null : deadlineAt.atOffset(ZoneOffset.UTC))
                .set(TASKS.UPDATED_AT, updatedAt.atOffset(ZoneOffset.UTC))
                .where(TASKS.ID.eq(id))
                .returning()
                .fetchOne();

        return TaskMapper.toModel(record);
    }

    public Task updateDetails(
            Long id,
            String title,
            String description,
            Instant deadlineAt,
            TaskCategory category,
            UUID assigneeId,
            Instant updatedAt) {
        TasksRecord record = dsl.update(TASKS)
                .set(TASKS.TITLE, title)
                .set(TASKS.DESCRIPTION, description)
                .set(TASKS.DEADLINE_AT, deadlineAt == null ? null : deadlineAt.atOffset(ZoneOffset.UTC))
                .set(TASKS.CATEGORY, category.name())
                .set(TASKS.ASSIGNEE_ID, assigneeId)
                .set(TASKS.UPDATED_AT, updatedAt.atOffset(ZoneOffset.UTC))
                .where(TASKS.ID.eq(id))
                .returning()
                .fetchOne();

        return TaskMapper.toModel(record);
    }

    public Task archive(Long id, Instant updatedAt) {
        TasksRecord record = dsl.update(TASKS)
                .set(TASKS.IS_ARCHIVED, true)
                .set(TASKS.UPDATED_AT, updatedAt.atOffset(ZoneOffset.UTC))
                .where(TASKS.ID.eq(id))
                .returning()
                .fetchOne();

        return TaskMapper.toModel(record);
    }

    public Task restoreFromArchive(Long id, Instant updatedAt) {
        TasksRecord record = dsl.update(TASKS)
                .set(TASKS.IS_ARCHIVED, false)
                .set(TASKS.UPDATED_AT, updatedAt.atOffset(ZoneOffset.UTC))
                .where(TASKS.ID.eq(id))
                .returning()
                .fetchOne();

        return TaskMapper.toModel(record);
    }
}
