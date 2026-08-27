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
import dev.gushchin.taskmanager.view.TeamMemberView;
import dev.gushchin.taskmanager.view.TeamPageView;
import dev.gushchin.taskmanager.view.TeamTasksStats;
import java.util.ArrayList;
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
    private static final String DELETE_TEAM_CONFIRMATION_TEXT = "я хочу удалить команду";
    private static final String DELETE_TEAM_ERROR_MESSAGE = "Не удалось удалить команду";
    private static final String DELETE_TEAM_INVALID_CONFIRMATION_MESSAGE = "Проверочный текст введен неверно";
    private static final String DELETE_TEAM_SUCCESS_MESSAGE = "Команда успешно удалена";
    private static final String MEMBERS_PATH_SUFFIX = "/members";
    private static final String NAVIGATION_TEAMS_ATTRIBUTE = "navigationTeams";
    private static final String OWNER_INVITE_REQUIRED_MESSAGE_PREFIX =
            "Только owner команды может приглашать новых участников. ";
    private static final String OWNER_INVITE_REQUIRED_MESSAGE_SUFFIX = "Вы можете пока только просматривать команду";
    private static final String OWNER_INVITE_REQUIRED_MESSAGE =
            OWNER_INVITE_REQUIRED_MESSAGE_PREFIX + OWNER_INVITE_REQUIRED_MESSAGE_SUFFIX;
    private static final String OWNER_REMOVE_REQUIRED_MESSAGE = "Только owner команды может удалять участников";
    private static final String PENDING_INVITATION_CANCEL_SUCCESS_MESSAGE = "Приглашение отменено";
    private static final String PENDING_INVITATION_EXISTS_MESSAGE = "Приглашение на этот email уже отправлено";
    private static final String PENDING_INVITATION_REQUIRED_MESSAGE = "Отменить можно только ожидающее приглашение";
    private static final String REDIRECT_TEAMS_PREFIX = "redirect:/teams/";
    private static final String REDIRECT_TEAMS = "redirect:/teams";
    private static final String REMOVE_MEMBER_ERROR_MESSAGE = "Участника не удалось удалить";
    private static final String REMOVE_MEMBER_SUCCESS_MESSAGE = "Участник удалён из команды";
    private static final String SUCCESS_MESSAGE_ATTRIBUTE = "successMessage";
    private static final String TEAM_ATTRIBUTE = "team";
    private static final String PAGE_ATTRIBUTE = "page";
    private static final String TEAMS_SHOW_VIEW = "teams/show";
    private static final String TEAM_SETTINGS_PATH_SUFFIX = "/settings";
    private static final String TEAM_SETTINGS_SECTION_DELETE = "delete";
    private static final String TEAM_SETTINGS_SECTION_ATTRIBUTE = "settingsSection";
    private static final String TEAM_SETTINGS_SECTION_GENERAL = "general";
    private static final String TEAM_SETTINGS_VIEW = "teams/settings";
    private static final String TEAM_TAG_EXISTS_MESSAGE = "Такой тег уже существует";

    private final TeamService teamService;
    private final TaskService taskService;
    private final UserService userService;
    private final TeamInvitationService teamInvitationService;
    private final TeamMemberService teamMemberService;
    private final TeamTagService teamTagService;

    @GetMapping("/teams")
    public String teamsPage(@AuthenticationPrincipal AuthUser authUser, Model model, CsrfToken csrfToken) {
        List<Team> teams = teamService.findByUserId(authUser.getId());

        model.addAttribute("teams", teams);
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);
        addEmptyFlashAttributes(model);

        return "teams/index";
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
        TeamMember currentMember;

        try {
            currentMember = teamMemberService.findById(id, authUser.getId());
        } catch (TeamMemberNotFoundException ex) {
            return notFound();
        }

        TaskStatus status = request.getStatus();
        TaskSort sort = request.getSort();
        UUID authorId = request.getAuthorId();
        UUID assigneeId = request.getAssigneeId();
        Long tagId = request.getTagId();

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
        List<Task> tagFilteredTasks = taskService.filterByTagId(statusFilteredTasks, tagId);
        List<Task> authorFilteredTasks = taskService.filterByAuthorId(tagFilteredTasks, authorId);
        List<Task> assigneeFilteredTasks = taskService.filterByAssigneeId(authorFilteredTasks, assigneeId);
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
                new TeamPageView.TeamPageFilters(status, sort, authorId, assigneeId, tagId),
                canInvite,
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
        UUID authorId = request.getAuthorId();
        UUID assigneeId = request.getAssigneeId();
        Long tagId = request.getTagId();

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
        List<Task> tagFilteredTasks = taskService.filterByTagId(statusFilteredTasks, tagId);
        List<Task> authorFilteredTasks = taskService.filterByAuthorId(tagFilteredTasks, authorId);
        List<Task> assigneeFilteredTasks = taskService.filterByAssigneeId(authorFilteredTasks, assigneeId);
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
                new TeamPageView.TeamPageFilters(status, sort, authorId, assigneeId, tagId),
                canInvite,
                TaskListMode.ARCHIVE);

        model.addAttribute(PAGE_ATTRIBUTE, page);
        model.addAttribute(NAVIGATION_TEAMS_ATTRIBUTE, teamService.findByUserId(authUser.getId()));
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
            return notFound();
        }

        Team team = teamService.findById(id);
        List<Task> tasks = taskService.findVisibleByTeamId(id, authUser.getId());
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
        model.addAttribute("canSeeMemberDetails", canSeeMemberDetails);
        model.addAttribute("canSeeMemberPrivateData", canSeeMemberPrivateData);
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
        if (!isTeamOwner(id, authUser.getId())) {
            return notFound();
        }

        String settingsSection = TEAM_SETTINGS_SECTION_DELETE.equals(section)
                ? TEAM_SETTINGS_SECTION_DELETE
                : TEAM_SETTINGS_SECTION_GENERAL;

        model.addAttribute(TEAM_ATTRIBUTE, teamService.findById(id));
        model.addAttribute(NAVIGATION_TEAMS_ATTRIBUTE, teamService.findByUserId(authUser.getId()));
        model.addAttribute("tags", teamTagService.findByTeamId(id));
        model.addAttribute("usedTagIds", teamTagService.findUsedIdsByTeamId(id));
        model.addAttribute(TEAM_SETTINGS_SECTION_ATTRIBUTE, settingsSection);
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
            teamTagService.create(id, name);
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
            teamTagService.rename(tagId, teamId, name);
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
            teamTagService.delete(tagId, teamId);
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

    @PostMapping("/teams/{teamId}/members/{userId}/visibility")
    public String updateMemberTaskVisibility(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long teamId,
            @PathVariable UUID userId,
            @RequestParam TeamTaskVisibility taskVisibility) {
        teamMemberService.updateTaskVisibility(teamId, userId, taskVisibility, authUser.getId());

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
            teamInvitationService.resend(invitationId, teamId, authUser.getId());
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
        try {
            teamInvitationService.createAndSend(teamId, email, currentUserId);
            redirectAttributes.addFlashAttribute(
                    SUCCESS_MESSAGE_ATTRIBUTE, "Приглашение создано. Ссылку-приглашение можно отправить лично");
        } catch (TeamMemberAlreadyExistsException ex) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTRIBUTE, "Пользователь уже состоит в этой команде");
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

    private String notFound() {
        throw new PageNotFoundException();
    }

    private String redirectToDeleteTeamSettings(Long teamId) {
        return REDIRECT_TEAMS_PREFIX + teamId + TEAM_SETTINGS_PATH_SUFFIX + "?section=" + TEAM_SETTINGS_SECTION_DELETE;
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
