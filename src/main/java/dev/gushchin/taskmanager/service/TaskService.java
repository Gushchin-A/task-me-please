package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.AccessDeniedForTaskException;
import dev.gushchin.taskmanager.exception.BlankTaskDescriptionException;
import dev.gushchin.taskmanager.exception.MissingTaskDeadlineException;
import dev.gushchin.taskmanager.exception.TaskNotFoundException;
import dev.gushchin.taskmanager.exception.TaskTitleAlreadyExistsException;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskDetailsUpdate;
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
import java.util.Locale;
import java.util.Objects;
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
    private final TaskEmailService taskEmailService;

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

        String preparedTitle = prepareTitle(teamId, title, null);

        Instant now = Instant.now();

        Instant deadlineAt = requireDeadline(deadlineDate);
        String preparedDescription = prepareRequiredDescription(description);

        Task task = new Task(
                null,
                teamId,
                authorId,
                assigneeId,
                preparedTitle,
                preparedDescription,
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
        taskEmailService.sendTaskCreated(savedTask, authorId);

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

    public List<Task> filterByAuthorIds(List<Task> tasks, List<UUID> authorIds) {
        if (authorIds.isEmpty()) {
            return tasks;
        }

        return tasks.stream()
                .filter(task -> authorIds.contains(task.getAuthorId()))
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

    public List<Task> filterByAssigneeIds(List<Task> tasks, List<UUID> assigneeIds) {
        if (assigneeIds.isEmpty()) {
            return tasks;
        }

        return tasks.stream()
                .filter(task -> assigneeIds.contains(task.getAssigneeId()))
                .toList();
    }

    public List<Task> filterByTagId(List<Task> tasks, Long tagId) {
        if (tagId == null) {
            return tasks;
        }

        return tasks.stream().filter(task -> tagId.equals(task.getTagId())).toList();
    }

    public List<Task> filterByTagIds(List<Task> tasks, List<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return tasks;
        }

        return tasks.stream().filter(task -> tagIds.contains(task.getTagId())).toList();
    }

    public List<Task> sortTasks(List<Task> tasks, TaskSort sort) {
        TaskSort resolvedSort = sort == null ? TaskSort.NEWEST : sort;
        Comparator<Task> order =
                switch (resolvedSort) {
                    case NEWEST -> getNewestTaskOrder();
                    case OLDEST -> getOldestTaskOrder();
                    case DEADLINE_ASC ->
                        Comparator.comparing(Task::getDeadlineAt, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(getNewestTaskOrder());
                    case DEADLINE_DESC ->
                        Comparator.comparing(Task::getDeadlineAt, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(getNewestTaskOrder());
                };

        return tasks.stream().sorted(order).toList();
    }

    public List<TaskWithTeamView> sortTaskCards(List<TaskWithTeamView> taskCards, TaskSort sort) {
        TaskSort resolvedSort = sort == null ? TaskSort.NEWEST : sort;
        Comparator<TaskWithTeamView> order =
                switch (resolvedSort) {
                    case NEWEST -> getNewestTaskCardOrder();
                    case OLDEST -> getOldestTaskCardOrder();
                    case DEADLINE_ASC ->
                        Comparator.comparing(
                                        (TaskWithTeamView taskCard) ->
                                                taskCard.task().deadlineAt(),
                                        Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(getNewestTaskCardOrder());
                    case DEADLINE_DESC ->
                        Comparator.comparing(
                                        (TaskWithTeamView taskCard) ->
                                                taskCard.task().deadlineAt(),
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(getNewestTaskCardOrder());
                };

        return taskCards.stream().sorted(order).toList();
    }

    private Comparator<Task> getNewestTaskOrder() {
        return Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(getIdDescOrder());
    }

    private Comparator<Task> getOldestTaskOrder() {
        return Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Task::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private Comparator<Task> getIdDescOrder() {
        return Comparator.comparing(Task::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<TaskWithTeamView> getNewestTaskCardOrder() {
        return Comparator.comparing(
                        (TaskWithTeamView taskCard) -> taskCard.task().createdAt(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(getTaskCardIdDescOrder());
    }

    private Comparator<TaskWithTeamView> getOldestTaskCardOrder() {
        return Comparator.comparing(
                        (TaskWithTeamView taskCard) -> taskCard.task().createdAt(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(taskCard -> taskCard.task().id(), Comparator.nullsLast(Comparator.naturalOrder()));
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
            taskEmailService.sendStatusChanged(updatedTask, userId);
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
        requireTeamMemberIfChanged(task.getTeamId(), task.getAuthorId(), authorId);

        Task updatedTask = taskRepository.updateAuthor(task.getId(), authorId, Instant.now());
        if (!task.getAuthorId().equals(updatedTask.getAuthorId())) {
            notificationPublisher.taskAuthorChanged(task, updatedTask, userId);
            taskEmailService.sendAuthorChanged(task, updatedTask, userId);
        }

        return updatedTask;
    }

    @Transactional
    public Task updateAssignee(Long id, UUID assigneeId, UUID userId) {
        Task task = findByIdForUser(id, userId);

        checkCanUpdateTask(task, userId);
        requireTeamMemberIfChanged(task.getTeamId(), task.getAssigneeId(), assigneeId);

        Task updatedTask = taskRepository.updateAssignee(task.getId(), assigneeId, Instant.now());
        if (!task.getAssigneeId().equals(updatedTask.getAssigneeId())) {
            notificationPublisher.taskAssigneeChanged(task, updatedTask, userId);
            taskEmailService.sendAssigneeChanged(task, updatedTask, userId);
        }

        return updatedTask;
    }

    @Transactional
    public Task updateDeadline(Long id, LocalDate deadlineDate, UUID userId) {
        Task task = findByIdForUser(id, userId);

        checkCanUpdateTask(task, userId);

        Instant deadlineAt = requireDeadline(deadlineDate);

        Task updatedTask = taskRepository.updateDeadline(task.getId(), deadlineAt, Instant.now());
        if (!Objects.equals(task.getDeadlineAt(), updatedTask.getDeadlineAt())) {
            notificationPublisher.taskDeadlineChanged(task, updatedTask, userId);
            taskEmailService.sendDeadlineChanged(updatedTask, userId);
        }

        return updatedTask;
    }

    @Transactional
    public Task updateDetails(Long id, TaskDetailsUpdate update, UUID userId) {
        Task task = findByIdForUser(id, userId);
        final Task before = copyTask(task);

        checkCanUpdateTask(task, userId);
        requireTeamMemberIfChanged(task.getTeamId(), task.getAuthorId(), update.authorId());
        requireTeamMemberIfChanged(task.getTeamId(), task.getAssigneeId(), update.assigneeId());
        teamTagService.findByIdForTeam(update.tagId(), task.getTeamId());

        String preparedTitle = prepareTitle(task.getTeamId(), update.title(), task.getId());

        Instant deadlineAt = requireDeadline(update.deadlineDate());

        task.setTitle(preparedTitle);
        task.setDescription(prepareDescription(update.description()));
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
        taskEmailService.sendTaskArchived(task, updatedTask, userId);

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
        taskEmailService.sendTaskRestored(updatedTask, userId);

        return updatedTask;
    }

    private void requireTeamMemberIfChanged(Long teamId, UUID currentUserId, UUID newUserId) {
        if (!Objects.equals(newUserId, currentUserId)) {
            teamMemberService.findById(teamId, newUserId);
        }
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

    private Instant requireDeadline(LocalDate deadlineDate) {
        if (deadlineDate == null) {
            throw new MissingTaskDeadlineException();
        }

        return deadlineDate.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private String prepareDescription(String description) {
        if (description == null) {
            return null;
        }

        String preparedDescription = description.stripTrailing();

        if (preparedDescription.isBlank()) {
            throw new BlankTaskDescriptionException();
        }

        return preparedDescription;
    }

    private String prepareRequiredDescription(String description) {
        if (description == null) {
            throw new BlankTaskDescriptionException();
        }

        return prepareDescription(description);
    }

    private String prepareTitle(Long teamId, String title, Long excludedTaskId) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title must not be blank");
        }

        String preparedTitle = title.trim();
        String normalizedTitle = preparedTitle.toLowerCase(Locale.ROOT);

        if (taskRepository.existsActiveByTeamIdAndNormalizedTitle(teamId, normalizedTitle, excludedTaskId)) {
            throw new TaskTitleAlreadyExistsException(teamId, preparedTitle);
        }

        return preparedTitle;
    }

    private void publishDetailsChanges(Task before, Task after, UUID userId) {
        if (before.getStatus() != after.getStatus()) {
            notificationPublisher.taskStatusChanged(before, after, userId);
            taskEmailService.sendStatusChanged(after, userId);
        }
        if (!before.getAuthorId().equals(after.getAuthorId())) {
            notificationPublisher.taskAuthorChanged(before, after, userId);
            taskEmailService.sendAuthorChanged(before, after, userId);
        }
        if (!before.getAssigneeId().equals(after.getAssigneeId())) {
            notificationPublisher.taskAssigneeChanged(before, after, userId);
            taskEmailService.sendAssigneeChanged(before, after, userId);
        }
        if (!java.util.Objects.equals(before.getTitle(), after.getTitle())) {
            notificationPublisher.taskTitleChanged(before, after, userId);
        }
        if (!java.util.Objects.equals(before.getDescription(), after.getDescription())) {
            notificationPublisher.taskDescriptionChanged(before, after, userId);
        }
        if (!java.util.Objects.equals(before.getDeadlineAt(), after.getDeadlineAt())) {
            notificationPublisher.taskDeadlineChanged(before, after, userId);
            taskEmailService.sendDeadlineChanged(after, userId);
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
