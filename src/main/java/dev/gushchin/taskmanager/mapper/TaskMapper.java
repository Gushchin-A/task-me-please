package dev.gushchin.taskmanager.mapper;

import dev.gushchin.taskmanager.jooq.tables.records.TasksRecord;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskCategory;
import dev.gushchin.taskmanager.model.TaskStatus;

public final class TaskMapper {

    private TaskMapper() {}

    public static Task toModel(TasksRecord record) {
        if (record == null) {
            return null;
        }

        return new Task(
                record.getId(),
                record.getTeamId(),
                record.getAuthorId(),
                record.getAssigneeId(),
                record.getTitle(),
                record.getDescription(),
                record.getDeadlineAt() != null ? record.getDeadlineAt().toInstant() : null,
                TaskStatus.valueOf(record.getStatus()),
                TaskCategory.valueOf(record.getCategory()),
                record.getCreatedAt().toInstant(),
                record.getUpdatedAt().toInstant(),
                Boolean.TRUE.equals(record.getIsArchived()),
                Boolean.TRUE.equals(record.getIsDeleted()));
    }
}
