package dev.gushchin.taskmanager.exception;

public class TeamTagAlreadyExistsException extends RuntimeException {
    public TeamTagAlreadyExistsException(Long teamId, String name) {
        super("Team tag already exists. Team id = " + teamId + ", name = " + name);
    }
}
