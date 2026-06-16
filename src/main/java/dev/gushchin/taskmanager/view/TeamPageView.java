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
        int totalTasksCount,
        int membersCount,
        TeamTasksStats stats,
        TaskStatus selectedStatus,
        TaskSort selectedSort,
        boolean canInvite) {}
