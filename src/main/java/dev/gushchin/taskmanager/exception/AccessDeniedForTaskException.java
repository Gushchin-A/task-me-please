package dev.gushchin.taskmanager.exception;

public class AccessDeniedForTaskException extends RuntimeException {
    public AccessDeniedForTaskException() {
        super("Not enough permission to edit");
    }
}
