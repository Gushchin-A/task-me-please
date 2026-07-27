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
import dev.gushchin.taskmanager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class InvitationPageController {
    private static final String CSRF_ATTRIBUTE = "_csrf";
    private static final String INVITATIONS_PATH_PREFIX = "/invitations/";
    private static final String INVITATION_INVALID_VIEW = "invitations/invalid";
    private static final String INVITATION_SHOW_VIEW = "invitations/show";
    private static final String REDIRECT_PREFIX = "redirect:";
    private static final String REDIRECT_TEAMS = "redirect:/teams";
    private static final String REDIRECT_TEAMS_PREFIX = "redirect:/teams/";

    private final TeamInvitationService teamInvitationService;
    private final TeamMemberService teamMemberService;
    private final TeamService teamService;
    private final UserService userService;

    @GetMapping("/invitations/{token}")
    public String showInvitation(
            @AuthenticationPrincipal AuthUser authUser, @PathVariable String token, Model model, CsrfToken csrfToken) {
        TeamInvitation invitation = findPendingInvitation(token);
        if (invitation == null) {
            return showInvalidInvitation(authUser, model);
        }

        if (authUser == null) {
            return REDIRECT_PREFIX + buildLoginUrl(token);
        }

        if (teamMemberService.isActiveMember(invitation.getTeamId(), authUser.getId())) {
            return REDIRECT_TEAMS_PREFIX + invitation.getTeamId();
        }

        Team team = teamService.findById(invitation.getTeamId());
        String invitedByEmail = userService.findById(invitation.getInvitedBy()).getEmail();

        model.addAttribute("invitation", invitation);
        model.addAttribute("team", team);
        model.addAttribute("invitedByEmail", invitedByEmail);
        model.addAttribute(CSRF_ATTRIBUTE, csrfToken);

        return INVITATION_SHOW_VIEW;
    }

    @PostMapping("/invitations/{token}/accept")
    public String acceptInvitation(@AuthenticationPrincipal AuthUser authUser, @PathVariable String token) {
        TeamInvitation invitation = findPendingInvitation(token);
        if (invitation == null) {
            return REDIRECT_PREFIX + INVITATIONS_PATH_PREFIX + token;
        }

        if (teamMemberService.isActiveMember(invitation.getTeamId(), authUser.getId())) {
            return REDIRECT_TEAMS_PREFIX + invitation.getTeamId();
        }

        teamInvitationService.accept(token, authUser.getId());

        return REDIRECT_TEAMS;
    }

    @PostMapping("/invitations/{token}/decline")
    public String declineInvitation(@AuthenticationPrincipal AuthUser authUser, @PathVariable String token) {
        TeamInvitation invitation = findPendingInvitation(token);
        if (invitation == null) {
            return REDIRECT_PREFIX + INVITATIONS_PATH_PREFIX + token;
        }

        if (teamMemberService.isActiveMember(invitation.getTeamId(), authUser.getId())) {
            return REDIRECT_TEAMS_PREFIX + invitation.getTeamId();
        }

        teamInvitationService.decline(token, authUser.getId());

        return REDIRECT_TEAMS;
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

    private String showInvalidInvitation(AuthUser authUser, Model model) {
        model.addAttribute("authenticated", authUser != null);

        return INVITATION_INVALID_VIEW;
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
