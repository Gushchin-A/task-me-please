package dev.gushchin.taskmanager.exception;

public class PageNotFoundException extends RuntimeException {
    public PageNotFoundException() {}

    public PageNotFoundException(Throwable cause) {
        super(cause);
    }
}
