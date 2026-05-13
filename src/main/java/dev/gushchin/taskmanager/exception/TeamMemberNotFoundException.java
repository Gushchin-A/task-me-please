package dev.gushchin.taskmanager.exception;

import java.util.UUID;

public class TeamMemberNotFoundException extends RuntimeException {
    public TeamMemberNotFoundException(Long teamId, UUID userId) {
        super("Team member not found. " + "Team id = + " + teamId + ", user id = " + userId);
    }
}
