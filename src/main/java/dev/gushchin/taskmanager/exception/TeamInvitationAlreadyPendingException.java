package dev.gushchin.taskmanager.exception;

public class TeamInvitationAlreadyPendingException extends RuntimeException {
    public TeamInvitationAlreadyPendingException(Long teamId, String invitedEmail) {
        super("Team invitation already pending for team " + teamId + " and email " + invitedEmail);
    }
}
