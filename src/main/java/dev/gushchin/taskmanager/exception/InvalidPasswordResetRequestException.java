package dev.gushchin.taskmanager.exception;

public class InvalidPasswordResetRequestException extends RuntimeException {
    public InvalidPasswordResetRequestException(String message) {
        super(message);
    }
}
