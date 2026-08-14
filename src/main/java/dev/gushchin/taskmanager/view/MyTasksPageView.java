package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TaskListMode;
import dev.gushchin.taskmanager.model.TaskRoleFilter;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamTag;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

public record MyTasksPageView(
        List<TaskWithTeamView> tasks,
        MyTasksPageResources resources,
        int totalTasksCount,
        TeamTasksStats stats,
        MyTasksPageFilters filters,
        MyTasksRoleCounts roleCounts,
        boolean hasTaskHistory,
        TaskListMode mode) {
    private static final long VISIBLE_TEAMS_LIMIT = 5L;

    public List<Team> teams() {
        return resources.teams();
    }

    public List<TaskParticipantView> filterMembers() {
        return resources.filterMembers();
    }

    public List<TeamTag> tags() {
        return resources.tags();
    }

    public List<Team> visibleTeams() {
        return teams().stream().limit(VISIBLE_TEAMS_LIMIT).toList();
    }

    public TaskStatus selectedStatus() {
        return filters.selectedStatus();
    }

    public Long selectedTeamId() {
        return filters.selectedTeamId();
    }

    public TaskRoleFilter selectedRole() {
        return filters.selectedRole();
    }

    public TaskSort selectedSort() {
        return filters.selectedSort();
    }

    public UUID selectedAuthorId() {
        return filters.selectedAuthorId();
    }

    public UUID selectedAssigneeId() {
        return filters.selectedAssigneeId();
    }

    public Long selectedTagId() {
        return filters.selectedTagId();
    }

    public int authorTasksCount() {
        return roleCounts.authorTasksCount();
    }

    public int assigneeTasksCount() {
        return roleCounts.assigneeTasksCount();
    }

    public String allStatusesUrl() {
        return buildUrl(
                null,
                selectedTeamId(),
                selectedRole(),
                selectedSort(),
                selectedAuthorId(),
                selectedAssigneeId(),
                selectedTagId());
    }

    public String statusUrl(TaskStatus status) {
        return buildUrl(
                status,
                selectedTeamId(),
                selectedRole(),
                selectedSort(),
                selectedAuthorId(),
                selectedAssigneeId(),
                selectedTagId());
    }

    public String allTeamsUrl() {
        return buildUrl(
                selectedStatus(),
                null,
                selectedRole(),
                selectedSort(),
                selectedAuthorId(),
                selectedAssigneeId(),
                selectedTagId());
    }

    public String teamUrl(Long teamId) {
        return buildUrl(
                selectedStatus(),
                teamId,
                selectedRole(),
                selectedSort(),
                selectedAuthorId(),
                selectedAssigneeId(),
                selectedTagId());
    }

    public String allRolesUrl() {
        return buildUrl(
                selectedStatus(),
                selectedTeamId(),
                null,
                selectedSort(),
                selectedAuthorId(),
                selectedAssigneeId(),
                selectedTagId());
    }

    public String roleUrl(TaskRoleFilter role) {
        return buildUrl(
                selectedStatus(),
                selectedTeamId(),
                role,
                selectedSort(),
                selectedAuthorId(),
                selectedAssigneeId(),
                selectedTagId());
    }

    public boolean archiveMode() {
        return mode == TaskListMode.ARCHIVE;
    }

    public boolean activeMode() {
        return mode == TaskListMode.ACTIVE;
    }

    public boolean hasAccessibleTeams() {
        return !teams().isEmpty();
    }

    public boolean showTeamPrerequisiteState() {
        return !hasAccessibleTeams() && !hasTaskHistory;
    }

    public boolean showTaskWorkspaceNavigation() {
        return hasTaskHistory;
    }

    public String sortUrl(TaskSort sort) {
        return buildUrl(
                selectedStatus(),
                selectedTeamId(),
                selectedRole(),
                sort,
                selectedAuthorId(),
                selectedAssigneeId(),
                selectedTagId());
    }

    public String defaultSortUrl() {
        return buildUrl(
                selectedStatus(),
                selectedTeamId(),
                selectedRole(),
                null,
                selectedAuthorId(),
                selectedAssigneeId(),
                selectedTagId());
    }

    public String clearFiltersUrl() {
        return buildUrl(null, null, null, selectedSort(), null, null, null);
    }

    public String authorUrl(UUID authorId) {
        return buildUrl(
                selectedStatus(),
                selectedTeamId(),
                selectedRole(),
                selectedSort(),
                authorId,
                selectedAssigneeId(),
                selectedTagId());
    }

    public String assigneeUrl(UUID assigneeId) {
        return buildUrl(
                selectedStatus(),
                selectedTeamId(),
                selectedRole(),
                selectedSort(),
                selectedAuthorId(),
                assigneeId,
                selectedTagId());
    }

    public String tagUrl(Long tagId) {
        return buildUrl(
                selectedStatus(),
                selectedTeamId(),
                selectedRole(),
                selectedSort(),
                selectedAuthorId(),
                selectedAssigneeId(),
                tagId);
    }

    private String buildUrl(
            TaskStatus status,
            Long teamId,
            TaskRoleFilter role,
            TaskSort sort,
            UUID authorId,
            UUID assigneeId,
            Long tagId) {
        StringJoiner query = new StringJoiner("&");

        if (status != null) {
            query.add("status=" + status.name());
        }

        if (teamId != null) {
            query.add("teamId=" + teamId);
        }

        if (role != null) {
            query.add("role=" + role.name());
        }

        if (sort != null) {
            query.add("sort=" + sort.name());
        }

        if (authorId != null) {
            query.add("authorId=" + authorId);
        }

        if (assigneeId != null) {
            query.add("assigneeId=" + assigneeId);
        }

        if (tagId != null) {
            query.add("tagId=" + tagId);
        }

        String queryString = query.toString();
        String baseUrl = activeMode() ? "/tasks" : "/tasks/archive";

        if (queryString.isBlank()) {
            return baseUrl;
        }

        return baseUrl + "?" + queryString;
    }

    public record MyTasksPageFilters(
            TaskStatus selectedStatus,
            Long selectedTeamId,
            TaskRoleFilter selectedRole,
            TaskSort selectedSort,
            UUID selectedAuthorId,
            UUID selectedAssigneeId,
            Long selectedTagId) {}

    public record MyTasksPageResources(List<Team> teams, List<TaskParticipantView> filterMembers, List<TeamTag> tags) {}

    public record MyTasksRoleCounts(int authorTasksCount, int assigneeTasksCount) {}
}
