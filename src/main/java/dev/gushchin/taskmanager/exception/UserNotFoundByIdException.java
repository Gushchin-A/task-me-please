package dev.gushchin.taskmanager.exception;

import java.util.UUID;

public class UserNotFoundByIdException extends RuntimeException {
    public UserNotFoundByIdException(UUID id) {
        super("User not found " + id);
    }
}
