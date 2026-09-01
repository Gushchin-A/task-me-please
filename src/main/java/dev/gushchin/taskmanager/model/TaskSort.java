package dev.gushchin.taskmanager.model;

public enum TaskSort {
    NEWEST("Сначала новые"),
    OLDEST("Сначала старые"),
    DEADLINE_ASC("Ближайший дедлайн"),
    DEADLINE_DESC("Поздний дедлайн");

    private final String displayName;

    TaskSort(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
