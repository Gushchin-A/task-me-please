package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TaskListMode;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamTag;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

public record TeamPageView(
        Team team,
        List<TaskView> tasks,
        TeamPageResources resources,
        TeamPageCounts counts,
        TeamTasksStats stats,
        TeamPageFilters filters,
        TeamPageAccess access,
        TaskListMode mode) {
    private static final String TEAMS_PATH_PREFIX = "/teams/";
    private static final String TEAM_COUNT_TEXT_PREFIX = "Всего задач в команде ";
    private static final String ARCHIVE_COUNT_TEXT_PREFIX = "Задач в архиве ";
    private static final String FILTERED_COUNT_TEXT_PREFIX = "Задач по выбранным фильтрам ";

    public List<TaskParticipantView> members() {
        return resources.members();
    }

    public List<TaskParticipantView> filterMembers() {
        return resources.filterMembers();
    }

    public List<TeamTag> tags() {
        return resources.tags();
    }

    public int totalTasksCount() {
        return archiveMode() ? counts.archiveTasksCount() : counts.activeTasksCount();
    }

    public int activeTasksCount() {
        return counts.activeTasksCount();
    }

    public int archiveTasksCount() {
        return counts.archiveTasksCount();
    }

    public boolean hasNoTasks() {
        return activeTasksCount() == 0 && archiveTasksCount() == 0;
    }

    public int membersCount() {
        return counts.membersCount();
    }

    public boolean canInvite() {
        return access.canInvite();
    }

    public boolean limitedTaskVisibility() {
        return access.limitedTaskVisibility();
    }

    public TaskStatus selectedStatus() {
        return filters.selectedStatus();
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

    public boolean activeMode() {
        return mode == TaskListMode.ACTIVE;
    }

    public boolean archiveMode() {
        return mode == TaskListMode.ARCHIVE;
    }

    public String baseUrl() {
        String teamUrl = TEAMS_PATH_PREFIX + team.getId();

        if (activeMode()) {
            return teamUrl;
        }

        return teamUrl + "/archive";
    }

    public String taskCountText() {
        if (hasSelectedFilters()) {
            return FILTERED_COUNT_TEXT_PREFIX + filteredTasksCount();
        }

        if (archiveMode()) {
            return ARCHIVE_COUNT_TEXT_PREFIX + totalTasksCount();
        }

        return TEAM_COUNT_TEXT_PREFIX + totalTasksCount();
    }

    public int filteredTasksCount() {
        return counts.filteredTasksCount();
    }

    public boolean hasSelectedFilters() {
        return selectedStatus() != null
                || !selectedAuthorIds().isEmpty()
                || !selectedAssigneeIds().isEmpty()
                || !selectedTagIds().isEmpty();
    }

    public String allStatusesUrl() {
        return buildUrl(null, selectedSort(), selectedAuthorIds(), selectedAssigneeIds(), selectedTagIds());
    }

    public String statusUrl(TaskStatus status) {
        TaskStatus nextStatus = status == selectedStatus() ? null : status;

        return buildUrl(nextStatus, selectedSort(), selectedAuthorIds(), selectedAssigneeIds(), selectedTagIds());
    }

    public String sortUrl(TaskSort sort) {
        return buildUrl(selectedStatus(), sort, selectedAuthorIds(), selectedAssigneeIds(), selectedTagIds());
    }

    public String clearFiltersUrl() {
        return buildUrl(null, selectedSort(), List.of(), List.of(), List.of());
    }

    public String authorUrl(UUID authorId) {
        return buildUrl(
                selectedStatus(),
                selectedSort(),
                toggleValue(selectedAuthorIds(), authorId),
                selectedAssigneeIds(),
                selectedTagIds());
    }

    public String assigneeUrl(UUID assigneeId) {
        return buildUrl(
                selectedStatus(),
                selectedSort(),
                selectedAuthorIds(),
                toggleValue(selectedAssigneeIds(), assigneeId),
                selectedTagIds());
    }

    public String tagUrl(Long tagId) {
        return buildUrl(
                selectedStatus(),
                selectedSort(),
                selectedAuthorIds(),
                selectedAssigneeIds(),
                toggleValue(selectedTagIds(), tagId));
    }

    private String buildUrl(
            TaskStatus status, TaskSort sort, List<UUID> authorIds, List<UUID> assigneeIds, List<Long> tagIds) {
        StringJoiner query = new StringJoiner("&");

        if (status != null) {
            query.add("status=" + status.name());
        }

        if (sort != null) {
            query.add("sort=" + sort.name());
        }

        authorIds.forEach(authorId -> query.add("authorIds=" + authorId));
        assigneeIds.forEach(assigneeId -> query.add("assigneeIds=" + assigneeId));
        tagIds.forEach(tagId -> query.add("tagIds=" + tagId));

        String queryString = query.toString();

        if (queryString.isBlank()) {
            return baseUrl();
        }

        return baseUrl() + "?" + queryString;
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

    public record TeamPageResources(
            List<TaskParticipantView> members, List<TaskParticipantView> filterMembers, List<TeamTag> tags) {}

    public record TeamPageCounts(
            int activeTasksCount, int archiveTasksCount, int filteredTasksCount, int membersCount) {}

    public record TeamPageFilters(
            TaskStatus selectedStatus,
            TaskSort selectedSort,
            List<UUID> selectedAuthorIds,
            List<UUID> selectedAssigneeIds,
            List<Long> selectedTagIds,
            String openFilter) {}

    public record TeamPageAccess(boolean canInvite, boolean limitedTaskVisibility) {}
}
