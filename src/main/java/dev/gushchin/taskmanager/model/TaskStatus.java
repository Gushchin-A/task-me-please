package dev.gushchin.taskmanager.model;

public enum TaskStatus {
    OPEN("Открыто"),
    IN_PROGRESS("В работе"),
    DONE("Готово"),
    NOT_RELEVANT("Неактуально");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
