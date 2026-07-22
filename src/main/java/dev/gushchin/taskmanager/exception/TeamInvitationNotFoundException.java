package dev.gushchin.taskmanager.exception;

public class TeamInvitationNotFoundException extends RuntimeException {
    public TeamInvitationNotFoundException(String token) {
        super("Team invitation not found with token " + token);
    }
}
