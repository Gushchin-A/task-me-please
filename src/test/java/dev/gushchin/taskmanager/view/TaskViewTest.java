package dev.gushchin.taskmanager.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskViewTest {
    @Test
    void statusTextShouldUseRussianLabels() {
        assertEquals("Открыто", createTaskView(TaskStatus.OPEN, Instant.now()).statusText());
        assertEquals(
                "В работе",
                createTaskView(TaskStatus.IN_PROGRESS, Instant.now()).statusText());
        assertEquals("Готово", createTaskView(TaskStatus.DONE, Instant.now()).statusText());
        assertEquals(
                "Неактуально",
                createTaskView(TaskStatus.NOT_RELEVANT, Instant.now()).statusText());
    }

    @Test
    void updatedAtTextShouldUseRelativeRussianTime() {
        Instant now = Instant.now();

        assertEquals(
                "4 минуты назад",
                createTaskView(TaskStatus.OPEN, now.minus(Duration.ofMinutes(4)))
                        .updatedAtText());
        assertEquals(
                "3 часа назад",
                createTaskView(TaskStatus.OPEN, now.minus(Duration.ofHours(3))).updatedAtText());
        assertEquals(
                "вчера",
                createTaskView(TaskStatus.OPEN, now.minus(Duration.ofHours(25))).updatedAtText());
        assertEquals(
                "2 дня назад",
                createTaskView(TaskStatus.OPEN, now.minus(Duration.ofDays(2))).updatedAtText());
    }

    private static TaskView createTaskView(TaskStatus status, Instant updatedAt) {
        UUID authorId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        Task task = new Task(
                1L,
                1L,
                authorId,
                assigneeId,
                "Задача",
                "Описание",
                null,
                status,
                1L,
                updatedAt,
                updatedAt,
                false,
                false);

        return TaskView.from(task, "Тег", "Автор", "Исполнитель");
    }
}
