package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TaskListMode;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamTag;
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
        boolean canInvite,
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

    public TaskStatus selectedStatus() {
        return filters.selectedStatus();
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
                || selectedAuthorId() != null
                || selectedAssigneeId() != null
                || selectedTagId() != null;
    }

    public String allStatusesUrl() {
        return buildUrl(null, selectedSort(), selectedAuthorId(), selectedAssigneeId(), selectedTagId());
    }

    public String statusUrl(TaskStatus status) {
        return buildUrl(status, selectedSort(), selectedAuthorId(), selectedAssigneeId(), selectedTagId());
    }

    public String sortUrl(TaskSort sort) {
        return buildUrl(selectedStatus(), sort, selectedAuthorId(), selectedAssigneeId(), selectedTagId());
    }

    public String clearFiltersUrl() {
        return buildUrl(null, selectedSort(), null, null, null);
    }

    public String defaultSortUrl() {
        return buildUrl(selectedStatus(), null, selectedAuthorId(), selectedAssigneeId(), selectedTagId());
    }

    public String allAuthorsUrl() {
        return buildUrl(selectedStatus(), selectedSort(), null, selectedAssigneeId(), selectedTagId());
    }

    public String authorUrl(UUID authorId) {
        return buildUrl(selectedStatus(), selectedSort(), authorId, selectedAssigneeId(), selectedTagId());
    }

    public String allAssigneesUrl() {
        return buildUrl(selectedStatus(), selectedSort(), selectedAuthorId(), null, selectedTagId());
    }

    public String assigneeUrl(UUID assigneeId) {
        return buildUrl(selectedStatus(), selectedSort(), selectedAuthorId(), assigneeId, selectedTagId());
    }

    public String allTagsUrl() {
        return buildUrl(selectedStatus(), selectedSort(), selectedAuthorId(), selectedAssigneeId(), null);
    }

    public String tagUrl(Long tagId) {
        return buildUrl(selectedStatus(), selectedSort(), selectedAuthorId(), selectedAssigneeId(), tagId);
    }

    private String buildUrl(TaskStatus status, TaskSort sort, UUID authorId, UUID assigneeId, Long tagId) {
        StringJoiner query = new StringJoiner("&");

        if (status != null) {
            query.add("status=" + status.name());
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

        if (queryString.isBlank()) {
            return baseUrl();
        }

        return baseUrl() + "?" + queryString;
    }

    public record TeamPageResources(
            List<TaskParticipantView> members, List<TaskParticipantView> filterMembers, List<TeamTag> tags) {}

    public record TeamPageCounts(
            int activeTasksCount, int archiveTasksCount, int filteredTasksCount, int membersCount) {}

    public record TeamPageFilters(
            TaskStatus selectedStatus,
            TaskSort selectedSort,
            UUID selectedAuthorId,
            UUID selectedAssigneeId,
            Long selectedTagId) {}
}
