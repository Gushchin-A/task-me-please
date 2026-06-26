package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TaskListMode;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.User;
import java.util.List;
import java.util.StringJoiner;

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

    public String allStatusesUrl() {
        return buildUrl(null, selectedSort());
    }

    public String statusUrl(TaskStatus status) {
        return buildUrl(status, selectedSort());
    }

    public String sortUrl(TaskSort sort) {
        return buildUrl(selectedStatus(), sort);
    }

    private String buildUrl(TaskStatus status, TaskSort sort) {
        StringJoiner query = new StringJoiner("&");

        if (status != null) {
            query.add("status=" + status.name());
        }

        if (sort != null) {
            query.add("sort=" + sort.name());
        }

        String queryString = query.toString();

        if (queryString.isBlank()) {
            return baseUrl();
        }

        return baseUrl() + "?" + queryString;
    }

    public record TeamPageCounts(int totalTasksCount, int membersCount) {}

    public record TeamPageFilters(TaskStatus selectedStatus, TaskSort selectedSort) {}
}
