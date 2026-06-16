package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.User;
import java.util.List;

public record TeamPageView(
        Team team,
        List<TaskView> tasks,
        List<User> members,
        TeamPageCounts counts,
        TeamTasksStats stats,
        TeamPageFilters filters,
        boolean canInvite) {
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

    public record TeamPageCounts(int totalTasksCount, int membersCount) {}

    public record TeamPageFilters(TaskStatus selectedStatus, TaskSort selectedSort) {}
}
