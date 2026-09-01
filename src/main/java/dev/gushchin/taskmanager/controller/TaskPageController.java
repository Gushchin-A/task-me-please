package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.exception.TeamInvitationNotFoundException;
import dev.gushchin.taskmanager.exception.TeamInvitationNotPendingException;
import dev.gushchin.taskmanager.model.Comment;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskDetailsUpdate;
import dev.gushchin.taskmanager.model.TaskListMode;
import dev.gushchin.taskmanager.model.TaskRoleFilter;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamTag;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.CommentService;
import dev.gushchin.taskmanager.service.TaskService;
import dev.gushchin.taskmanager.service.TeamInvitationService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.TeamTagService;
import dev.gushchin.taskmanager.service.UserService;
import dev.gushchin.taskmanager.view.CommentView;
import dev.gushchin.taskmanager.view.InvitationDecisionView;
import dev.gushchin.taskmanager.view.MyTasksFilterRequest;
import dev.gushchin.taskmanager.view.MyTasksPageView;
import dev.gushchin.taskmanager.view.MyTasksPageView.MyTasksPageFilters;
import dev.gushchin.taskmanager.view.MyTasksPageView.MyTasksPageResources;
import dev.gushchin.taskmanager.view.MyTasksPageView.MyTasksRoleCounts;
import dev.gushchin.taskmanager.view.TaskParticipantView;
import dev.gushchin.taskmanager.view.TaskView;
import dev.gushchin.taskmanager.view.TaskWithTeamView;
import dev.gushchin.taskmanager.view.TeamTasksStats;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class TaskPageController {
    private static final String TAGS_ATTRIBUTE = "tags";
    private static final String CSRF_ATTRIBUTE = "_csrf";
    private static final String MEMBERS_ATTRIBUTE = "members";
    private static final String NAVIGATION_TEAMS_ATTRIBUTE = "navigationTeams";
    private static final String REDIRECT_TEAMS_PREFIX = "redirect:/teams/";
    private static final String REDIRECT_TASKS = "redirect:/tasks";
    private static final String REDIRECT_TASKS_PREFIX = "redirect:/tasks/";
    private static final String RETURN_TO_TASK = "task";
    private static final String RETURN_TO_TASKS = "tasks";
    private static final String RETURN_TO_TASKS_ARCHIVE = "tasksArchive";
    private static final String RETURN_TO_TEAM_ARCHIVE = "teamArchive";
    private static final String SORT_QUERY_PARAM = "sort=";
    private static final String TASK_ATTRIBUTE = RETURN_TO_TASK;
    private static final String TEAM_ATTRIBUTE = "team";
    private static final String PAGE_ATTRIBUTE = "page";
    private static final String TASKS_INDEX_VIEW = "tasks/index";
    private static final String SUCCESS_MESSAGE_ATTRIBUTE = "successMessage";
    private static final String INVITATION_DECISION_ATTRIBUTE = "invitationDecision";

    private final TeamService teamService;
    private final TaskService taskService;
    private final TeamMemberService teamMemberService;
    private final UserService userService;
    private final CommentService commentService;
    private final TeamTagService teamTagService;
    private final TeamInvitationService teamInvitationService;

    @GetMapping("/tasks")
    public String tasksPage(
            @AuthenticationPrincipal AuthUser authUser,
            @ModelAttribute MyTasksFilterRequest filters,
            @RequestParam(required = false) String invitation,
            Model model,
            CsrfToken csrfToken) {
        TaskStatus status = filters.getStatus();
        Long teamId = filters.getTeamId();
        TaskRoleFilter role = filters.getRole();
        TaskSort sort = filters.getSort();
        UUID authorId = filters.getAuthorId();
        UUID assigneeId = filters.getAssigneeId();
        Long tagId = filters.getTagId();
        List<Team> teams = teamService.findByUserId(authUser.getId());
        List<Long> teamIds = teams.stream().map(Team::getId).toList();

        List<Task> allTasks = taskService.findByTeamIds(teamIds);
        List<Task> visibleTasks = taskService.filterByVisibility(allTasks, authUser.getId());
        List<Task> modeFilteredTasks = taskService.filterByArchived(visibleTasks, false);
        List<Task> teamFilteredTasks = taskService.filterByTeamId(modeFilteredTasks, teamId);
        List<Task> roleFilteredTasks = taskService.filterByRole(teamFilteredTasks, role, authUser.getId());
        List<Task> statusFilteredTasks = taskService.filterByStatus(roleFilteredTasks, status);
        List<Task> tagFilteredTasks = taskService.filterByTagId(statusFilteredTasks, tagId);
        List<Task> authorFilteredTasks = taskService.filterByAuthorId(tagFilteredTasks, authorId);
        List<Task> assigneeFilteredTasks = taskService.filterByAssigneeId(authorFilteredTasks, assigneeId);

        List<Task> statusScopedTasks = taskService.filterByStatus(teamFilteredTasks, status);
        int authorTasksCount = taskService
                .filterByRole(statusScopedTasks, TaskRoleFilter.AUTHOR, authUser.getId())
                .size();
        int assigneeTasksCount = taskService
                .filterByRole(statusScopedTasks, TaskRoleFilter.ASSIGNEE, authUser.getId())
                .size();

        TeamTasksStats stats = taskService.getStats(roleFilteredTasks);

        List<TaskWithTeamView> taskCards = assigneeFilteredTasks.stream()
                .map(task -> toTaskWithTeamView(task, authUser.getId()))
                .toList();

        List<TaskWithTeamView> sortedTaskCards = taskService.sortTaskCards(taskCards, sort);

        MyTasksPageView page = new MyTasksPageView(
                sortedTaskCards,
                new MyTasksPageResources(teams, getFilterParticipants(visibleTasks), getTeamTags(teams)),
                roleFilteredTasks.size(),
                stats,
                new MyTasksPageFilters(status, teamId, role, sort, authorId, assigneeId, tagId),
                new MyTasksRoleCounts(authorTasksCount, assigneeTasksCount),
                !visibleTasks.isEmpty(),
                TaskListMode.ACTIVE);

        model.addAttribute(PAGE_ATTRIBUTE, page);
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);
        InvitationDecisionView invitationDecision = buildInvitationDecision(invitation, authUser.getId());
        if (invitationDecision != null) {
            model.addAttribute(INVITATION_DECISION_ATTRIBUTE, invitationDecision);
        }

        return TASKS_INDEX_VIEW;
    }

    @GetMapping("/tasks/archive")
    public String archivedTasksPage(
            @AuthenticationPrincipal AuthUser authUser,
            @ModelAttribute MyTasksFilterRequest filters,
            Model model,
            CsrfToken csrfToken) {
        TaskStatus status = filters.getStatus();
        Long teamId = filters.getTeamId();
        TaskRoleFilter role = filters.getRole();
        TaskSort sort = filters.getSort();
        UUID authorId = filters.getAuthorId();
        UUID assigneeId = filters.getAssigneeId();
        Long tagId = filters.getTagId();
        List<Team> teams = teamService.findByUserId(authUser.getId());
        List<Long> teamIds = teams.stream().map(Team::getId).toList();

        List<Task> allTasks = taskService.findByTeamIds(teamIds);
        List<Task> visibleTasks = taskService.filterByVisibility(allTasks, authUser.getId());
        List<Task> modeFilteredTasks = taskService.filterByArchived(visibleTasks, true);
        List<Task> teamFilteredTasks = taskService.filterByTeamId(modeFilteredTasks, teamId);
        List<Task> roleFilteredTasks = taskService.filterByRole(teamFilteredTasks, role, authUser.getId());
        List<Task> statusFilteredTasks = taskService.filterByStatus(roleFilteredTasks, status);
        List<Task> tagFilteredTasks = taskService.filterByTagId(statusFilteredTasks, tagId);
        List<Task> authorFilteredTasks = taskService.filterByAuthorId(tagFilteredTasks, authorId);
        List<Task> assigneeFilteredTasks = taskService.filterByAssigneeId(authorFilteredTasks, assigneeId);

        List<Task> statusScopedTasks = taskService.filterByStatus(teamFilteredTasks, status);
        int authorTasksCount = taskService
                .filterByRole(statusScopedTasks, TaskRoleFilter.AUTHOR, authUser.getId())
                .size();
        int assigneeTasksCount = taskService
                .filterByRole(statusScopedTasks, TaskRoleFilter.ASSIGNEE, authUser.getId())
                .size();

        TeamTasksStats stats = taskService.getStats(roleFilteredTasks);

        List<TaskWithTeamView> taskCards = assigneeFilteredTasks.stream()
                .map(task -> toTaskWithTeamView(task, authUser.getId()))
                .toList();

        List<TaskWithTeamView> sortedTaskCards = taskService.sortTaskCards(taskCards, sort);

        MyTasksPageView page = new MyTasksPageView(
                sortedTaskCards,
                new MyTasksPageResources(teams, getFilterParticipants(visibleTasks), getTeamTags(teams)),
                roleFilteredTasks.size(),
                stats,
                new MyTasksPageFilters(status, teamId, role, sort, authorId, assigneeId, tagId),
                new MyTasksRoleCounts(authorTasksCount, assigneeTasksCount),
                !visibleTasks.isEmpty(),
                TaskListMode.ARCHIVE);

        model.addAttribute(PAGE_ATTRIBUTE, page);
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);

        return TASKS_INDEX_VIEW;
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

        List<TaskParticipantView> members = selectedTeamId == null ? List.of() : getTeamUsers(selectedTeamId);

        List<TeamTag> tags = selectedTeamId == null ? List.of() : teamTagService.findByTeamId(selectedTeamId);

        model.addAttribute("teams", teams);
        model.addAttribute("selectedTeamId", selectedTeamId);
        model.addAttribute(MEMBERS_ATTRIBUTE, members);
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);
        model.addAttribute(TAGS_ATTRIBUTE, tags);

        return "tasks/new";
    }

    @GetMapping("/tasks/{id}")
    public String showTask(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long id, Model model, CsrfToken csrfToken) {
        Task task = taskService.findByIdForUser(id, authUser.getId());

        teamMemberService.findById(task.getTeamId(), authUser.getId());

        Team team = teamService.findById(task.getTeamId());
        List<TaskParticipantView> members = getTeamUsers(task.getTeamId());

        List<CommentView> comments = commentService.findByTaskId(id).stream()
                .map(comment -> {
                    User user = userService.findById(comment.getUserId());

                    boolean canEditComment = comment.getUserId().equals(authUser.getId());

                    return new CommentView(
                            comment.getId(),
                            user.getName(),
                            user.getEmail(),
                            comment.getMessage(),
                            comment.getCreatedAt(),
                            comment.getUpdatedAt(),
                            canEditComment);
                })
                .toList();

        model.addAttribute(TASK_ATTRIBUTE, toTaskView(task, authUser.getId()));
        model.addAttribute(TEAM_ATTRIBUTE, team);
        model.addAttribute(NAVIGATION_TEAMS_ATTRIBUTE, teamService.findByUserId(authUser.getId()));
        model.addAttribute(MEMBERS_ATTRIBUTE, members);
        model.addAttribute(TAGS_ATTRIBUTE, teamTagService.findByTeamId(task.getTeamId()));
        model.addAttribute("comments", comments);
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);

        return "tasks/show";
    }

    @GetMapping("/tasks/{id}/edit")
    public String editTaskPage(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long id, Model model, CsrfToken csrfToken) {
        Task task = taskService.findByIdForUser(id, authUser.getId());

        teamMemberService.findById(task.getTeamId(), authUser.getId());

        Team team = teamService.findById(task.getTeamId());
        List<TaskParticipantView> members = getTeamUsers(task.getTeamId());

        model.addAttribute(TASK_ATTRIBUTE, toTaskView(task, authUser.getId()));
        model.addAttribute(TEAM_ATTRIBUTE, team);
        model.addAttribute(MEMBERS_ATTRIBUTE, members);
        model.addAttribute(TAGS_ATTRIBUTE, teamTagService.findByTeamId(task.getTeamId()));
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);

        return "tasks/edit";
    }

    @PostMapping("/tasks/{id}/edit")
    public String updateTask(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            InlineTaskUpdateRequest request,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes redirectAttributes) {
        Task task = taskService.findById(id);
        String updatedDescription = request.getDescription() == null ? task.getDescription() : request.getDescription();
        TaskStatus updatedStatus = request.getStatus() == null ? task.getStatus() : request.getStatus();
        UUID updatedAuthorId = request.getAuthorId() == null ? task.getAuthorId() : request.getAuthorId();
        final TaskDetailsUpdate update = new TaskDetailsUpdate(
                request.getTitle(),
                updatedDescription,
                request.getDeadlineDate(),
                updatedStatus,
                request.getTagId(),
                updatedAuthorId,
                request.getAssigneeId());

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        teamMemberService.findById(task.getTeamId(), updatedAuthorId);
        teamMemberService.findById(task.getTeamId(), request.getAssigneeId());
        taskService.updateDetails(id, update, authUser.getId());
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, "Задача успешно изменена");

        return buildRedirectAfterInlineUpdate(task, request, returnTo);
    }

    @PostMapping("/tasks/{id}/comments")
    public String createComment(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long id, @RequestParam String message) {
        Task task = taskService.findByIdForUser(id, authUser.getId());

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
        Task task = taskService.findByIdForUser(taskId, authUser.getId());
        Comment comment = commentService.findById(commentId);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        checkCommentBelongsToTask(task, comment);
        commentService.updateMessage(commentId, message, authUser.getId());

        return REDIRECT_TASKS_PREFIX + taskId;
    }

    @PostMapping("/tasks/{taskId}/comments/{commentId}/delete")
    public String deleteComment(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long taskId, @PathVariable Long commentId) {
        Task task = taskService.findByIdForUser(taskId, authUser.getId());
        Comment comment = commentService.findById(commentId);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        checkCommentBelongsToTask(task, comment);
        commentService.deleteById(commentId, authUser.getId());

        return REDIRECT_TASKS_PREFIX + taskId;
    }

    @PostMapping("/tasks")
    public String createTask(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam Long teamId,
            @RequestParam UUID assigneeId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam LocalDate deadlineDate,
            @RequestParam Long tagId) {
        teamMemberService.findById(teamId, authUser.getId());
        teamMemberService.findById(teamId, assigneeId);

        taskService.create(teamId, authUser.getId(), assigneeId, title, description, deadlineDate, tagId);

        return REDIRECT_TEAMS_PREFIX + teamId;
    }

    @PostMapping("/tasks/{id}/status")
    public String updateTaskStatus(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam TaskStatus status,
            InlineTaskUpdateRequest request,
            @RequestParam(required = false) String returnTo) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        taskService.updateStatus(id, status, authUser.getId());

        return buildRedirectAfterInlineUpdate(task, request, returnTo);
    }

    @PostMapping("/tasks/{id}/tag")
    public String updateTaskTag(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam Long tagId,
            InlineTaskUpdateRequest request,
            @RequestParam(required = false) String returnTo) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        taskService.updateTag(id, tagId, authUser.getId());

        return buildRedirectAfterInlineUpdate(task, request, returnTo);
    }

    @PostMapping("/tasks/{id}/author")
    public String updateTaskAuthor(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam UUID authorId,
            InlineTaskUpdateRequest request,
            @RequestParam(required = false) String returnTo) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        teamMemberService.findById(task.getTeamId(), authorId);
        taskService.updateAuthor(id, authorId, authUser.getId());

        return buildRedirectAfterInlineUpdate(task, request, returnTo);
    }

    @PostMapping("/tasks/{id}/assignee")
    public String updateTaskAssignee(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam UUID assigneeId,
            InlineTaskUpdateRequest request,
            @RequestParam(required = false) String returnTo) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        teamMemberService.findById(task.getTeamId(), assigneeId);
        taskService.updateAssignee(id, assigneeId, authUser.getId());

        return buildRedirectAfterInlineUpdate(task, request, returnTo);
    }

    @PostMapping("/tasks/{id}/deadline")
    public String updateTaskDeadline(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate deadlineDate,
            InlineTaskUpdateRequest request,
            @RequestParam(required = false) String returnTo) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        taskService.updateDeadline(id, deadlineDate, authUser.getId());

        return buildRedirectAfterInlineUpdate(task, request, returnTo);
    }

    @PostMapping("/tasks/{id}/archive")
    public String archiveTask(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            InlineTaskUpdateRequest request,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes redirectAttributes) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        taskService.archive(id, authUser.getId());
        String successMessage = task.getStatus() == TaskStatus.DONE
                ? "Задача была перенесена в архив"
                : "Задача удалена и перенесена в архив";
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, successMessage);

        return buildRedirectAfterInlineUpdate(task, request, returnTo);
    }

    @PostMapping("/tasks/{id}/restore")
    public String restoreTaskFromArchive(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            InlineTaskUpdateRequest request,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes redirectAttributes) {
        Task task = taskService.findById(id);

        teamMemberService.findById(task.getTeamId(), authUser.getId());
        taskService.restoreFromArchive(id, authUser.getId());
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, "Задача восстановлена из архива");

        return buildRedirectAfterInlineUpdate(task, request, returnTo);
    }

    private TaskWithTeamView toTaskWithTeamView(Task task, UUID userId) {
        Team team = teamService.findById(task.getTeamId());
        List<TaskParticipantView> members = getTeamUsers(task.getTeamId());
        List<TeamTag> tags = teamTagService.findByTeamId(task.getTeamId());

        return new TaskWithTeamView(toTaskView(task, userId), team.getId(), team.getName(), members, tags);
    }

    private TaskView toTaskView(Task task, UUID userId) {
        TaskParticipantView author = toTaskParticipant(task.getTeamId(), task.getAuthorId());
        TaskParticipantView assignee = toTaskParticipant(task.getTeamId(), task.getAssigneeId());

        boolean canUpdateTask = taskService.canUpdateTask(task, userId);
        boolean canUpdateStatus = taskService.canUpdateStatus(task, userId);
        boolean canArchive = taskService.canArchiveTask(task, userId);
        boolean canRestore = taskService.canRestoreTask(task, userId);
        boolean showAuthorChangeWarning = task.getAuthorId().equals(userId) && !taskService.isTeamOwner(task, userId);

        TeamTag tag = teamTagService.findById(task.getTagId());

        TaskView.TaskState state = new TaskView.TaskState(
                task.isArchived(), canUpdateTask, canUpdateStatus, canArchive, canRestore, showAuthorChangeWarning);

        return TaskView.from(task, tag.getName(), author, assignee, state);
    }

    private List<TaskParticipantView> getTeamUsers(Long teamId) {
        return teamMemberService.findByTeamId(teamId).stream()
                .map(teamMember -> toTaskParticipant(teamId, teamMember.getUserId()))
                .toList();
    }

    private InvitationDecisionView buildInvitationDecision(String token, UUID currentUserId) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            TeamInvitation invitation = teamInvitationService.findPendingByToken(token);
            if (teamMemberService.isActiveMember(invitation.getTeamId(), currentUserId)) {
                return null;
            }

            Team team = teamService.findById(invitation.getTeamId());
            String invitedByEmail =
                    userService.findById(invitation.getInvitedBy()).getEmail();

            return new InvitationDecisionView(token, team.getName(), invitedByEmail);
        } catch (TeamInvitationNotFoundException | TeamInvitationNotPendingException ex) {
            return null;
        }
    }

    private TaskParticipantView toTaskParticipant(Long teamId, UUID userId) {
        User user = userService.findById(userId);

        return new TaskParticipantView(user.getId(), user.getName(), !teamMemberService.isActiveMember(teamId, userId));
    }

    private List<TaskParticipantView> getFilterParticipants(List<Task> tasks) {
        List<TaskParticipantView> participants = new ArrayList<>();

        for (Task task : tasks) {
            addFilterParticipant(participants, toTaskParticipant(task.getTeamId(), task.getAuthorId()));
            addFilterParticipant(participants, toTaskParticipant(task.getTeamId(), task.getAssigneeId()));
        }

        return participants;
    }

    private void addFilterParticipant(List<TaskParticipantView> participants, TaskParticipantView participant) {
        boolean alreadyAdded = participants.stream()
                .anyMatch(existingParticipant -> existingParticipant.id().equals(participant.id()));

        if (!alreadyAdded) {
            participants.add(participant);
        }
    }

    private List<TeamTag> getTeamTags(List<Team> teams) {
        return teams.stream()
                .flatMap(team -> teamTagService.findByTeamId(team.getId()).stream())
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
            redirect.append(hasQueryParams ? "&" : "?").append(SORT_QUERY_PARAM).append(selectedSort.name());
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
            query.add(SORT_QUERY_PARAM + selectedSort.name());
        }

        String queryString = query.toString();

        if (queryString.isBlank()) {
            return REDIRECT_TASKS;
        }

        return REDIRECT_TASKS + "?" + queryString;
    }

    private String buildRedirectAfterInlineUpdate(Task task, InlineTaskUpdateRequest request, String returnTo) {
        String redirect;

        if (RETURN_TO_TASK.equals(returnTo)) {
            redirect = REDIRECT_TASKS_PREFIX + task.getId();
        } else if (RETURN_TO_TASKS.equals(returnTo)) {
            redirect = buildTasksRedirect(
                    request.getSelectedStatus(),
                    request.getSelectedSort(),
                    request.getSelectedTeamId(),
                    request.getSelectedRole());
        } else if (RETURN_TO_TASKS_ARCHIVE.equals(returnTo)) {
            redirect = "redirect:/tasks/archive";
        } else if (RETURN_TO_TEAM_ARCHIVE.equals(returnTo)) {
            redirect = REDIRECT_TEAMS_PREFIX + task.getTeamId() + "/archive";
        } else {
            redirect = buildTeamRedirect(task.getTeamId(), request.getSelectedStatus(), request.getSelectedSort());
        }

        return redirect;
    }

    private void checkCommentBelongsToTask(Task task, Comment comment) {
        if (!comment.getTaskId().equals(task.getId())) {
            throw new IllegalArgumentException("Comment does not belong to task " + task.getId());
        }
    }
}
