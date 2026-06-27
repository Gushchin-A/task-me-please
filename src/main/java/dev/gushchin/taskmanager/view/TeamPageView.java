package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TaskListMode;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.User;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

public record TeamPageView(
        Team team,
        List<TaskView> tasks,
        List<User> members,
        TeamPageCounts counts,
        TeamTasksStats stats,
        TeamPageFilters filters,
        boolean canInvite,
        TaskListMode mode) {
    private static final String TEAMS_PATH_PREFIX = "/teams/";
    private static final String TEAM_COUNT_TEXT_PREFIX = "Всего задач в команде ";
    private static final String ARCHIVE_COUNT_TEXT_PREFIX = "Задач в архиве ";

    public int totalTasksCount() {
        return counts.totalTasksCount();
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
        if (archiveMode()) {
            return ARCHIVE_COUNT_TEXT_PREFIX + totalTasksCount();
        }

        return TEAM_COUNT_TEXT_PREFIX + totalTasksCount();
    }

    public String allStatusesUrl() {
        return buildUrl(null, selectedSort(), selectedAuthorId(), selectedAssigneeId());
    }

    public String statusUrl(TaskStatus status) {
        return buildUrl(status, selectedSort(), selectedAuthorId(), selectedAssigneeId());
    }

    public String sortUrl(TaskSort sort) {
        return buildUrl(selectedStatus(), sort, selectedAuthorId(), selectedAssigneeId());
    }

    public String allAuthorsUrl() {
        return buildUrl(selectedStatus(), selectedSort(), null, selectedAssigneeId());
    }

    public String authorUrl(UUID authorId) {
        return buildUrl(selectedStatus(), selectedSort(), authorId, selectedAssigneeId());
    }

    public String allAssigneesUrl() {
        return buildUrl(selectedStatus(), selectedSort(), selectedAuthorId(), null);
    }

    public String assigneeUrl(UUID assigneeId) {
        return buildUrl(selectedStatus(), selectedSort(), selectedAuthorId(), assigneeId);
    }

    private String buildUrl(TaskStatus status, TaskSort sort, UUID authorId, UUID assigneeId) {
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

        String queryString = query.toString();

        if (queryString.isBlank()) {
            return baseUrl();
        }

        return baseUrl() + "?" + queryString;
    }

    public record TeamPageCounts(int totalTasksCount, int membersCount) {}

    public record TeamPageFilters(
            TaskStatus selectedStatus, TaskSort selectedSort, UUID selectedAuthorId, UUID selectedAssigneeId) {}
}
