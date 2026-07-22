package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;

public record TeamInvitationView(Long id, String invitedEmail, TeamInvitationStatus status, String invitationUrl) {
    private static final String INVITATION_PATH_PREFIX = "/invitations/";

    public static TeamInvitationView from(TeamInvitation invitation) {
        String invitationUrl = null;

        if (invitation.getStatus() == TeamInvitationStatus.PENDING) {
            invitationUrl = INVITATION_PATH_PREFIX + invitation.getToken();
        }

        return new TeamInvitationView(
                invitation.getId(), invitation.getInvitedEmail(), invitation.getStatus(), invitationUrl);
    }
}
