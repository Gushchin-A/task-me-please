package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TaskListMode;
import dev.gushchin.taskmanager.model.TaskRoleFilter;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamTag;
import java.util.ArrayList;
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

    public List<UUID> selectedAuthorIds() {
        return filters.selectedAuthorIds();
    }

    public List<UUID> selectedAssigneeIds() {
        return filters.selectedAssigneeIds();
    }

    public List<Long> selectedTagIds() {
        return filters.selectedTagIds();
    }

    public String openFilter() {
        return filters.openFilter();
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
                selectedAuthorIds(),
                selectedAssigneeIds(),
                selectedTagIds());
    }

    public String statusUrl(TaskStatus status) {
        TaskStatus nextStatus = status == selectedStatus() ? null : status;

        return buildUrl(
                nextStatus,
                selectedTeamId(),
                selectedRole(),
                selectedSort(),
                selectedAuthorIds(),
                selectedAssigneeIds(),
                selectedTagIds());
    }

    public String allTeamsUrl() {
        return buildUrl(
                selectedStatus(),
                null,
                selectedRole(),
                selectedSort(),
                selectedAuthorIds(),
                selectedAssigneeIds(),
                selectedTagIds());
    }

    public String teamUrl(Long teamId) {
        Long nextTeamId = teamId.equals(selectedTeamId()) ? null : teamId;

        return buildUrl(
                selectedStatus(),
                nextTeamId,
                selectedRole(),
                selectedSort(),
                selectedAuthorIds(),
                selectedAssigneeIds(),
                selectedTagIds());
    }

    public String roleUrl(TaskRoleFilter role) {
        TaskRoleFilter nextRole = role == selectedRole() ? null : role;

        return buildUrl(
                selectedStatus(),
                selectedTeamId(),
                nextRole,
                selectedSort(),
                selectedAuthorIds(),
                selectedAssigneeIds(),
                selectedTagIds());
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
                selectedAuthorIds(),
                selectedAssigneeIds(),
                selectedTagIds());
    }

    public String clearFiltersUrl() {
        return buildUrl(null, null, null, selectedSort(), List.of(), List.of(), List.of());
    }

    public String authorUrl(UUID authorId) {
        List<UUID> authorIds = toggleValue(selectedAuthorIds(), authorId);

        return buildUrl(
                selectedStatus(),
                selectedTeamId(),
                selectedRole(),
                selectedSort(),
                authorIds,
                selectedAssigneeIds(),
                selectedTagIds());
    }

    public String assigneeUrl(UUID assigneeId) {
        List<UUID> assigneeIds = toggleValue(selectedAssigneeIds(), assigneeId);

        return buildUrl(
                selectedStatus(),
                selectedTeamId(),
                selectedRole(),
                selectedSort(),
                selectedAuthorIds(),
                assigneeIds,
                selectedTagIds());
    }

    public String tagUrl(Long tagId) {
        List<Long> tagIds = toggleValue(selectedTagIds(), tagId);

        return buildUrl(
                selectedStatus(),
                selectedTeamId(),
                selectedRole(),
                selectedSort(),
                selectedAuthorIds(),
                selectedAssigneeIds(),
                tagIds);
    }

    private String buildUrl(
            TaskStatus status,
            Long teamId,
            TaskRoleFilter role,
            TaskSort sort,
            List<UUID> authorIds,
            List<UUID> assigneeIds,
            List<Long> tagIds) {
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

        authorIds.forEach(authorId -> query.add("authorIds=" + authorId));
        assigneeIds.forEach(assigneeId -> query.add("assigneeIds=" + assigneeId));
        tagIds.forEach(tagId -> query.add("tagIds=" + tagId));

        String queryString = query.toString();
        String baseUrl = activeMode() ? "/tasks" : "/tasks/archive";

        if (queryString.isBlank()) {
            return baseUrl;
        }

        return baseUrl + "?" + queryString;
    }

    private <T> List<T> toggleValue(List<T> selectedValues, T value) {
        if (selectedValues.contains(value)) {
            return selectedValues.stream()
                    .filter(selectedValue -> !selectedValue.equals(value))
                    .toList();
        }

        List<T> values = new ArrayList<>(selectedValues);

        values.add(value);
        return List.copyOf(values);
    }

    public record MyTasksPageFilters(
            TaskStatus selectedStatus,
            Long selectedTeamId,
            TaskRoleFilter selectedRole,
            TaskSort selectedSort,
            List<UUID> selectedAuthorIds,
            List<UUID> selectedAssigneeIds,
            List<Long> selectedTagIds,
            String openFilter) {}

    public record MyTasksPageResources(List<Team> teams, List<TaskParticipantView> filterMembers, List<TeamTag> tags) {}

    public record MyTasksRoleCounts(int authorTasksCount, int assigneeTasksCount) {}
}
