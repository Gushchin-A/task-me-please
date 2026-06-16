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
        TaskTimeline timeline,
        TaskStatus status,
        TaskCategory category,
        TaskParticipants participants,
        boolean archived) {
    private static final DateTimeFormatter DEADLINE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"));

    public static TaskView from(Task task, String authorName, String assigneeName) {
        return new TaskView(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                new TaskTimeline(task.getDeadlineAt(), task.getCreatedAt()),
                task.getStatus(),
                task.getCategory(),
                new TaskParticipants(task.getAuthorId(), authorName, task.getAssigneeId(), assigneeName),
                task.isArchived());
    }

    public Instant deadlineAt() {
        return timeline.deadlineAt();
    }

    public Instant createdAt() {
        return timeline.createdAt();
    }

    public UUID authorId() {
        return participants.authorId();
    }

    public String authorName() {
        return participants.authorName();
    }

    public UUID assigneeId() {
        return participants.assigneeId();
    }

    public String assigneeName() {
        return participants.assigneeName();
    }

    public String deadlineText() {
        String deadlineText;

        if (deadlineAt() == null) {
            deadlineText = "не указан";
        } else {
            deadlineText = formatDeadlineDate();
        }

        return deadlineText;
    }

    public boolean deadlineOverdue() {
        if (deadlineAt() == null) {
            return false;
        }

        LocalDate deadlineDate = getDeadlineDate();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        return deadlineDate.isBefore(today);
    }

    private LocalDate getDeadlineDate() {
        return deadlineAt().atZone(ZoneOffset.UTC).toLocalDate();
    }

    private String formatDeadlineDate() {
        LocalDate deadlineDate = getDeadlineDate();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String deadlineText = deadlineDate.format(DEADLINE_FORMATTER);

        if (deadlineDate.equals(today)) {
            deadlineText = "сегодня";
        } else if (deadlineDate.equals(today.plusDays(1))) {
            deadlineText = "завтра";
        } else if (deadlineDate.equals(today.minusDays(1))) {
            deadlineText = "вчера";
        }

        return deadlineText;
    }

    public record TaskTimeline(Instant deadlineAt, Instant createdAt) {}

    public record TaskParticipants(UUID authorId, String authorName, UUID assigneeId, String assigneeName) {}
}
