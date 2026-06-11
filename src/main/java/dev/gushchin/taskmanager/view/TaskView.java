package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskCategory;
import dev.gushchin.taskmanager.model.TaskStatus;
import java.time.Instant;

public record TaskView(
        Long id,
        String title,
        String description,
        Instant deadlineAt,
        TaskStatus status,
        TaskCategory category,
        String authorName) {

    public static TaskView from(Task task, String authorName) {
        return new TaskView(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDeadlineAt(),
                task.getStatus(),
                task.getCategory(),
                authorName);
    }
}
