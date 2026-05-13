package dev.gushchin.taskmanager.exception;

import java.util.UUID;

public class TeamMemberAlreadyExistsException extends RuntimeException {
    public TeamMemberAlreadyExistsException(Long teamId, UUID userId) {
        super("Team member already exists. " + "Team id = + " + teamId + ", user id = " + userId);
    }
}
