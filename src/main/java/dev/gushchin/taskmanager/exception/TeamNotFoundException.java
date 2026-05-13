package dev.gushchin.taskmanager.exception;

public class TeamNotFoundException extends RuntimeException {
    public TeamNotFoundException(Long id) {
        super("Team not found + " + id);
    }
}
