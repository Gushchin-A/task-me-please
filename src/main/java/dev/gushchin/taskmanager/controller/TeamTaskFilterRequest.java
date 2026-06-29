package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamTaskFilterRequest {
    private TaskStatus status;
    private TaskSort sort;
    private UUID authorId;
    private UUID assigneeId;
}
