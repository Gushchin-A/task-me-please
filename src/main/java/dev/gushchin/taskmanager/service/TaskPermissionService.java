package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamMemberRole;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskPermissionService {
    private final TeamMemberService teamMemberService;

    public boolean canUpdateTask(Task task, UUID userId) {
        return isTeamOwner(task, userId) || isTaskAuthor(task, userId);
    }

    public boolean canUpdateStatus(Task task, UUID userId) {
        return canUpdateTask(task, userId) || isTaskAssignee(task, userId);
    }

    public boolean canArchiveTask(Task task, UUID userId) {
        return canUpdateTask(task, userId) && canBeArchived(task);
    }

    public boolean canRestoreTask(Task task, UUID userId) {
        return canUpdateTask(task, userId);
    }

    public boolean canBeArchived(Task task) {
        return task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.NOT_RELEVANT;
    }

    public boolean isTeamOwner(Task task, UUID userId) {
        TeamMember teamMember = teamMemberService.findById(task.getTeamId(), userId);

        return teamMember.getRole() == TeamMemberRole.OWNER;
    }

    public boolean isTaskAuthor(Task task, UUID userId) {
        return task.getAuthorId().equals(userId);
    }

    public boolean isTaskAssignee(Task task, UUID userId) {
        return task.getAssigneeId().equals(userId);
    }
}
