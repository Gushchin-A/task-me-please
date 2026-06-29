package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.exception.TeamMemberAlreadyExistsException;
import dev.gushchin.taskmanager.exception.TeamMemberNotFoundException;
import dev.gushchin.taskmanager.exception.UserNotFoundByEmailException;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskListMode;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamMemberRole;
import dev.gushchin.taskmanager.model.TeamTaskVisibility;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.TaskService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.UserService;
import dev.gushchin.taskmanager.view.TaskView;
import dev.gushchin.taskmanager.view.TeamMemberView;
import dev.gushchin.taskmanager.view.TeamPageView;
import dev.gushchin.taskmanager.view.TeamPageView.TeamPageCounts;
import dev.gushchin.taskmanager.view.TeamPageView.TeamPageFilters;
import dev.gushchin.taskmanager.view.TeamTasksStats;
import java.util.List;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class TeamPageController {
    private static final String CAN_INVITE_ATTRIBUTE = "canInvite";
    private static final String CSRF_ATTRIBUTE = "_csrf";
    private static final String ERROR_MESSAGE_ATTRIBUTE = "errorMessage";
    private static final String INVITE_PATH_SUFFIX = "/invite";
    private static final String NOT_FOUND_VIEW = "teams/not-found";
    private static final String OWNER_INVITE_REQUIRED_MESSAGE_PREFIX =
            "Только owner команды может приглашать новых участников. ";
    private static final String OWNER_INVITE_REQUIRED_MESSAGE_SUFFIX = "Вы можете пока только просматривать команду.";
    private static final String OWNER_INVITE_REQUIRED_MESSAGE =
            OWNER_INVITE_REQUIRED_MESSAGE_PREFIX + OWNER_INVITE_REQUIRED_MESSAGE_SUFFIX;
    private static final String REDIRECT_TEAMS_PREFIX = "redirect:/teams/";
    private static final String SUCCESS_MESSAGE_ATTRIBUTE = "successMessage";
    private static final String TEAM_ATTRIBUTE = "team";
    private static final String PAGE_ATTRIBUTE = "page";
    private static final String TEAMS_SHOW_VIEW = "teams/show";

    private final TeamService teamService;
    private final TaskService taskService;
    private final UserService userService;
    private final TeamMemberService teamMemberService;

    @GetMapping("/teams")
    public String teamsPage(@AuthenticationPrincipal AuthUser authUser, Model model) {
        List<Team> teams = teamService.findByUserId(authUser.getId());

        model.addAttribute("teams", teams);
        model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, null);

        return "teams/index";
    }

    @GetMapping("/teams/new")
    public String newTeamPage(Model model, CsrfToken csrfToken) {
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);

        return "teams/new";
    }

    @GetMapping("/teams/{id}")
    public String showTeam(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            TeamTaskFilterRequest request,
            Model model,
            CsrfToken csrfToken) {
        TeamMember currentMember;

        try {
            currentMember = teamMemberService.findById(id, authUser.getId());
        } catch (TeamMemberNotFoundException ex) {
            return NOT_FOUND_VIEW;
        }

        TaskStatus status = request.getStatus();
        TaskSort sort = request.getSort();
        UUID authorId = request.getAuthorId();
        UUID assigneeId = request.getAssigneeId();

        Team team = teamService.findById(id);
        List<Task> allTasks = taskService.findByTeamId(id);
        List<Task> visibleTasks = taskService.filterByVisibility(allTasks, authUser.getId());
        List<Task> modeFilteredTasks = taskService.filterByArchived(visibleTasks, false);
        List<TeamMember> teamMembers = teamMemberService.findByTeamId(id);

        List<User> members = teamMembers.stream()
                .map(teamMember -> userService.findById(teamMember.getUserId()))
                .toList();

        TeamTasksStats stats = taskService.getStats(modeFilteredTasks);

        List<Task> statusFilteredTasks = taskService.filterByStatus(modeFilteredTasks, status);
        List<Task> authorFilteredTasks = taskService.filterByAuthorId(statusFilteredTasks, authorId);
        List<Task> assigneeFilteredTasks = taskService.filterByAssigneeId(authorFilteredTasks, assigneeId);
        List<Task> sortedTasks = taskService.sortTasks(assigneeFilteredTasks, sort);

        List<TaskView> taskViews = sortedTasks.stream()
                .map(task -> toTaskView(task, authUser.getId()))
                .toList();

        boolean canInvite = currentMember.getRole() == TeamMemberRole.OWNER;

        TeamPageView page = new TeamPageView(
                team,
                taskViews,
                members,
                new TeamPageCounts(modeFilteredTasks.size(), sortedTasks.size(), teamMembers.size()),
                stats,
                new TeamPageFilters(status, sort, authorId, assigneeId),
                canInvite,
                TaskListMode.ACTIVE);

        model.addAttribute(PAGE_ATTRIBUTE, page);
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);

        return TEAMS_SHOW_VIEW;
    }

    @GetMapping("/teams/{id}/archive")
    public String showTeamArchive(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            TeamTaskFilterRequest request,
            Model model,
            CsrfToken csrfToken) {
        TeamMember currentMember;

        try {
            currentMember = teamMemberService.findById(id, authUser.getId());
        } catch (TeamMemberNotFoundException ex) {
            return NOT_FOUND_VIEW;
        }

        TaskStatus status = request.getStatus();
        TaskSort sort = request.getSort();
        UUID authorId = request.getAuthorId();
        UUID assigneeId = request.getAssigneeId();

        Team team = teamService.findById(id);
        List<Task> allTasks = taskService.findByTeamId(id);
        List<Task> visibleTasks = taskService.filterByVisibility(allTasks, authUser.getId());
        List<Task> modeFilteredTasks = taskService.filterByArchived(visibleTasks, true);
        List<TeamMember> teamMembers = teamMemberService.findByTeamId(id);

        List<User> members = teamMembers.stream()
                .map(teamMember -> userService.findById(teamMember.getUserId()))
                .toList();

        TeamTasksStats stats = taskService.getStats(modeFilteredTasks);

        List<Task> statusFilteredTasks = taskService.filterByStatus(modeFilteredTasks, status);
        List<Task> authorFilteredTasks = taskService.filterByAuthorId(statusFilteredTasks, authorId);
        List<Task> assigneeFilteredTasks = taskService.filterByAssigneeId(authorFilteredTasks, assigneeId);
        List<Task> sortedTasks = taskService.sortTasks(assigneeFilteredTasks, sort);

        List<TaskView> taskViews = sortedTasks.stream()
                .map(task -> {
                    User author = userService.findById(task.getAuthorId());
                    User assignee = userService.findById(task.getAssigneeId());

                    return TaskView.from(task, author.getName(), assignee.getName());
                })
                .toList();

        boolean canInvite = currentMember.getRole() == TeamMemberRole.OWNER;

        TeamPageView page = new TeamPageView(
                team,
                taskViews,
                members,
                new TeamPageCounts(modeFilteredTasks.size(), sortedTasks.size(), members.size()),
                stats,
                new TeamPageFilters(status, sort, authorId, assigneeId),
                canInvite,
                TaskListMode.ARCHIVE);

        model.addAttribute(PAGE_ATTRIBUTE, page);
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);

        return TEAMS_SHOW_VIEW;
    }

    @GetMapping("/teams/{id}/members")
    public String showTeamMembers(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long id, Model model, CsrfToken csrfToken) {
        TeamMember currentMember;

        try {
            currentMember = teamMemberService.findById(id, authUser.getId());
        } catch (TeamMemberNotFoundException ex) {
            return NOT_FOUND_VIEW;
        }

        Team team = teamService.findById(id);
        List<Task> tasks = taskService.findByTeamId(id);
        List<TeamMember> members = teamMemberService.findByTeamId(id);

        boolean canManageVisibility = currentMember.getRole() == TeamMemberRole.OWNER;
        boolean canSeeMemberDetails = currentMember.getRole() == TeamMemberRole.OWNER
                || currentMember.getTaskVisibility() == TeamTaskVisibility.ALL_TASKS;
        boolean canSeeMemberPrivateData = currentMember.getRole() == TeamMemberRole.OWNER;

        List<TeamMemberView> memberViews = members.stream()
                .map(member -> {
                    User user = userService.findById(member.getUserId());

                    long authorTasksCount = tasks.stream()
                            .filter(task -> task.getAuthorId().equals(user.getId()))
                            .count();

                    long assigneeTasksCount = tasks.stream()
                            .filter(task -> task.getAssigneeId().equals(user.getId()))
                            .count();

                    return new TeamMemberView(
                            user.getId(),
                            user.getName(),
                            user.getEmail(),
                            member.getRole(),
                            authorTasksCount,
                            assigneeTasksCount,
                            member.getTaskVisibility());
                })
                .toList();

        boolean onlyCurrentOwnerInTeam = members.size() == 1
                && currentMember.getRole() == TeamMemberRole.OWNER
                && currentMember.getUserId().equals(authUser.getId());

        boolean canInvite = currentMember.getRole() == TeamMemberRole.OWNER;

        model.addAttribute(TEAM_ATTRIBUTE, team);
        model.addAttribute("totalTasksCount", tasks.size());
        model.addAttribute("membersCount", members.size());
        model.addAttribute("members", memberViews);
        model.addAttribute("onlyCurrentOwnerInTeam", onlyCurrentOwnerInTeam);
        model.addAttribute(CAN_INVITE_ATTRIBUTE, canInvite);
        model.addAttribute("currentUserId", authUser.getId());
        model.addAttribute("canManageVisibility", canManageVisibility);
        model.addAttribute("canSeeMemberDetails", canSeeMemberDetails);
        model.addAttribute("canSeeMemberPrivateData", canSeeMemberPrivateData);
        model.addAttribute("taskVisibilities", TeamTaskVisibility.values());
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);

        return "teams/members";
    }

    @GetMapping("/teams/{id}/invite")
    public String inviteMemberPage(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long id, Model model, CsrfToken csrfToken) {
        TeamMember currentMember;

        try {
            currentMember = teamMemberService.findById(id, authUser.getId());
        } catch (TeamMemberNotFoundException ex) {
            return NOT_FOUND_VIEW;
        }

        Team team = teamService.findById(id);
        boolean canInvite = currentMember.getRole() == TeamMemberRole.OWNER;

        model.addAttribute(TEAM_ATTRIBUTE, team);
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);
        model.addAttribute(CAN_INVITE_ATTRIBUTE, canInvite);

        if (!model.containsAttribute(SUCCESS_MESSAGE_ATTRIBUTE)) {
            model.addAttribute(SUCCESS_MESSAGE_ATTRIBUTE, null);
        }

        if (!model.containsAttribute(ERROR_MESSAGE_ATTRIBUTE)) {
            model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, null);
        }

        if (!canInvite) {
            model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, OWNER_INVITE_REQUIRED_MESSAGE);
        }

        return "teams/invite";
    }

    @PostMapping("/teams/{teamId}/members/{userId}/visibility")
    public String updateMemberTaskVisibility(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long teamId,
            @PathVariable UUID userId,
            @RequestParam TeamTaskVisibility taskVisibility) {
        teamMemberService.updateTaskVisibility(teamId, userId, taskVisibility, authUser.getId());

        return REDIRECT_TEAMS_PREFIX + teamId + "/members";
    }

    @PostMapping("/teams/{id}/members")
    public String addMember(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {
        TeamMember currentMember;

        try {
            currentMember = teamMemberService.findById(id, authUser.getId());
        } catch (TeamMemberNotFoundException ex) {
            return NOT_FOUND_VIEW;
        }

        String redirect = REDIRECT_TEAMS_PREFIX + id + INVITE_PATH_SUFFIX;

        if (currentMember.getRole() != TeamMemberRole.OWNER) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, OWNER_INVITE_REQUIRED_MESSAGE);
        } else {
            addMemberByEmail(id, email, redirectAttributes);
        }

        return redirect;
    }

    @PostMapping("/teams")
    public String createTeam(@AuthenticationPrincipal AuthUser authUser, @RequestParam String name) {
        teamService.create(name, authUser.getId());

        return "redirect:/teams";
    }

    private void addMemberByEmail(Long teamId, String email, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByEmail(email);
            teamMemberService.addMember(teamId, user.getId());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, "Пользователь добавлен в команду.");
        } catch (UserNotFoundByEmailException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, "Пользователь с такой почтой не найден.");
        } catch (TeamMemberAlreadyExistsException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, "Пользователь уже состоит в этой команде.");
        }
    }

    private TaskView toTaskView(Task task, UUID userId) {
        User author = userService.findById(task.getAuthorId());
        User assignee = userService.findById(task.getAssigneeId());

        boolean canUpdateTask = taskService.canUpdateTask(task, userId);
        boolean canUpdateStatus = taskService.canUpdateStatus(task, userId);
        boolean canArchive = taskService.canArchiveTask(task, userId);
        boolean canRestore = taskService.canRestoreTask(task, userId);
        boolean showAuthorChangeWarning = task.getAuthorId().equals(userId) && !taskService.isTeamOwner(task, userId);

        TaskView.TaskState state = new TaskView.TaskState(
                task.isArchived(), canUpdateTask, canUpdateStatus, canArchive, canRestore, showAuthorChangeWarning);

        return TaskView.from(task, author.getName(), assignee.getName(), state);
    }
}
