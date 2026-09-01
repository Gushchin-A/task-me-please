package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.exception.TeamInvitationNotFoundException;
import dev.gushchin.taskmanager.exception.TeamInvitationNotPendingException;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.security.SafeRedirectAuthenticationSuccessHandler;
import dev.gushchin.taskmanager.service.TeamInvitationService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class InvitationPageController {
    private static final String INVITATIONS_PATH_PREFIX = "/invitations/";
    private static final String INVITATION_INVALID_VIEW = "invitations/invalid";
    private static final String REDIRECT_PREFIX = "redirect:";
    private static final String REDIRECT_TASKS = "redirect:/tasks";
    private static final String REDIRECT_TEAMS_PREFIX = "redirect:/teams/";
    private static final String SUCCESS_MESSAGE_ATTRIBUTE = "successMessage";

    private final TeamInvitationService teamInvitationService;
    private final TeamMemberService teamMemberService;
    private final TeamService teamService;

    @GetMapping("/invitations/{token}")
    public String showInvitation(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String token,
            Model model,
            RedirectAttributes redirectAttributes) {
        TeamInvitation invitation = findPendingInvitation(token);
        if (invitation == null) {
            return INVITATION_INVALID_VIEW;
        }

        if (authUser == null) {
            return REDIRECT_PREFIX + buildLoginUrl(token);
        }

        if (teamMemberService.isActiveMember(invitation.getTeamId(), authUser.getId())) {
            return REDIRECT_TEAMS_PREFIX + invitation.getTeamId();
        }

        Object successMessage = model.asMap().get(SUCCESS_MESSAGE_ATTRIBUTE);
        if (successMessage instanceof String message) {
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTRIBUTE, message);
        }

        return REDIRECT_PREFIX
                + UriComponentsBuilder.fromPath("/tasks")
                        .queryParam("invitation", token)
                        .build()
                        .encode()
                        .toUriString();
    }

    @PostMapping("/invitations/{token}/accept")
    public String acceptInvitation(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String token,
            RedirectAttributes redirectAttributes) {
        TeamInvitation invitation = findPendingInvitation(token);
        if (invitation == null) {
            return REDIRECT_PREFIX + INVITATIONS_PATH_PREFIX + token;
        }

        if (teamMemberService.isActiveMember(invitation.getTeamId(), authUser.getId())) {
            return REDIRECT_TEAMS_PREFIX + invitation.getTeamId();
        }

        teamInvitationService.accept(token, authUser.getId());
        Team team = teamService.findById(invitation.getTeamId());
        redirectAttributes.addFlashAttribute(
                SUCCESS_MESSAGE_ATTRIBUTE,
                "Приглашение принято. Теперь вы состоите в команде «" + team.getName() + "»");

        return REDIRECT_TEAMS_PREFIX + invitation.getTeamId();
    }

    @PostMapping("/invitations/{token}/decline")
    public String declineInvitation(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String token,
            @RequestParam(required = false) String returnTo) {
        TeamInvitation invitation = findPendingInvitation(token);
        if (invitation == null) {
            return REDIRECT_PREFIX + INVITATIONS_PATH_PREFIX + token;
        }

        if (teamMemberService.isActiveMember(invitation.getTeamId(), authUser.getId())) {
            return REDIRECT_TEAMS_PREFIX + invitation.getTeamId();
        }

        teamInvitationService.decline(token, authUser.getId());

        return "notifications".equals(returnTo) ? "redirect:/notifications" : REDIRECT_TASKS;
    }

    private TeamInvitation findPendingInvitation(String token) {
        try {
            return teamInvitationService.findPendingByToken(token);
        } catch (TeamInvitationNotFoundException ex) {
            return null;
        } catch (TeamInvitationNotPendingException ex) {
            return null;
        }
    }

    private String buildLoginUrl(String token) {
        String redirect = INVITATIONS_PATH_PREFIX + token;

        return UriComponentsBuilder.fromPath("/login")
                .queryParam(SafeRedirectAuthenticationSuccessHandler.REDIRECT_PARAMETER, redirect)
                .queryParam("invite", token)
                .build()
                .encode()
                .toUriString();
    }
}
