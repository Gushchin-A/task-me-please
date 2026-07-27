package dev.gushchin.taskmanager.exception;

public class TeamTagNotFoundException extends RuntimeException {
    public TeamTagNotFoundException(Long id) {
        super("Team tag not found " + id);
    }
}
