package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.exception.TeamMemberAlreadyExistsException;
import dev.gushchin.taskmanager.exception.TeamMemberNotFoundException;
import dev.gushchin.taskmanager.exception.UserNotFoundByEmailException;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamMemberRole;
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
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskSort sort,
            Model model,
            CsrfToken csrfToken) {
        TeamMember currentMember;

        try {
            currentMember = teamMemberService.findById(id, authUser.getId());
        } catch (TeamMemberNotFoundException ex) {
            return NOT_FOUND_VIEW;
        }

        Team team = teamService.findById(id);
        List<Task> allTasks = taskService.findByTeamId(id);
        List<TeamMember> teamMembers = teamMemberService.findByTeamId(id);

        List<User> members = teamMembers.stream()
                .map(teamMember -> userService.findById(teamMember.getUserId()))
                .toList();

        TeamTasksStats stats = taskService.getStats(allTasks);
        List<Task> filteredTasks = taskService.filterByStatus(allTasks, status);
        List<Task> sortedTasks = taskService.sortTasks(filteredTasks, sort);

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
                new TeamPageCounts(allTasks.size(), teamMembers.size()),
                stats,
                new TeamPageFilters(status, sort),
                canInvite);

        model.addAttribute("page", page);
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);

        return "teams/show";
    }

    @GetMapping("/teams/{id}/members")
    public String showTeamMembers(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id, Model model) {
        TeamMember currentMember;

        try {
            currentMember = teamMemberService.findById(id, authUser.getId());
        } catch (TeamMemberNotFoundException ex) {
            return NOT_FOUND_VIEW;
        }

        Team team = teamService.findById(id);
        List<Task> tasks = taskService.findByTeamId(id);
        List<TeamMember> members = teamMemberService.findByTeamId(id);

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
                            user.getName(), user.getEmail(), member.getRole(), authorTasksCount, assigneeTasksCount);
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
}
