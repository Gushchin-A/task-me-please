package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.exception.AccessDeniedForTaskException;
import dev.gushchin.taskmanager.exception.InvalidTeamNameException;
import dev.gushchin.taskmanager.exception.InvalidTeamTagException;
import dev.gushchin.taskmanager.exception.PageNotFoundException;
import dev.gushchin.taskmanager.exception.TeamInvitationAlreadyPendingException;
import dev.gushchin.taskmanager.exception.TeamInvitationNotFoundException;
import dev.gushchin.taskmanager.exception.TeamInvitationNotPendingException;
import dev.gushchin.taskmanager.exception.TeamMemberAlreadyExistsException;
import dev.gushchin.taskmanager.exception.TeamMemberNotFoundException;
import dev.gushchin.taskmanager.exception.TeamNotFoundException;
import dev.gushchin.taskmanager.exception.TeamTagAlreadyExistsException;
import dev.gushchin.taskmanager.exception.TeamTagNotFoundException;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskListMode;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamMemberRole;
import dev.gushchin.taskmanager.model.TeamTag;
import dev.gushchin.taskmanager.model.TeamTaskVisibility;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.TaskService;
import dev.gushchin.taskmanager.service.TeamInvitationService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.TeamTagService;
import dev.gushchin.taskmanager.service.UserService;
import dev.gushchin.taskmanager.view.TaskParticipantView;
import dev.gushchin.taskmanager.view.TaskView;
import dev.gushchin.taskmanager.view.TeamInvitationView;
import dev.gushchin.taskmanager.view.TeamListItemView;
import dev.gushchin.taskmanager.view.TeamMemberView;
import dev.gushchin.taskmanager.view.TeamPageView;
import dev.gushchin.taskmanager.view.TeamTasksStats;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class TeamPageController {
    private static final String CAN_INVITE_ATTRIBUTE = "canInvite";
    private static final String CSRF_ATTRIBUTE = "_csrf";
    private static final String ERROR_MESSAGE_ATTRIBUTE = "errorMessage";
    private static final int INVITATION_EMAIL_MAX_LENGTH = 255;
    private static final String INVITATION_EMAIL_FAILED_MESSAGE =
            "Не удалось отправить письмо. Скопируйте ссылку вручную из таблицы приглашений";
    private static final Pattern INVITATION_EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final String INVITE_PATH_SUFFIX = "/invite";
    private static final String DELETE_TEAM_CONFIRMATION_TEXT = "я хочу удалить команду";
    private static final String DELETE_TEAM_ERROR_MESSAGE = "Не удалось удалить команду";
    private static final String DELETE_TEAM_INVALID_CONFIRMATION_MESSAGE = "Проверочный текст введен неверно";
    private static final String DELETE_TEAM_SUCCESS_MESSAGE = "Команда успешно удалена";
    private static final String LEAVE_TEAM_CONFIRMATION_TEXT = "да хочу выйти из команды";
    private static final String LEAVE_TEAM_ERROR_MESSAGE = "Не удалось покинуть команду";
    private static final String LEAVE_TEAM_INVALID_CONFIRMATION_MESSAGE = DELETE_TEAM_INVALID_CONFIRMATION_MESSAGE;
    private static final String MEMBERS_PATH_SUFFIX = "/members";
    private static final String SETTINGS_SECTION_QUERY = "?section=";
    private static final String NAVIGATION_TEAMS_ATTRIBUTE = "navigationTeams";
    private static final String OWNER_INVITE_REQUIRED_MESSAGE_PREFIX =
            "Только owner команды может приглашать новых участников. ";
    private static final String OWNER_INVITE_REQUIRED_MESSAGE_SUFFIX = "Вы можете пока только просматривать команду";
    private static final String OWNER_INVITE_REQUIRED_MESSAGE =
            OWNER_INVITE_REQUIRED_MESSAGE_PREFIX + OWNER_INVITE_REQUIRED_MESSAGE_SUFFIX;
    private static final String OWNER_REMOVE_REQUIRED_MESSAGE = "Только owner команды может удалять участников";
    private static final String PENDING_INVITATION_CANCEL_SUCCESS_MESSAGE = "Приглашение успешно отменено";
    private static final String PENDING_INVITATION_EXISTS_MESSAGE = "Приглашение на этот email уже отправлено";
    private static final String PENDING_INVITATION_REQUIRED_MESSAGE = "Отменить можно только ожидающее приглашение";
    private static final String RESEND_INVITATION_SUCCESS_MESSAGE =
            "Приглашение отправлено повторно. Прошлая ссылка больше недействительна";
    private static final String REDIRECT_TEAMS_PREFIX = "redirect:/teams/";
    private static final String REDIRECT_TEAMS = "redirect:/teams";
    private static final String REMOVE_MEMBER_ERROR_MESSAGE = "Участника не удалось удалить";
    private static final String REMOVE_MEMBER_SUCCESS_MESSAGE = "Участник удален из команды";
    private static final String SUCCESS_MESSAGE_ATTRIBUTE = "successMessage";
    private static final String TEAM_ATTRIBUTE = "team";
    private static final String PAGE_ATTRIBUTE = "page";
    private static final String TEAMS_SHOW_VIEW = "teams/show";
    private static final String TEAM_SETTINGS_PATH_SUFFIX = "/settings";
    private static final String TEAM_SETTINGS_SECTION_DELETE = "delete";
    private static final String TEAM_SETTINGS_SECTION_LEAVE = "leave";
    private static final String TEAM_SETTINGS_SECTION_ATTRIBUTE = "settingsSection";
    private static final String TEAM_SETTINGS_SECTION_GENERAL = "general";
    private static final String TEAM_SETTINGS_VIEW = "teams/settings";
    private static final String TEAM_TAG_EXISTS_MESSAGE = "Такой тег уже существует";
    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";

    private final TeamService teamService;
    private final TaskService taskService;
    private final UserService userService;
    private final TeamInvitationService teamInvitationService;
    private final TeamMemberService teamMemberService;
    private final TeamTagService teamTagService;

    @GetMapping("/teams")
    public String teamsPage(@AuthenticationPrincipal AuthUser authUser, Model model, CsrfToken csrfToken) {
        List<Team> teams = teamService.findByUserId(authUser.getId());
        List<Task> tasks =
                taskService.findByTeamIds(teams.stream().map(Team::getId).toList());
        List<TeamListItemView> teamItems = teams.stream()
                .map(team -> toTeamListItem(team, tasks, authUser.getId()))
                .toList();

        model.addAttribute("teams", teamItems);
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);
        addEmptyFlashAttributes(model);

        return "teams/index";
    }

    private TeamListItemView toTeamListItem(Team team, List<Task> tasks, UUID currentUserId) {
        boolean owner = team.getCreatedBy().equals(currentUserId);
        long taskCount = tasks.stream()
                .filter(task -> task.getTeamId().equals(team.getId()))
                .filter(task -> owner
                        || (!task.isArchived()
                                && (task.getAuthorId().equals(currentUserId)
                                        || task.getAssigneeId().equals(currentUserId))))
                .count();

        return new TeamListItemView(team, owner, Math.toIntExact(taskCount));
    }

    @GetMapping("/teams/new")
    public String newTeamPage(Model model, CsrfToken csrfToken) {
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);
        addEmptyFlashAttributes(model);

        return "teams/new";
    }

    @GetMapping("/teams/{id}")
    public String showTeam(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            TeamTaskFilterRequest request,
            Model model,
            CsrfToken csrfToken) {
        TeamMember currentMember = findTeamMemberOrNotFound(id, authUser.getId());

        TaskStatus status = request.getStatus();
        TaskSort sort = request.getSort();
        List<UUID> authorIds = request.getAuthorIds();
        List<UUID> assigneeIds = request.getAssigneeIds();
        List<Long> tagIds = request.getTagIds();

        Team team = teamService.findById(id);
        List<Task> visibleTasks = taskService.findVisibleByTeamId(id, authUser.getId());
        List<Task> activeTasks = taskService.filterByArchived(visibleTasks, false);
        List<Task> archivedTasks = taskService.filterByArchived(visibleTasks, true);
        List<Task> modeFilteredTasks = activeTasks;
        List<TeamTag> tags = teamTagService.findByTeamId(id);
        List<TeamMember> teamMembers = teamMemberService.findByTeamId(id);

        List<TaskParticipantView> members = toTaskParticipants(id, teamMembers);

        TeamTasksStats stats = taskService.getStats(modeFilteredTasks);
        List<Task> statusFilteredTasks = taskService.filterByStatus(modeFilteredTasks, status);
        List<Task> tagFilteredTasks = taskService.filterByTagIds(statusFilteredTasks, tagIds);
        List<Task> authorFilteredTasks = taskService.filterByAuthorIds(tagFilteredTasks, authorIds);
        List<Task> assigneeFilteredTasks = taskService.filterByAssigneeIds(authorFilteredTasks, assigneeIds);
        List<Task> sortedTasks = taskService.sortTasks(assigneeFilteredTasks, sort);

        List<TaskView> taskViews = sortedTasks.stream()
                .map(task -> toTaskView(task, authUser.getId()))
                .toList();

        boolean canInvite = currentMember.getRole() == TeamMemberRole.OWNER;

        TeamPageView page = new TeamPageView(
                team,
                taskViews,
                new TeamPageView.TeamPageResources(
                        members, getFilterParticipants(id, modeFilteredTasks, members), tags),
                new TeamPageView.TeamPageCounts(
                        activeTasks.size(), archivedTasks.size(), sortedTasks.size(), teamMembers.size()),
                stats,
                new TeamPageView.TeamPageFilters(status, sort, authorIds, assigneeIds, tagIds, request.getOpenFilter()),
                new TeamPageView.TeamPageAccess(
                        canInvite, currentMember.getTaskVisibility() == TeamTaskVisibility.OWN_TASKS),
                TaskListMode.ACTIVE);

        model.addAttribute(PAGE_ATTRIBUTE, page);
        model.addAttribute(NAVIGATION_TEAMS_ATTRIBUTE, teamService.findByUserId(authUser.getId()));
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
            return notFound();
        }

        TaskStatus status = request.getStatus();
        TaskSort sort = request.getSort();
        List<UUID> authorIds = request.getAuthorIds();
        List<UUID> assigneeIds = request.getAssigneeIds();
        List<Long> tagIds = request.getTagIds();

        Team team = teamService.findById(id);
        List<Task> visibleTasks = taskService.findVisibleByTeamId(id, authUser.getId());
        List<Task> activeTasks = taskService.filterByArchived(visibleTasks, false);
        List<Task> archivedTasks = taskService.filterByArchived(visibleTasks, true);
        List<Task> modeFilteredTasks = archivedTasks;
        List<TeamTag> tags = teamTagService.findByTeamId(id);
        List<TeamMember> teamMembers = teamMemberService.findByTeamId(id);

        List<TaskParticipantView> members = toTaskParticipants(id, teamMembers);

        TeamTasksStats stats = taskService.getStats(modeFilteredTasks);
        List<Task> statusFilteredTasks = taskService.filterByStatus(modeFilteredTasks, status);
        List<Task> tagFilteredTasks = taskService.filterByTagIds(statusFilteredTasks, tagIds);
        List<Task> authorFilteredTasks = taskService.filterByAuthorIds(tagFilteredTasks, authorIds);
        List<Task> assigneeFilteredTasks = taskService.filterByAssigneeIds(authorFilteredTasks, assigneeIds);
        List<Task> sortedTasks = taskService.sortTasks(assigneeFilteredTasks, sort);

        List<TaskView> taskViews = sortedTasks.stream()
                .map(task -> toTaskView(task, authUser.getId()))
                .toList();

        boolean canInvite = currentMember.getRole() == TeamMemberRole.OWNER;

        TeamPageView page = new TeamPageView(
                team,
                taskViews,
                new TeamPageView.TeamPageResources(
                        members, getFilterParticipants(id, modeFilteredTasks, members), tags),
                new TeamPageView.TeamPageCounts(
                        activeTasks.size(), archivedTasks.size(), sortedTasks.size(), teamMembers.size()),
                stats,
                new TeamPageView.TeamPageFilters(status, sort, authorIds, assigneeIds, tagIds, request.getOpenFilter()),
                new TeamPageView.TeamPageAccess(
                        canInvite, currentMember.getTaskVisibility() == TeamTaskVisibility.OWN_TASKS),
                TaskListMode.ARCHIVE);

        model.addAttribute(PAGE_ATTRIBUTE, page);
        model.addAttribute(NAVIGATION_TEAMS_ATTRIBUTE, teamService.findByUserId(authUser.getId()));
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);

        return TEAMS_SHOW_VIEW;
    }

    @GetMapping("/teams/{id}/members")
    public String showTeamMembers(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long id, Model model, CsrfToken csrfToken) {
        TeamMember currentMember = findTeamMemberOrNotFound(id, authUser.getId());

        Team team = teamService.findById(id);
        List<Task> tasks = taskService.findVisibleByTeamId(id, authUser.getId());
        List<TeamMember> members = teamMemberService.findByTeamId(id);

        boolean canManageVisibility = currentMember.getRole() == TeamMemberRole.OWNER;
        List<TeamMemberView> memberViews = members.stream()
                .sorted(Comparator.comparing((TeamMember member) -> member.getRole() != TeamMemberRole.OWNER))
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
        model.addAttribute(
                "totalTasksCount", taskService.filterByArchived(tasks, false).size());
        model.addAttribute(
                "archiveTasksCount", taskService.filterByArchived(tasks, true).size());
        model.addAttribute(NAVIGATION_TEAMS_ATTRIBUTE, teamService.findByUserId(authUser.getId()));
        model.addAttribute("membersCount", members.size());
        model.addAttribute("members", memberViews);
        model.addAttribute("onlyCurrentOwnerInTeam", onlyCurrentOwnerInTeam);
        model.addAttribute(CAN_INVITE_ATTRIBUTE, canInvite);
        model.addAttribute("currentUserId", authUser.getId());
        model.addAttribute("canManageVisibility", canManageVisibility);
        model.addAttribute("taskVisibilities", TeamTaskVisibility.values());
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);
        addEmptyFlashAttributes(model);

        return "teams/members";
    }

    @GetMapping("/teams/{id}/invite")
    public String inviteMemberPage(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable Long id, Model model, CsrfToken csrfToken) {
        TeamMember currentMember;

        try {
            currentMember = teamMemberService.findById(id, authUser.getId());
        } catch (TeamMemberNotFoundException ex) {
            return notFound();
        }

        Team team = teamService.findById(id);
        boolean canInvite = currentMember.getRole() == TeamMemberRole.OWNER;

        model.addAttribute(TEAM_ATTRIBUTE, team);
        model.addAttribute(NAVIGATION_TEAMS_ATTRIBUTE, teamService.findByUserId(authUser.getId()));
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);
        model.addAttribute(CAN_INVITE_ATTRIBUTE, canInvite);
        model.addAttribute("invitationExpirationDays", TeamInvitationService.EXPIRATION_DAYS);
        model.addAttribute(
                "invitations",
                canInvite
                        ? teamInvitationService.findByTeamId(id, authUser.getId()).stream()
                                .map(TeamInvitationView::from)
                                .toList()
                        : List.of());

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

    @GetMapping("/teams/{id}/settings")
    public String teamSettings(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam(defaultValue = TEAM_SETTINGS_SECTION_GENERAL) String section,
            Model model,
            CsrfToken csrfToken) {
        TeamMember currentMember;

        try {
            currentMember = teamMemberService.findById(id, authUser.getId());
        } catch (TeamMemberNotFoundException ex) {
            return notFound();
        }

        boolean owner = currentMember.getRole() == TeamMemberRole.OWNER;

        if (!owner && !TEAM_SETTINGS_SECTION_LEAVE.equals(section)) {
            return REDIRECT_TEAMS_PREFIX + id + MEMBERS_PATH_SUFFIX;
        }

        String settingsSection;

        if (owner) {
            settingsSection = TEAM_SETTINGS_SECTION_DELETE.equals(section)
                    ? TEAM_SETTINGS_SECTION_DELETE
                    : TEAM_SETTINGS_SECTION_GENERAL;
        } else {
            settingsSection = TEAM_SETTINGS_SECTION_LEAVE;
        }

        model.addAttribute(TEAM_ATTRIBUTE, teamService.findById(id));
        model.addAttribute(NAVIGATION_TEAMS_ATTRIBUTE, teamService.findByUserId(authUser.getId()));
        model.addAttribute("tags", teamTagService.findByTeamId(id));
        model.addAttribute("usedTagIds", teamTagService.findUsedIdsByTeamId(id));
        model.addAttribute(TEAM_SETTINGS_SECTION_ATTRIBUTE, settingsSection);
        model.addAttribute("owner", owner);
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);
        addEmptyFlashAttributes(model);

        return TEAM_SETTINGS_VIEW;
    }

    @PostMapping("/teams/{id}/settings/name")
    public String renameTeam(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam String name,
            RedirectAttributes redirectAttributes) {
        if (!isTeamOwner(id, authUser.getId())) {
            return notFound();
        }

        try {
            teamService.rename(id, name, authUser.getId());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, "Название успешно изменено");
        } catch (InvalidTeamNameException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, getTeamNameErrorMessage(ex));
        }

        return REDIRECT_TEAMS_PREFIX + id + TEAM_SETTINGS_PATH_SUFFIX;
    }

    @PostMapping("/teams/{id}/settings/tags")
    public String addTeamTag(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam String name,
            RedirectAttributes redirectAttributes) {
        if (!isTeamOwner(id, authUser.getId())) {
            return notFound();
        }

        try {
            teamTagService.create(id, name, authUser.getId());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, "Тег успешно добавлен");
        } catch (TeamTagAlreadyExistsException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, TEAM_TAG_EXISTS_MESSAGE);
        } catch (InvalidTeamTagException ex) {
            redirectAttributes.addFlashAttribute(
                    ERROR_MESSAGE_ATTRIBUTE, "Не удалось добавить тег. Проверьте название");
        }

        return REDIRECT_TEAMS_PREFIX + id + TEAM_SETTINGS_PATH_SUFFIX;
    }

    @PostMapping("/teams/{teamId}/settings/tags/{tagId}/rename")
    public String renameTeamTag(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long teamId,
            @PathVariable Long tagId,
            @RequestParam String name,
            RedirectAttributes redirectAttributes) {
        if (!isTeamOwner(teamId, authUser.getId())) {
            return notFound();
        }

        try {
            teamTagService.rename(tagId, teamId, name, authUser.getId());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, "Тег успешно изменен");
        } catch (TeamTagAlreadyExistsException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, TEAM_TAG_EXISTS_MESSAGE);
        } catch (InvalidTeamTagException | TeamTagNotFoundException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, "Не удалось изменить тег");
        }

        return REDIRECT_TEAMS_PREFIX + teamId + TEAM_SETTINGS_PATH_SUFFIX;
    }

    @PostMapping("/teams/{teamId}/settings/tags/{tagId}/delete")
    public String deleteTeamTag(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long teamId,
            @PathVariable Long tagId,
            RedirectAttributes redirectAttributes) {
        if (!isTeamOwner(teamId, authUser.getId())) {
            return notFound();
        }

        try {
            teamTagService.delete(tagId, teamId, authUser.getId());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, "Тег успешно удален");
        } catch (InvalidTeamTagException | TeamTagNotFoundException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, "Не удалось удалить тег");
        }

        return REDIRECT_TEAMS_PREFIX + teamId + TEAM_SETTINGS_PATH_SUFFIX;
    }

    @PostMapping("/teams/{id}/delete")
    public String confirmDeleteTeam(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String confirmationText,
            RedirectAttributes redirectAttributes) {
        if (!isTeamOwner(id, authUser.getId())) {
            return notFound();
        }

        if (!DELETE_TEAM_CONFIRMATION_TEXT.equals(confirmationText)) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, DELETE_TEAM_INVALID_CONFIRMATION_MESSAGE);
            return redirectToDeleteTeamSettings(id);
        }

        try {
            teamService.delete(id, authUser.getId());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, DELETE_TEAM_SUCCESS_MESSAGE);
            return REDIRECT_TEAMS;
        } catch (AccessDeniedForTaskException | TeamNotFoundException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, DELETE_TEAM_ERROR_MESSAGE);
            return redirectToDeleteTeamSettings(id);
        }
    }

    @PostMapping("/teams/{id}/leave")
    public String leaveTeam(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String confirmationText,
            RedirectAttributes redirectAttributes) {
        TeamMember currentMember = findTeamMemberOrNotFound(id, authUser.getId());

        if (currentMember.getRole() == TeamMemberRole.OWNER) {
            return notFound();
        }

        if (!LEAVE_TEAM_CONFIRMATION_TEXT.equals(confirmationText)) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, LEAVE_TEAM_INVALID_CONFIRMATION_MESSAGE);
            return redirectToLeaveTeamSettings(id);
        }

        try {
            Team team = teamService.findById(id);
            teamMemberService.leaveTeam(id, authUser.getId());
            redirectAttributes.addFlashAttribute(
                    SUCCESS_MESSAGE_ATTRIBUTE, "Вы покинули команду «" + team.getName() + "»");
            return REDIRECT_TEAMS;
        } catch (AccessDeniedForTaskException | TeamMemberNotFoundException | TeamNotFoundException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, LEAVE_TEAM_ERROR_MESSAGE);
            return redirectToLeaveTeamSettings(id);
        }
    }

    @PostMapping("/teams/{teamId}/members/{userId}/visibility")
    public Object updateMemberTaskVisibility(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long teamId,
            @PathVariable UUID userId,
            @RequestParam TeamTaskVisibility taskVisibility,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            RedirectAttributes redirectAttributes) {
        boolean asynchronousRequest = XML_HTTP_REQUEST.equals(requestedWith);

        try {
            teamMemberService.updateTaskVisibility(teamId, userId, taskVisibility, authUser.getId());
            if (asynchronousRequest) {
                return ResponseEntity.noContent().build();
            }
        } catch (AccessDeniedForTaskException ex) {
            redirectAttributes.addFlashAttribute(
                    ERROR_MESSAGE_ATTRIBUTE, "Только owner команды может изменять видимость задач");
            if (asynchronousRequest) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (TeamMemberNotFoundException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, "Не удалось изменить видимость задач");
            if (asynchronousRequest) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        }

        return REDIRECT_TEAMS_PREFIX + teamId + MEMBERS_PATH_SUFFIX;
    }

    @PostMapping("/teams/{teamId}/members/{userId}/remove")
    public String removeMember(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long teamId,
            @PathVariable UUID userId,
            RedirectAttributes redirectAttributes) {
        try {
            teamMemberService.removeMember(teamId, userId, authUser.getId());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, REMOVE_MEMBER_SUCCESS_MESSAGE);
        } catch (AccessDeniedForTaskException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, OWNER_REMOVE_REQUIRED_MESSAGE);
        } catch (TeamMemberNotFoundException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, REMOVE_MEMBER_ERROR_MESSAGE);
        }

        return REDIRECT_TEAMS_PREFIX + teamId + MEMBERS_PATH_SUFFIX;
    }

    @PostMapping("/teams/{teamId}/invitations/{invitationId}/cancel")
    public String cancelInvitation(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long teamId,
            @PathVariable Long invitationId,
            RedirectAttributes redirectAttributes) {
        try {
            teamInvitationService.cancel(invitationId, teamId, authUser.getId());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, PENDING_INVITATION_CANCEL_SUCCESS_MESSAGE);
        } catch (AccessDeniedForTaskException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, OWNER_INVITE_REQUIRED_MESSAGE);
        } catch (TeamInvitationNotFoundException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, PENDING_INVITATION_REQUIRED_MESSAGE);
        } catch (TeamInvitationNotPendingException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, PENDING_INVITATION_REQUIRED_MESSAGE);
        }

        return REDIRECT_TEAMS_PREFIX + teamId + INVITE_PATH_SUFFIX;
    }

    @PostMapping("/teams/{teamId}/invitations/{invitationId}/resend")
    public String resendInvitation(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long teamId,
            @PathVariable Long invitationId,
            RedirectAttributes redirectAttributes) {
        try {
            if (teamInvitationService.resend(invitationId, teamId, authUser.getId())) {
                redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, RESEND_INVITATION_SUCCESS_MESSAGE);
            } else {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, INVITATION_EMAIL_FAILED_MESSAGE);
            }
        } catch (AccessDeniedForTaskException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, OWNER_INVITE_REQUIRED_MESSAGE);
        } catch (TeamInvitationNotFoundException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, PENDING_INVITATION_REQUIRED_MESSAGE);
        } catch (TeamInvitationNotPendingException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, PENDING_INVITATION_REQUIRED_MESSAGE);
        }

        return REDIRECT_TEAMS_PREFIX + teamId + INVITE_PATH_SUFFIX;
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
            return notFound();
        }

        String redirect = REDIRECT_TEAMS_PREFIX + id + INVITE_PATH_SUFFIX;

        if (currentMember.getRole() != TeamMemberRole.OWNER) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, OWNER_INVITE_REQUIRED_MESSAGE);
        } else {
            createInvitation(id, email, authUser.getId(), redirectAttributes);
        }

        return redirect;
    }

    @PostMapping("/teams")
    public String createTeam(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam String name,
            @RequestParam(required = false) List<String> tags,
            RedirectAttributes redirectAttributes) {
        try {
            teamService.create(name, authUser.getId(), tags);
        } catch (InvalidTeamNameException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, getTeamNameErrorMessage(ex));

            return "redirect:/teams/new";
        }

        return REDIRECT_TEAMS;
    }

    private void createInvitation(
            Long teamId, String email, UUID currentUserId, RedirectAttributes redirectAttributes) {
        if (email.isBlank()
                || email.length() > INVITATION_EMAIL_MAX_LENGTH
                || !INVITATION_EMAIL_PATTERN.matcher(email).matches()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, "Введите корректный email");
            return;
        }

        try {
            if (teamInvitationService.createAndSend(teamId, email, currentUserId)) {
                redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, "Приглашение успешно создано");
            } else {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, INVITATION_EMAIL_FAILED_MESSAGE);
            }
        } catch (TeamMemberAlreadyExistsException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, "Пользователь уже состоит в команде");
        } catch (TeamInvitationAlreadyPendingException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, PENDING_INVITATION_EXISTS_MESSAGE);
        }
    }

    private void addEmptyFlashAttributes(Model model) {
        if (!model.containsAttribute(SUCCESS_MESSAGE_ATTRIBUTE)) {
            model.addAttribute(SUCCESS_MESSAGE_ATTRIBUTE, null);
        }

        if (!model.containsAttribute(ERROR_MESSAGE_ATTRIBUTE)) {
            model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, null);
        }
    }

    private String getTeamNameErrorMessage(InvalidTeamNameException exception) {
        return switch (exception.getReason()) {
            case BLANK -> "Введите название команды";
            case TOO_LONG -> "Название команды не должно быть длиннее 100 символов";
            case UNCHANGED -> "Название не изменилось. Введите новое название команды";
        };
    }

    private boolean isTeamOwner(Long teamId, UUID currentUserId) {
        try {
            return teamMemberService.findById(teamId, currentUserId).getRole() == TeamMemberRole.OWNER;
        } catch (TeamMemberNotFoundException | TeamNotFoundException ex) {
            return false;
        }
    }

    private TeamMember findTeamMemberOrNotFound(Long teamId, UUID userId) {
        try {
            return teamMemberService.findById(teamId, userId);
        } catch (TeamMemberNotFoundException ex) {
            throw new PageNotFoundException(ex);
        }
    }

    private String notFound() {
        throw new PageNotFoundException();
    }

    private String redirectToDeleteTeamSettings(Long teamId) {
        return REDIRECT_TEAMS_PREFIX
                + teamId
                + TEAM_SETTINGS_PATH_SUFFIX
                + SETTINGS_SECTION_QUERY
                + TEAM_SETTINGS_SECTION_DELETE;
    }

    private String redirectToLeaveTeamSettings(Long teamId) {
        return REDIRECT_TEAMS_PREFIX
                + teamId
                + TEAM_SETTINGS_PATH_SUFFIX
                + SETTINGS_SECTION_QUERY
                + TEAM_SETTINGS_SECTION_LEAVE;
    }

    private TaskView toTaskView(Task task, UUID userId) {
        TaskParticipantView author = toTaskParticipant(task.getTeamId(), task.getAuthorId());
        TaskParticipantView assignee = toTaskParticipant(task.getTeamId(), task.getAssigneeId());
        TeamTag tag = teamTagService.findById(task.getTagId());

        boolean canUpdateTask = taskService.canUpdateTask(task, userId);
        boolean canUpdateStatus = taskService.canUpdateStatus(task, userId);
        boolean canArchive = taskService.canArchiveTask(task, userId);
        boolean canRestore = taskService.canRestoreTask(task, userId);
        boolean showAuthorChangeWarning = task.getAuthorId().equals(userId) && !taskService.isTeamOwner(task, userId);

        TaskView.TaskState state = new TaskView.TaskState(
                task.isArchived(), canUpdateTask, canUpdateStatus, canArchive, canRestore, showAuthorChangeWarning);

        return TaskView.from(task, tag.getName(), author, assignee, state);
    }

    private List<TaskParticipantView> toTaskParticipants(Long teamId, List<TeamMember> teamMembers) {
        return teamMembers.stream()
                .map(teamMember -> toTaskParticipant(teamId, teamMember.getUserId()))
                .toList();
    }

    private TaskParticipantView toTaskParticipant(Long teamId, UUID userId) {
        User user = userService.findById(userId);

        return new TaskParticipantView(user.getId(), user.getName(), !teamMemberService.isActiveMember(teamId, userId));
    }

    private List<TaskParticipantView> getFilterParticipants(
            Long teamId, List<Task> tasks, List<TaskParticipantView> activeParticipants) {
        List<TaskParticipantView> filterParticipants = new ArrayList<>(activeParticipants);

        for (Task task : tasks) {
            addFilterParticipant(filterParticipants, toTaskParticipant(teamId, task.getAuthorId()));
            addFilterParticipant(filterParticipants, toTaskParticipant(teamId, task.getAssigneeId()));
        }

        return filterParticipants;
    }

    private void addFilterParticipant(List<TaskParticipantView> filterParticipants, TaskParticipantView participant) {
        boolean alreadyAdded = filterParticipants.stream()
                .anyMatch(existingParticipant -> existingParticipant.id().equals(participant.id()));

        if (!alreadyAdded) {
            filterParticipants.add(participant);
        }
    }
}
