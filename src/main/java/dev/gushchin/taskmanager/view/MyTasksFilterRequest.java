package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TaskRoleFilter;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import java.util.ArrayList;
import java.util.List;
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
    private List<UUID> authorIds = new ArrayList<>();
    private List<UUID> assigneeIds = new ArrayList<>();
    private List<Long> tagIds = new ArrayList<>();
    private String openFilter;
}
