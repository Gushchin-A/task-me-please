package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private static final ZoneId APPLICATION_TIME_ZONE = ZoneId.of("Europe/Moscow");
    private static final int MINIMUM_ELAPSED_UNIT = 1;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int HOURS_PER_DAY = 24;
    private static final int HOURS_PER_TWO_DAYS = 48;
    private static final int PLURAL_BASE = 100;
    private static final int PLURAL_LAST_DIGIT_BASE = 10;
    private static final int PLURAL_TEEN_START = 11;
    private static final int PLURAL_TEEN_END = 14;
    private static final int PLURAL_FEW_START = 2;
    private static final int PLURAL_FEW_END = 4;
    private static final String YESTERDAY_TEXT = "вчера";

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
                new TaskTimeline(task.getDeadlineAt(), task.getCreatedAt(), task.getUpdatedAt()),
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

    public Instant updatedAt() {
        return timeline.updatedAt();
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

    public String authorChangeWarningText() {
        if (!showAuthorChangeWarning()) {
            return null;
        }

        if (authorId().equals(assigneeId())) {
            return "После смены автора вы не сможете редактировать задачу";
        }

        return "После смены автора вы потеряете доступ к этой задаче";
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
        LocalDate today = LocalDate.now(APPLICATION_TIME_ZONE);

        return deadlineDate.isBefore(today);
    }

    public boolean deadlineToday() {
        if (deadlineAt() == null) {
            return false;
        }

        return getDeadlineDate().equals(LocalDate.now(APPLICATION_TIME_ZONE));
    }

    public String statusText() {
        return statusText(status);
    }

    public static String statusText(TaskStatus status) {
        return status.getDisplayName();
    }

    public String statusCssClass() {
        return status.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public String updatedAtText() {
        String updatedAtText;

        if (updatedAt() == null) {
            updatedAtText = "";
        } else {
            updatedAtText = formatUpdatedAt(Duration.between(updatedAt(), Instant.now()));
        }

        return updatedAtText;
    }

    public String createdAtText() {
        if (createdAt() == null) {
            return "";
        }

        return formatUpdatedAt(Duration.between(createdAt(), Instant.now()));
    }

    private static String formatUpdatedAt(Duration elapsed) {
        long minutes = Math.max(0, elapsed.toMinutes());
        long hours = elapsed.toHours();
        String updatedAtText;

        if (minutes < MINIMUM_ELAPSED_UNIT) {
            updatedAtText = "только что";
        } else if (minutes < MINUTES_PER_HOUR) {
            updatedAtText = formatRelativeTime(minutes, "минуту", "минуты", "минут");
        } else if (hours < HOURS_PER_DAY) {
            updatedAtText = formatRelativeTime(hours, "час", "часа", "часов");
        } else if (hours < HOURS_PER_TWO_DAYS) {
            updatedAtText = YESTERDAY_TEXT;
        } else {
            updatedAtText = formatRelativeTime(elapsed.toDays(), "день", "дня", "дней");
        }

        return updatedAtText;
    }

    private static String formatRelativeTime(long value, String singular, String few, String many) {
        long lastTwoDigits = value % PLURAL_BASE;
        long lastDigit = value % PLURAL_LAST_DIGIT_BASE;
        String unit = many;

        if (lastTwoDigits < PLURAL_TEEN_START || lastTwoDigits > PLURAL_TEEN_END) {
            if (lastDigit == MINIMUM_ELAPSED_UNIT) {
                unit = singular;
            } else if (lastDigit >= PLURAL_FEW_START && lastDigit <= PLURAL_FEW_END) {
                unit = few;
            }
        }

        return value + " " + unit + " назад";
    }

    private LocalDate getDeadlineDate() {
        return deadlineAt().atZone(ZoneOffset.UTC).toLocalDate();
    }

    private String formatDeadlineDate() {
        LocalDate deadlineDate = getDeadlineDate();
        LocalDate today = LocalDate.now(APPLICATION_TIME_ZONE);
        String deadlineText = deadlineDate.format(DEADLINE_FORMATTER);

        if (deadlineDate.equals(today)) {
            deadlineText = "сегодня";
        } else if (deadlineDate.equals(today.plusDays(1))) {
            deadlineText = "завтра";
        } else if (deadlineDate.equals(today.minusDays(1))) {
            deadlineText = YESTERDAY_TEXT;
        }

        return deadlineText;
    }

    public record TaskTimeline(Instant deadlineAt, Instant createdAt, Instant updatedAt) {}

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
