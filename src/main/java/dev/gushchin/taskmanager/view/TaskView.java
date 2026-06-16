package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskCategory;
import dev.gushchin.taskmanager.model.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public record TaskView(
        Long id,
        String title,
        String description,
        Instant deadlineAt,
        Instant createdAt,
        TaskStatus status,
        TaskCategory category,
        UUID authorId,
        String authorName,
        UUID assigneeId,
        String assigneeName,
        boolean archived) {
    private static final DateTimeFormatter DEADLINE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"));

    public static TaskView from(Task task, String authorName, String assigneeName) {
        return new TaskView(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDeadlineAt(),
                task.getCreatedAt(),
                task.getStatus(),
                task.getCategory(),
                task.getAuthorId(),
                authorName,
                task.getAssigneeId(),
                assigneeName,
                task.isArchived());
    }

    public String deadlineText() {
        if (deadlineAt == null) {
            return "не указан";
        }

        LocalDate deadlineDate = getDeadlineDate();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        if (deadlineDate.equals(today)) {
            return "сегодня";
        }

        if (deadlineDate.equals(today.plusDays(1))) {
            return "завтра";
        }

        if (deadlineDate.equals(today.minusDays(1))) {
            return "вчера";
        }

        return deadlineDate.format(DEADLINE_FORMATTER);
    }

    public boolean deadlineOverdue() {
        if (deadlineAt == null) {
            return false;
        }

        LocalDate deadlineDate = getDeadlineDate();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        return deadlineDate.isBefore(today);
    }

    private LocalDate getDeadlineDate() {
        return deadlineAt.atZone(ZoneOffset.UTC).toLocalDate();
    }
}
