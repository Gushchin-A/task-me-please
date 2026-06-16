package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.TaskNotFoundException;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskCategory;
import dev.gushchin.taskmanager.model.TaskRoleFilter;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.repository.TaskRepository;
import dev.gushchin.taskmanager.view.TaskWithTeamView;
import dev.gushchin.taskmanager.view.TeamTasksStats;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
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

    public List<Task> findByTeamIds(List<Long> teamIds) {
        return teamIds.stream().flatMap(teamId -> findByTeamId(teamId).stream()).toList();
    }

    public Task findById(Long id) {
        Task task = taskRepository.findById(id);

        if (task == null || task.isDeleted()) {
            throw new TaskNotFoundException(id);
        }

        return task;
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

    public List<Task> filterByTeamId(List<Task> tasks, Long teamId) {
        if (teamId == null) {
            return tasks;
        }

        return tasks.stream().filter(task -> task.getTeamId().equals(teamId)).toList();
    }

    public List<Task> filterByRole(List<Task> tasks, TaskRoleFilter role, UUID userId) {
        List<Task> result = tasks;

        if (role == null) {
            result = tasks;
        } else if (role == TaskRoleFilter.AUTHOR) {
            result = tasks.stream()
                    .filter(task -> task.getAuthorId().equals(userId))
                    .toList();
        } else if (role == TaskRoleFilter.ASSIGNEE) {
            result = tasks.stream()
                    .filter(task -> task.getAssigneeId().equals(userId))
                    .toList();
        }

        return result;
    }

    public List<Task> sortTasks(List<Task> tasks, TaskSort sort) {
        if (sort == null) {
            return tasks.stream().sorted(getDefaultTaskOrder()).toList();
        }

        if (sort == TaskSort.DEADLINE) {
            return tasks.stream()
                    .sorted(Comparator.comparing(Task::getDeadlineAt, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(getDefaultTaskOrder()))
                    .toList();
        }

        if (sort == TaskSort.CATEGORY) {
            return tasks.stream()
                    .sorted(Comparator.comparing(
                                    (Task task) -> task.getCategory().name())
                            .thenComparing(getDefaultTaskOrder()))
                    .toList();
        }

        return tasks.stream().sorted(getDefaultTaskOrder()).toList();
    }

    public List<TaskWithTeamView> sortTaskCards(List<TaskWithTeamView> taskCards, TaskSort sort) {
        Comparator<TaskWithTeamView> order = getDefaultTaskCardOrder();

        if (sort == null) {
            order = getDefaultTaskCardOrder();
        } else if (sort == TaskSort.DEADLINE) {
            order = Comparator.comparing(
                            (TaskWithTeamView taskCard) -> taskCard.task().deadlineAt(),
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(getDefaultTaskCardOrder());
        } else if (sort == TaskSort.CATEGORY) {
            order = Comparator.comparing((TaskWithTeamView taskCard) ->
                            taskCard.task().category().name())
                    .thenComparing(getDefaultTaskCardOrder());
        } else if (sort == TaskSort.TEAM) {
            order = Comparator.comparing(TaskWithTeamView::teamName).thenComparing(getDefaultTaskCardOrder());
        }

        return taskCards.stream().sorted(order).toList();
    }

    private Comparator<Task> getDefaultTaskOrder() {
        return Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(getIdDescOrder());
    }

    private Comparator<Task> getIdDescOrder() {
        return Comparator.comparing(Task::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<TaskWithTeamView> getDefaultTaskCardOrder() {
        return Comparator.comparing(
                        (TaskWithTeamView taskCard) -> taskCard.task().createdAt(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(getTaskCardIdDescOrder());
    }

    private Comparator<TaskWithTeamView> getTaskCardIdDescOrder() {
        return Comparator.comparing(taskCard -> taskCard.task().id(), Comparator.nullsLast(Comparator.reverseOrder()));
    }

    public Task updateStatus(Long id, TaskStatus status) {
        Task task = findById(id);

        return taskRepository.updateStatus(task.getId(), status, Instant.now());
    }

    public Task updateCategory(Long id, TaskCategory category) {
        Task task = findById(id);

        return taskRepository.updateCategory(task.getId(), category, Instant.now());
    }

    public Task updateAuthor(Long id, UUID authorId) {
        Task task = findById(id);

        return taskRepository.updateAuthor(task.getId(), authorId, Instant.now());
    }

    public Task updateAssignee(Long id, UUID assigneeId) {
        Task task = findById(id);

        return taskRepository.updateAssignee(task.getId(), assigneeId, Instant.now());
    }

    public Task updateDeadline(Long id, LocalDate deadlineDate) {
        Task task = findById(id);

        Instant deadlineAt =
                deadlineDate == null ? null : deadlineDate.atStartOfDay().toInstant(ZoneOffset.UTC);

        return taskRepository.updateDeadline(task.getId(), deadlineAt, Instant.now());
    }

    public Task updateDetails(
            Long id, String title, String description, LocalDate deadlineDate, TaskCategory category, UUID assigneeId) {
        Task task = findById(id);

        Instant deadlineAt =
                deadlineDate == null ? null : deadlineDate.atStartOfDay().toInstant(ZoneOffset.UTC);

        return taskRepository.updateDetails(
                task.getId(), title, description, deadlineAt, category, assigneeId, Instant.now());
    }

    public Task archive(Long id) {
        Task task = findById(id);

        return taskRepository.archive(task.getId(), Instant.now());
    }

    public Task restoreFromArchive(Long id) {
        Task task = findById(id);

        return taskRepository.restoreFromArchive(task.getId(), Instant.now());
    }
}
