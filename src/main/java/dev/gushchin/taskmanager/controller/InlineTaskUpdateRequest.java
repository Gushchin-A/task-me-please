package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.model.TaskRoleFilter;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import java.time.LocalDate;
import java.util.UUID;

public class InlineTaskUpdateRequest {
    private String title;
    private String description;
    private LocalDate deadlineDate;
    private TaskStatus status;
    private Long tagId;
    private UUID authorId;
    private UUID assigneeId;
    private TaskStatus selectedStatus;
    private TaskSort selectedSort;
    private Long selectedTeamId;
    private TaskRoleFilter selectedRole;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDeadlineDate() {
        return deadlineDate;
    }

    public void setDeadlineDate(LocalDate deadlineDate) {
        this.deadlineDate = deadlineDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

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
