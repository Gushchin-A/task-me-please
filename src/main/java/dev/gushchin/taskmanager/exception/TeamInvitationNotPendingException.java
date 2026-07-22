package dev.gushchin.taskmanager.exception;

public class TeamInvitationNotPendingException extends RuntimeException {
    public TeamInvitationNotPendingException(Long id) {
        super("Team invitation is not pending: " + id);
    }
}
