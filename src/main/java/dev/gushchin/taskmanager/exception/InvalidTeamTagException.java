package dev.gushchin.taskmanager.exception;

public class InvalidTeamTagException extends RuntimeException {
    public InvalidTeamTagException(String message) {
        super(message);
    }
}
