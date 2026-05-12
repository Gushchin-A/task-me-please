package dev.gushchin.taskmanager.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Task {
    private Long id;
    private Long teamId;
    private UUID authorId;
    private UUID assigneeId;
    private String title;
    private String description;
    private Instant deadlineAt;
    private TaskStatus status;
    private TaskCategory category;
    private boolean archived;
    private boolean deleted;
    private Instant createdAt;
    private Instant updatedAt;
}
