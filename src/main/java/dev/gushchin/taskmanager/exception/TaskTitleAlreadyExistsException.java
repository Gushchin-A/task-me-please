package dev.gushchin.taskmanager.exception;

public class TaskTitleAlreadyExistsException extends RuntimeException {
    public TaskTitleAlreadyExistsException(Long teamId, String title) {
        super("Task title already exists. Team id = " + teamId + ", title = " + title);
    }
}
