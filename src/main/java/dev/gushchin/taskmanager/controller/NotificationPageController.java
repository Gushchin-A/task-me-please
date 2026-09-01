package dev.gushchin.taskmanager.controller;

import static org.springframework.http.HttpStatus.NO_CONTENT;

import dev.gushchin.taskmanager.exception.TeamInvitationNotFoundException;
import dev.gushchin.taskmanager.exception.TeamInvitationNotPendingException;
import dev.gushchin.taskmanager.model.NotificationSort;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.NotificationService;
import dev.gushchin.taskmanager.service.TeamInvitationService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.UserService;
import dev.gushchin.taskmanager.view.InvitationDecisionView;
import dev.gushchin.taskmanager.view.NotificationsPageView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class NotificationPageController {
    private static final String ERROR_MESSAGE_ATTRIBUTE = "errorMessage";
    private static final String PAGE_ATTRIBUTE = "page";
    private static final String REDIRECT_NOTIFICATIONS = "redirect:/notifications";

    private final NotificationService notificationService;
    private final TeamInvitationService teamInvitationService;
    private final TeamMemberService teamMemberService;
    private final TeamService teamService;
    private final UserService userService;

    @GetMapping("/notifications")
    public String page(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(required = false) NotificationSort sort,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) String invitation,
            Model model,
            CsrfToken csrfToken) {
        NotificationSort resolvedSort = sort == null ? NotificationSort.NEWEST : sort;
        NotificationsPageView page = new NotificationsPageView(
                notificationService.findPage(authUser.getId(), unread, resolvedSort, cursor),
                notificationService.getCounts(authUser.getId()),
                unread,
                resolvedSort,
                buildInvitationDecision(invitation, authUser.getId()));
        model.addAttribute(PAGE_ATTRIBUTE, page);
        model.addAttribute("_csrf", csrfToken);

        return "notifications/index";
    }

    @PostMapping("/notifications/read")
    public String markAsRead(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) List<Long> notificationIds,
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "NEWEST") NotificationSort sort,
            RedirectAttributes redirectAttributes) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    ERROR_MESSAGE_ATTRIBUTE, "Не удалось отметить уведомления как прочитанные. Попробуйте еще раз");
        } else {
            notificationService.markAsRead(authUser.getId(), notificationIds);
        }

        return buildNotificationsRedirect(unread, sort);
    }

    @PostMapping("/notifications/read-on-open")
    @ResponseStatus(NO_CONTENT)
    public void markAsReadOnOpen(@AuthenticationPrincipal AuthUser authUser, @RequestParam Long notificationId) {
        notificationService.markAsRead(authUser.getId(), List.of(notificationId));
    }

    private String buildNotificationsRedirect(boolean unread, NotificationSort sort) {
        if (unread && sort == NotificationSort.OLDEST) {
            return REDIRECT_NOTIFICATIONS + "?unread=true&sort=OLDEST";
        }
        if (unread) {
            return REDIRECT_NOTIFICATIONS + "?unread=true";
        }
        if (sort == NotificationSort.OLDEST) {
            return REDIRECT_NOTIFICATIONS + "?sort=OLDEST";
        }

        return REDIRECT_NOTIFICATIONS;
    }

    private InvitationDecisionView buildInvitationDecision(String token, java.util.UUID currentUserId) {
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
}
