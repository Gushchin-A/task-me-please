package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.model.Comment;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskCategory;
import dev.gushchin.taskmanager.model.TaskRoleFilter;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.CommentService;
import dev.gushchin.taskmanager.service.TaskService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.UserService;
import dev.gushchin.taskmanager.view.CommentView;
import dev.gushchin.taskmanager.view.MyTasksPageView;
import dev.gushchin.taskmanager.view.TaskView;
import dev.gushchin.taskmanager.view.TaskWithTeamView;
import dev.gushchin.taskmanager.view.TeamTasksStats;
import java.time.LocalDate;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class TaskPageController {
    private static final String REDIRECT_TEAMS_PREFIX = "redirect:/teams/";
    private static final String REDIRECT_TASKS_PREFIX = "redirect:/tasks/";

    private final TeamService teamService;
    private final TaskService taskService;
    private final TeamMemberService teamMemberService;
    private final UserService userService;
    private final CommentService commentService;

    @GetMapping("/tasks")
    public String tasksPage(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) TaskRoleFilter role,
            @RequestParam(required = false) TaskSort sort,
            Model model,
            CsrfToken csrfToken) {
        List<Team> teams = teamService.findByUserId(authUser.getId());
        List<Long> teamIds = teams.stream().map(Team::getId).toList();

        List<Task> allTasks = taskService.findByTeamIds(teamIds);
        List<Task> teamFilteredTasks = taskService.filterByTeamId(allTasks, teamId);
        List<Task> roleFilteredTasks = taskService.filterByRole(teamFilteredTasks, role, authUser.getId());
        List<Task> statusFilteredTasks = taskService.filterByStatus(roleFilteredTasks, status);

        List<Task> statusScopedTasks = taskService.filterByStatus(teamFilteredTasks, status);
        int authorTasksCount = taskService
                .filterByRole(statusScopedTasks, TaskRoleFilter.AUTHOR, authUser.getId())
                .size();
        int assigneeTasksCount = taskService
                .filterByRole(statusScopedTasks, TaskRoleFilter.ASSIGNEE, authUser.getId())
                .size();

        TeamTasksStats stats = taskService.getStats(roleFilteredTasks);

        List<TaskWithTeamView> taskCards =
                statusFilteredTasks.stream().map(this::toTaskWithTeamView).toList();

        List<TaskWithTeamView> sortedTaskCards = taskService.sortTaskCards(taskCards, sort);

        MyTasksPageView page = new MyTasksPageView(
                sortedTaskCards,
                teams,
                roleFilteredTasks.size(),
                stats,
                status,
                teamId,
                role,
                sort,
                authorTasksCount,
                assigneeTasksCount);

        model.addAttribute("page", page);
        model.addAttribute("_csrf", csrfToken);

        return "tasks/index";
    }

    @GetMapping("/tasks/new")
    public String newTaskPage(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) Long teamId,
            Model model,
            CsrfToken csrfToken) {
        List<Team> teams = teamService.findByUserId(authUser.getId());

        Long selectedTeamId = teamId;

        if (selectedTeamId == null && !teams.isEmpty()) {
            selectedTeamId = teams.getFirst().getId();
        }

        List<User> members = selectedTeamId == null
                ? List.of()
                : teamMemberService.findByTeamId(selectedTeamId).stream()
                        .map(teamMember -> userService.findById(teamMember.getUserId()))
                        .toList();

        model.addAttribute("teams", teams);
        model.addAttribute("selectedTeamId", selectedTeamId);
        model.addAttribute("members", members);
        model.addAttribute("_csrf", csrfToken);
        model.addAttribute("categories", TaskCategory.values());

        return "tasks/new";
    }

    @GetMapping("/tasks/{id}")
    public String showTask(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long id, Model model, CsrfToken csrfToken) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());

        Team team = teamService.findById(task.getTeamId());
        User author = userService.findById(task.getAuthorId());
        User assignee = userService.findById(task.getAssigneeId());

        List<User> members = getTeamUsers(task.getTeamId());

        List<CommentView> comments = commentService.findByTaskId(id).stream()
                .map(comment -> {
                    User user = userService.findById(comment.getUserId());

                    return new CommentView(
                            comment.getId(),
                            user.getName(),
                            comment.getMessage(),
                            comment.getCreatedAt(),
                            comment.getUpdatedAt());
                })
                .toList();

        model.addAttribute("task", TaskView.from(task, author.getName(), assignee.getName()));
        model.addAttribute("team", team);
        model.addAttribute("members", members);
        model.addAttribute("categories", TaskCategory.values());
        model.addAttribute("comments", comments);
        model.addAttribute("_csrf", csrfToken);

        return "tasks/show";
    }

    @GetMapping("/tasks/{id}/edit")
    public String editTaskPage(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long id, Model model, CsrfToken csrfToken) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());

        Team team = teamService.findById(task.getTeamId());
        User author = userService.findById(task.getAuthorId());
        User assignee = userService.findById(task.getAssigneeId());

        List<User> members = getTeamUsers(task.getTeamId());

        model.addAttribute("task", TaskView.from(task, author.getName(), assignee.getName()));
        model.addAttribute("team", team);
        model.addAttribute("members", members);
        model.addAttribute("categories", TaskCategory.values());
        model.addAttribute("_csrf", csrfToken);

        return "tasks/edit";
    }

    @PostMapping("/tasks/{id}/edit")
    public String updateTask(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) LocalDate deadlineDate,
            @RequestParam TaskCategory category,
            @RequestParam UUID assigneeId) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        teamMemberService.findById(task.getTeamId(), assigneeId);
        taskService.updateDetails(id, title, description, deadlineDate, category, assigneeId);

        return REDIRECT_TASKS_PREFIX + id;
    }

    @PostMapping("/tasks/{id}/comments")
    public String createComment(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long id, @RequestParam String message) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        commentService.create(id, authUser.getId(), message);

        return REDIRECT_TASKS_PREFIX + id;
    }

    @PostMapping("/tasks/{taskId}/comments/{commentId}/edit")
    public String updateComment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @RequestParam String message) {
        Task task = taskService.findById(taskId);
        Comment comment = commentService.findById(commentId);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        checkCommentBelongsToTask(task, comment);
        commentService.updateMessage(commentId, message);

        return REDIRECT_TASKS_PREFIX + taskId;
    }

    @PostMapping("/tasks/{taskId}/comments/{commentId}/delete")
    public String deleteComment(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long taskId, @PathVariable Long commentId) {
        Task task = taskService.findById(taskId);
        Comment comment = commentService.findById(commentId);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        checkCommentBelongsToTask(task, comment);
        commentService.deleteById(commentId);

        return REDIRECT_TASKS_PREFIX + taskId;
    }

    @PostMapping("/tasks")
    public String createTask(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam Long teamId,
            @RequestParam UUID assigneeId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) LocalDate deadlineDate,
            @RequestParam TaskCategory category) {
        teamMemberService.findById(teamId, authUser.getId());
        teamMemberService.findById(teamId, assigneeId);

        taskService.create(teamId, authUser.getId(), assigneeId, title, description, deadlineDate, category);

        return REDIRECT_TEAMS_PREFIX + teamId;
    }

    @PostMapping("/tasks/{id}/status")
    public String updateTaskStatus(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam TaskStatus status,
            @RequestParam(required = false) TaskStatus selectedStatus,
            @RequestParam(required = false) TaskSort selectedSort,
            @RequestParam(required = false) Long selectedTeamId,
            @RequestParam(required = false) TaskRoleFilter selectedRole,
            @RequestParam(required = false) String returnTo) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        taskService.updateStatus(id, status);

        return buildRedirectAfterInlineUpdate(
                task, selectedStatus, selectedSort, selectedTeamId, selectedRole, returnTo);
    }

    @PostMapping("/tasks/{id}/category")
    public String updateTaskCategory(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam TaskCategory category,
            @RequestParam(required = false) TaskStatus selectedStatus,
            @RequestParam(required = false) TaskSort selectedSort,
            @RequestParam(required = false) Long selectedTeamId,
            @RequestParam(required = false) TaskRoleFilter selectedRole,
            @RequestParam(required = false) String returnTo) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        taskService.updateCategory(id, category);

        return buildRedirectAfterInlineUpdate(
                task, selectedStatus, selectedSort, selectedTeamId, selectedRole, returnTo);
    }

    @PostMapping("/tasks/{id}/author")
    public String updateTaskAuthor(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam UUID authorId,
            @RequestParam(required = false) TaskStatus selectedStatus,
            @RequestParam(required = false) TaskSort selectedSort,
            @RequestParam(required = false) Long selectedTeamId,
            @RequestParam(required = false) TaskRoleFilter selectedRole,
            @RequestParam(required = false) String returnTo) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        teamMemberService.findById(task.getTeamId(), authorId);
        taskService.updateAuthor(id, authorId);

        return buildRedirectAfterInlineUpdate(
                task, selectedStatus, selectedSort, selectedTeamId, selectedRole, returnTo);
    }

    @PostMapping("/tasks/{id}/assignee")
    public String updateTaskAssignee(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam UUID assigneeId,
            @RequestParam(required = false) TaskStatus selectedStatus,
            @RequestParam(required = false) TaskSort selectedSort,
            @RequestParam(required = false) Long selectedTeamId,
            @RequestParam(required = false) TaskRoleFilter selectedRole,
            @RequestParam(required = false) String returnTo) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        teamMemberService.findById(task.getTeamId(), assigneeId);
        taskService.updateAssignee(id, assigneeId);

        return buildRedirectAfterInlineUpdate(
                task, selectedStatus, selectedSort, selectedTeamId, selectedRole, returnTo);
    }

    @PostMapping("/tasks/{id}/deadline")
    public String updateTaskDeadline(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate deadlineDate,
            @RequestParam(required = false) TaskStatus selectedStatus,
            @RequestParam(required = false) TaskSort selectedSort,
            @RequestParam(required = false) Long selectedTeamId,
            @RequestParam(required = false) TaskRoleFilter selectedRole,
            @RequestParam(required = false) String returnTo) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        taskService.updateDeadline(id, deadlineDate);

        return buildRedirectAfterInlineUpdate(
                task, selectedStatus, selectedSort, selectedTeamId, selectedRole, returnTo);
    }

    @PostMapping("/tasks/{id}/archive")
    public String archiveTask(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam(required = false) String returnTo) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        taskService.archive(id);

        if ("task".equals(returnTo)) {
            return REDIRECT_TASKS_PREFIX + id;
        }

        return REDIRECT_TEAMS_PREFIX + task.getTeamId();
    }

    @PostMapping("/tasks/{id}/restore")
    public String restoreTaskFromArchive(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam(required = false) String returnTo) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        taskService.restoreFromArchive(id);

        if ("task".equals(returnTo)) {
            return REDIRECT_TASKS_PREFIX + id;
        }

        return REDIRECT_TEAMS_PREFIX + task.getTeamId();
    }

    private TaskWithTeamView toTaskWithTeamView(Task task) {
        Team team = teamService.findById(task.getTeamId());
        User author = userService.findById(task.getAuthorId());
        User assignee = userService.findById(task.getAssigneeId());
        List<User> members = getTeamUsers(task.getTeamId());

        return new TaskWithTeamView(
                TaskView.from(task, author.getName(), assignee.getName()), team.getId(), team.getName(), members);
    }

    private List<User> getTeamUsers(Long teamId) {
        return teamMemberService.findByTeamId(teamId).stream()
                .map(teamMember -> userService.findById(teamMember.getUserId()))
                .toList();
    }

    private String buildTeamRedirect(Long teamId, TaskStatus selectedStatus, TaskSort selectedSort) {
        StringBuilder redirect = new StringBuilder(REDIRECT_TEAMS_PREFIX).append(teamId);
        boolean hasQueryParams = false;

        if (selectedStatus != null) {
            redirect.append("?status=").append(selectedStatus.name());
            hasQueryParams = true;
        }

        if (selectedSort != null) {
            redirect.append(hasQueryParams ? "&" : "?");
            redirect.append("sort=").append(selectedSort.name());
        }

        return redirect.toString();
    }

    private String buildTasksRedirect(
            TaskStatus selectedStatus, TaskSort selectedSort, Long selectedTeamId, TaskRoleFilter selectedRole) {
        StringJoiner query = new StringJoiner("&");

        if (selectedStatus != null) {
            query.add("status=" + selectedStatus.name());
        }

        if (selectedTeamId != null) {
            query.add("teamId=" + selectedTeamId);
        }

        if (selectedRole != null) {
            query.add("role=" + selectedRole.name());
        }

        if (selectedSort != null) {
            query.add("sort=" + selectedSort.name());
        }

        String queryString = query.toString();

        if (queryString.isBlank()) {
            return "redirect:/tasks";
        }

        return "redirect:/tasks?" + queryString;
    }

    private String buildRedirectAfterInlineUpdate(
            Task task,
            TaskStatus selectedStatus,
            TaskSort selectedSort,
            Long selectedTeamId,
            TaskRoleFilter selectedRole,
            String returnTo) {
        if ("task".equals(returnTo)) {
            return REDIRECT_TASKS_PREFIX + task.getId();
        }

        if ("tasks".equals(returnTo)) {
            return buildTasksRedirect(selectedStatus, selectedSort, selectedTeamId, selectedRole);
        }

        return buildTeamRedirect(task.getTeamId(), selectedStatus, selectedSort);
    }

    private void checkCommentBelongsToTask(Task task, Comment comment) {
        if (!comment.getTaskId().equals(task.getId())) {
            throw new IllegalArgumentException("Comment does not belong to task " + task.getId());
        }
    }
}
