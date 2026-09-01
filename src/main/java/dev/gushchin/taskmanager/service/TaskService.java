package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.AccessDeniedForTaskException;
import dev.gushchin.taskmanager.exception.TaskNotFoundException;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskDetailsUpdate;
import dev.gushchin.taskmanager.model.TaskRoleFilter;
import dev.gushchin.taskmanager.model.TaskSort;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.TeamTag;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskPermissionService taskPermissionService;
    private final TeamTagService teamTagService;
    private final TeamMemberService teamMemberService;
    private final NotificationPublisher notificationPublisher;

    public List<Task> findByTeamId(Long teamId) {
        return taskRepository.findByTeamId(teamId).stream()
                .filter(Predicate.not(Task::isDeleted))
                .toList();
    }

    public List<Task> findByTeamIds(List<Long> teamIds) {
        return teamIds.stream().flatMap(teamId -> findByTeamId(teamId).stream()).toList();
    }

    public List<Task> findVisibleByTeamId(Long teamId, UUID userId) {
        List<Task> tasks = findByTeamId(teamId);
        return filterByVisibility(tasks, userId);
    }

    public Task findById(Long id) {
        Task task = taskRepository.findById(id);

        if (task == null || task.isDeleted()) {
            throw new TaskNotFoundException(id);
        }

        return task;
    }

    public Task findByIdForUser(Long id, UUID userId) {
        Task task = findById(id);

        if (!taskPermissionService.canViewTask(task, userId)) {
            throw new AccessDeniedForTaskException();
        }

        return task;
    }

    @Transactional
    public Task create(
            Long teamId,
            UUID authorId,
            UUID assigneeId,
            String title,
            String description,
            LocalDate deadlineDate,
            Long tagId) {
        teamMemberService.findById(teamId, authorId);
        teamMemberService.findById(teamId, assigneeId);
        teamTagService.findByIdForTeam(tagId, teamId);

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
                tagId,
                now,
                now,
                null,
                false,
                false);

        Task savedTask = taskRepository.save(task);
        notificationPublisher.taskCreated(savedTask, authorId);

        return savedTask;
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

    public List<Task> filterByArchived(List<Task> tasks, boolean archived) {
        return tasks.stream().filter(task -> task.isArchived() == archived).toList();
    }

    public List<Task> filterByVisibility(List<Task> tasks, UUID userId) {
        return tasks.stream()
                .filter(task -> taskPermissionService.canViewTask(task, userId))
                .toList();
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

    public List<Task> filterByAuthorId(List<Task> tasks, UUID authorId) {
        if (authorId == null) {
            return tasks;
        }

        return tasks.stream()
                .filter(task -> task.getAuthorId().equals(authorId))
                .toList();
    }

    public List<Task> filterByAssigneeId(List<Task> tasks, UUID assigneeId) {
        if (assigneeId == null) {
            return tasks;
        }

        return tasks.stream()
                .filter(task -> task.getAssigneeId().equals(assigneeId))
                .toList();
    }

    public List<Task> filterByTagId(List<Task> tasks, Long tagId) {
        if (tagId == null) {
            return tasks;
        }

        return tasks.stream().filter(task -> tagId.equals(task.getTagId())).toList();
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

        if (sort == TaskSort.TAG) {
            Comparator<Task> order = Comparator.comparing(
                            (Task task) -> getTagName(task.getTagId()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(getDefaultTaskOrder());

            return tasks.stream().sorted(order).toList();
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
        } else if (sort == TaskSort.TAG) {
            order = Comparator.comparing(
                            (TaskWithTeamView taskCard) -> taskCard.task().tagName(), String.CASE_INSENSITIVE_ORDER)
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

    @Transactional
    public Task updateStatus(Long id, TaskStatus status, UUID userId) {
        Task task = findByIdForUser(id, userId);

        checkCanUpdateStatus(task, userId);

        Task updatedTask = taskRepository.updateStatus(task.getId(), status, Instant.now());
        if (task.getStatus() != updatedTask.getStatus()) {
            notificationPublisher.taskStatusChanged(task, updatedTask, userId);
        }

        return updatedTask;
    }

    @Transactional
    public Task updateTag(Long id, Long tagId, UUID userId) {
        Task task = findByIdForUser(id, userId);

        checkCanUpdateTask(task, userId);
        teamTagService.findByIdForTeam(tagId, task.getTeamId());

        Task updatedTask = taskRepository.updateTag(task.getId(), tagId, Instant.now());
        if (!task.getTagId().equals(updatedTask.getTagId())) {
            notificationPublisher.taskTagChanged(task, updatedTask, userId);
        }

        return updatedTask;
    }

    @Transactional
    public Task updateAuthor(Long id, UUID authorId, UUID userId) {
        Task task = findByIdForUser(id, userId);

        checkCanUpdateTask(task, userId);
        teamMemberService.findById(task.getTeamId(), authorId);

        Task updatedTask = taskRepository.updateAuthor(task.getId(), authorId, Instant.now());
        if (!task.getAuthorId().equals(updatedTask.getAuthorId())) {
            notificationPublisher.taskAuthorChanged(task, updatedTask, userId);
        }

        return updatedTask;
    }

    @Transactional
    public Task updateAssignee(Long id, UUID assigneeId, UUID userId) {
        Task task = findByIdForUser(id, userId);

        checkCanUpdateTask(task, userId);
        teamMemberService.findById(task.getTeamId(), assigneeId);

        Task updatedTask = taskRepository.updateAssignee(task.getId(), assigneeId, Instant.now());
        if (!task.getAssigneeId().equals(updatedTask.getAssigneeId())) {
            notificationPublisher.taskAssigneeChanged(task, updatedTask, userId);
        }

        return updatedTask;
    }

    @Transactional
    public Task updateDeadline(Long id, LocalDate deadlineDate, UUID userId) {
        Task task = findByIdForUser(id, userId);

        checkCanUpdateTask(task, userId);

        Instant deadlineAt =
                deadlineDate == null ? null : deadlineDate.atStartOfDay().toInstant(ZoneOffset.UTC);

        Task updatedTask = taskRepository.updateDeadline(task.getId(), deadlineAt, Instant.now());
        if (!java.util.Objects.equals(task.getDeadlineAt(), updatedTask.getDeadlineAt())) {
            notificationPublisher.taskDeadlineChanged(task, updatedTask, userId);
        }

        return updatedTask;
    }

    @Transactional
    public Task updateDetails(Long id, TaskDetailsUpdate update, UUID userId) {
        Task task = findByIdForUser(id, userId);
        final Task before = copyTask(task);

        checkCanUpdateTask(task, userId);
        teamMemberService.findById(task.getTeamId(), update.authorId());
        teamMemberService.findById(task.getTeamId(), update.assigneeId());
        teamTagService.findByIdForTeam(update.tagId(), task.getTeamId());

        Instant deadlineAt = update.deadlineDate() == null
                ? null
                : update.deadlineDate().atStartOfDay().toInstant(ZoneOffset.UTC);

        task.setTitle(update.title());
        task.setDescription(update.description());
        task.setDeadlineAt(deadlineAt);
        task.setStatus(update.status());
        task.setTagId(update.tagId());
        task.setAuthorId(update.authorId());
        task.setAssigneeId(update.assigneeId());

        Task updatedTask = taskRepository.updateDetails(task, Instant.now());
        publishDetailsChanges(before, updatedTask, userId);

        return updatedTask;
    }

    @Transactional
    public Task archive(Long id, UUID userId) {
        Task task = findById(id);

        if (!taskPermissionService.canArchiveTask(task, userId)) {
            throw new AccessDeniedForTaskException();
        }

        Task updatedTask = taskRepository.archive(task.getId(), userId, Instant.now());
        notificationPublisher.taskArchived(task, updatedTask, userId);

        return updatedTask;
    }

    @Transactional
    public Task restoreFromArchive(Long id, UUID userId) {
        Task task = findById(id);

        if (!taskPermissionService.canRestoreTask(task, userId)) {
            throw new AccessDeniedForTaskException();
        }

        Task updatedTask = taskRepository.restoreFromArchive(task.getId(), Instant.now());
        notificationPublisher.taskRestored(task, updatedTask, userId);

        return updatedTask;
    }

    private void checkCanUpdateTask(Task task, UUID userId) {
        if (!taskPermissionService.canUpdateTask(task, userId)) {
            throw new AccessDeniedForTaskException();
        }
    }

    private void checkCanUpdateStatus(Task task, UUID userId) {
        if (!taskPermissionService.canUpdateStatus(task, userId)) {
            throw new AccessDeniedForTaskException();
        }
    }

    public boolean canUpdateTask(Task task, UUID userId) {
        return taskPermissionService.canUpdateTask(task, userId);
    }

    public boolean canUpdateStatus(Task task, UUID userId) {
        return taskPermissionService.canUpdateStatus(task, userId);
    }

    public boolean canArchiveTask(Task task, UUID userId) {
        return taskPermissionService.canArchiveTask(task, userId);
    }

    public boolean canRestoreTask(Task task, UUID userId) {
        return taskPermissionService.canRestoreTask(task, userId);
    }

    public boolean isTeamOwner(Task task, UUID userId) {
        return taskPermissionService.isTeamOwner(task, userId);
    }

    private String getTagName(Long tagId) {
        TeamTag teamTag = teamTagService.findById(tagId);

        return teamTag.getName();
    }

    private void publishDetailsChanges(Task before, Task after, UUID userId) {
        if (before.getStatus() != after.getStatus()) {
            notificationPublisher.taskStatusChanged(before, after, userId);
        }
        if (!before.getAuthorId().equals(after.getAuthorId())) {
            notificationPublisher.taskAuthorChanged(before, after, userId);
        }
        if (!before.getAssigneeId().equals(after.getAssigneeId())) {
            notificationPublisher.taskAssigneeChanged(before, after, userId);
        }
        if (!java.util.Objects.equals(before.getTitle(), after.getTitle())) {
            notificationPublisher.taskTitleChanged(before, after, userId);
        }
        if (!java.util.Objects.equals(before.getDescription(), after.getDescription())) {
            notificationPublisher.taskDescriptionChanged(before, after, userId);
        }
        if (!java.util.Objects.equals(before.getDeadlineAt(), after.getDeadlineAt())) {
            notificationPublisher.taskDeadlineChanged(before, after, userId);
        }
        if (!java.util.Objects.equals(before.getTagId(), after.getTagId())) {
            notificationPublisher.taskTagChanged(before, after, userId);
        }
    }

    private Task copyTask(Task task) {
        return new Task(
                task.getId(),
                task.getTeamId(),
                task.getAuthorId(),
                task.getAssigneeId(),
                task.getTitle(),
                task.getDescription(),
                task.getDeadlineAt(),
                task.getStatus(),
                task.getTagId(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getArchivedBy(),
                task.isArchived(),
                task.isDeleted());
    }
}
