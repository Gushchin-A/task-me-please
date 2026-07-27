package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.Task;
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
        TaskTag tag,
        TaskParticipants participants,
        TaskState state) {
    private static final DateTimeFormatter DEADLINE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"));

    public static TaskView from(Task task, String tagName, String authorName, String assigneeName) {
        return from(
                task,
                tagName,
                new TaskParticipantView(task.getAuthorId(), authorName, false),
                new TaskParticipantView(task.getAssigneeId(), assigneeName, false),
                new TaskState(task.isArchived(), true, true, true, true, false));
    }

    public static TaskView from(Task task, String tagName, String authorName, String assigneeName, TaskState state) {
        return from(
                task,
                tagName,
                new TaskParticipantView(task.getAuthorId(), authorName, false),
                new TaskParticipantView(task.getAssigneeId(), assigneeName, false),
                state);
    }

    public static TaskView from(
            Task task, String tagName, TaskParticipantView author, TaskParticipantView assignee, TaskState state) {
        return new TaskView(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                new TaskTimeline(task.getDeadlineAt(), task.getCreatedAt()),
                task.getStatus(),
                new TaskTag(task.getTagId(), tagName),
                new TaskParticipants(author, assignee),
                state);
    }

    public Instant deadlineAt() {
        return timeline.deadlineAt();
    }

    public Instant createdAt() {
        return timeline.createdAt();
    }

    public Long tagId() {
        return tag.id();
    }

    public String tagName() {
        return tag.name();
    }

    public UUID authorId() {
        return participants.author().id();
    }

    public String authorName() {
        return participants.author().name();
    }

    public boolean authorRemovedFromTeam() {
        return participants.author().removedFromTeam();
    }

    public UUID assigneeId() {
        return participants.assignee().id();
    }

    public String assigneeName() {
        return participants.assignee().name();
    }

    public boolean assigneeRemovedFromTeam() {
        return participants.assignee().removedFromTeam();
    }

    public boolean archived() {
        return state.archived();
    }

    public boolean canUpdateTask() {
        return state.canUpdateTask();
    }

    public boolean canUpdateStatus() {
        return state.canUpdateStatus();
    }

    public boolean canArchive() {
        return state.canArchive();
    }

    public boolean canRestore() {
        return state.canRestore();
    }

    public boolean showAuthorChangeWarning() {
        return state.showAuthorChangeWarning();
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

    public record TaskTag(Long id, String name) {}

    public record TaskParticipants(TaskParticipantView author, TaskParticipantView assignee) {}

    public record TaskState(
            boolean archived,
            boolean canUpdateTask,
            boolean canUpdateStatus,
            boolean canArchive,
            boolean canRestore,
            boolean showAuthorChangeWarning) {}
}
