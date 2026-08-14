package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TaskRoleFilter;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyTasksFilterRequest {
    private TaskStatus status;
    private Long teamId;
    private TaskRoleFilter role;
    private TaskSort sort;
    private UUID authorId;
    private UUID assigneeId;
    private Long tagId;
}
