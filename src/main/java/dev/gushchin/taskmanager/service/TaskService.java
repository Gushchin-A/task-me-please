package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskCategory;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.repository.TaskRepository;
import dev.gushchin.taskmanager.view.TeamTasksStats;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;

    public List<Task> findByTeamId(Long teamId) {
        return taskRepository.findByTeamId(teamId).stream()
                .filter(Predicate.not(Task::isDeleted))
                .toList();
    }

    public Task create(
            Long teamId,
            UUID authorId,
            UUID assigneeId,
            String title,
            String description,
            LocalDate deadlineDate,
            TaskCategory category) {
        Instant now = Instant.now();

        Instant deadlineAt =
                deadlineDate == null ? null : deadlineDate.atStartOfDay().toInstant(ZoneOffset.UTC);

        Task task = new Task(
                null,
                teamId,
                authorId,
                assigneeId,
                title,
                description,
                deadlineAt,
                TaskStatus.OPEN,
                category,
                now,
                now,
                false,
                false);

        return taskRepository.save(task);
    }

    public TeamTasksStats getStats(List<Task> tasks) {
        long openCount = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.OPEN)
                .count();

        long inProgressCount = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS)
                .count();

        long doneCount = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();

        long notRelevantCount = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.NOT_RELEVANT)
                .count();

        return new TeamTasksStats(openCount, inProgressCount, doneCount, notRelevantCount);
    }

    public List<Task> filterByStatus(List<Task> tasks, TaskStatus status) {
        if (status == null) {
            return tasks;
        }

        return tasks.stream().filter(task -> task.getStatus() == status).toList();
    }
}
