package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.model.TaskRoleFilter;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;

public class InlineTaskUpdateRequest {
    private TaskStatus selectedStatus;
    private TaskSort selectedSort;
    private Long selectedTeamId;
    private TaskRoleFilter selectedRole;

    public TaskStatus getSelectedStatus() {
        return selectedStatus;
    }

    public void setSelectedStatus(TaskStatus selectedStatus) {
        this.selectedStatus = selectedStatus;
    }

    public TaskSort getSelectedSort() {
        return selectedSort;
    }

    public void setSelectedSort(TaskSort selectedSort) {
        this.selectedSort = selectedSort;
    }

    public Long getSelectedTeamId() {
        return selectedTeamId;
    }

    public void setSelectedTeamId(Long selectedTeamId) {
        this.selectedTeamId = selectedTeamId;
    }

    public TaskRoleFilter getSelectedRole() {
        return selectedRole;
    }

    public void setSelectedRole(TaskRoleFilter selectedRole) {
        this.selectedRole = selectedRole;
    }
}
