package dev.gushchin.taskmanager.model;

import java.time.LocalDate;
import java.util.UUID;

public record TaskDetailsUpdate(
        String title, String description, LocalDate deadlineDate, TaskStatus status, Long tagId, UUID assigneeId) {}
