package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TaskRoleFilter;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import java.util.List;
import java.util.StringJoiner;

public record MyTasksPageView(
        List<TaskWithTeamView> tasks,
        List<Team> teams,
        int totalTasksCount,
        TeamTasksStats stats,
        TaskStatus selectedStatus,
        Long selectedTeamId,
        TaskRoleFilter selectedRole,
        TaskSort selectedSort,
        int authorTasksCount,
        int assigneeTasksCount) {

    public List<Team> visibleTeams() {
        return teams.stream()
                .limit(5) // пока в фильтре показываем первые 5 команд
                .toList();
    }

    public String allStatusesUrl() {
        return buildUrl(null, selectedTeamId, selectedRole, selectedSort);
    }

    public String statusUrl(TaskStatus status) {
        return buildUrl(status, selectedTeamId, selectedRole, selectedSort);
    }

    public String allTeamsUrl() {
        return buildUrl(selectedStatus, null, selectedRole, selectedSort);
    }

    public String teamUrl(Long teamId) {
        return buildUrl(selectedStatus, teamId, selectedRole, selectedSort);
    }

    public String allRolesUrl() {
        return buildUrl(selectedStatus, selectedTeamId, null, selectedSort);
    }

    public String roleUrl(TaskRoleFilter role) {
        return buildUrl(selectedStatus, selectedTeamId, role, selectedSort);
    }

    public String sortUrl(TaskSort sort) {
        return buildUrl(selectedStatus, selectedTeamId, selectedRole, sort);
    }

    private String buildUrl(TaskStatus status, Long teamId, TaskRoleFilter role, TaskSort sort) {
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

        String queryString = query.toString();

        if (queryString.isBlank()) {
            return "/tasks";
        }

        return "/tasks?" + queryString;
    }
}
