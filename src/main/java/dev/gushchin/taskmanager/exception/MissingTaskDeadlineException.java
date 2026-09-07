package dev.gushchin.taskmanager.exception;

public class MissingTaskDeadlineException extends RuntimeException {
    public MissingTaskDeadlineException() {
        super("Task deadline must not be null");
    }
}
