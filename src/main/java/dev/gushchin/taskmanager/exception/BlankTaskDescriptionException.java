package dev.gushchin.taskmanager.exception;

public class BlankTaskDescriptionException extends RuntimeException {
    public BlankTaskDescriptionException() {
        super("Task description must not be blank");
    }
}
