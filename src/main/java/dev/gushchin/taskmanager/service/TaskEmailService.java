package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.TeamMemberNotFoundException;
import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamRepository;
import dev.gushchin.taskmanager.repository.UserRepository;
import dev.gushchin.taskmanager.view.EmailActionView;
import dev.gushchin.taskmanager.view.EmailContentView;
import dev.gushchin.taskmanager.view.EmailParameterView;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskEmailService {
    private static final DateTimeFormatter DEADLINE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"));

    private static final String TASK_UPDATED_HEADING = "Задача обновлена";
    private static final String NEW_TASK_HEADING = "Новая задача";
    private static final String ASSIGNEE_HEADING = "Вас назначили исполнителем задачи";
    private static final String AUTHOR_HEADING = "Вас назначили автором задачи";

    private static final String OPEN_TASK_LABEL = "Открыть задачу";
    private static final String TASK_LABEL = "Задача";
    private static final String TEAM_LABEL = "Команда";
    private static final String DEADLINE_LABEL = "Дедлайн";
    private static final String TASK_QUOTE_OPEN = "задачи «";
    private static final String CHANGED_SUFFIX = "» изменен";
    private static final String TASK_PREFIX = "Задача «";
    private static final String TASKS_PATH = "/tasks/";
    private static final String NO_ACCESS_NOTE = " У вас больше нет доступа к этой задаче.";

    private final EmailMessageSender messageSender;
    private final TaskPermissionService taskPermissionService;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public void sendTaskCreated(Task task, UUID actorUserId) {
        String actor = displayName(actorUserId);
        String subject = "У вас новая задача «" + task.getTitle() + "»";

        send(
                task.getAssigneeId(),
                actorUserId,
                subject,
                EmailContentView.builder()
                        .heading(NEW_TASK_HEADING)
                        .bodyParagraph(actor + " создал задачу и назначил вас исполнителем.")
                        .parameters(withDeadline(task))
                        .action(openTask(task)));

        if (!isSameUser(task.getAssigneeId(), task.getAuthorId())) {
            send(
                    task.getAuthorId(),
                    actorUserId,
                    subject,
                    EmailContentView.builder()
                            .heading(NEW_TASK_HEADING)
                            .bodyParagraph(actor + " создал задачу и назначил вас автором.")
                            .parameters(withDeadline(task))
                            .action(openTask(task)));
        }
    }

    public void sendAssigneeChanged(Task before, Task after, UUID actorUserId) {
        String actor = displayName(actorUserId);
        String title = after.getTitle();

        send(
                after.getAssigneeId(),
                actorUserId,
                "Вы назначены исполнителем " + TASK_QUOTE_OPEN + title + "»",
                EmailContentView.builder()
                        .heading(ASSIGNEE_HEADING)
                        .bodyParagraph(actor + " назначил вас исполнителем задачи.")
                        .parameters(withDeadline(after))
                        .action(openTask(after)));

        sendRoleRemoved(
                before.getAssigneeId(),
                actorUserId,
                after,
                "Вы больше не исполнитель " + TASK_QUOTE_OPEN + title + "»",
                actor + " снял вас с роли исполнителя задачи.");

        UUID observerId = after.getAuthorId();
        if (!isSameUser(observerId, before.getAssigneeId()) && !isSameUser(observerId, after.getAssigneeId())) {
            List<EmailParameterView> parameters = baseParameters(after);
            parameters.add(new EmailParameterView("Новый исполнитель", displayName(after.getAssigneeId())));
            parameters.add(new EmailParameterView("Прошлый исполнитель", displayName(before.getAssigneeId())));
            send(
                    observerId,
                    actorUserId,
                    "Исполнитель " + TASK_QUOTE_OPEN + title + CHANGED_SUFFIX,
                    EmailContentView.builder()
                            .heading(TASK_UPDATED_HEADING)
                            .bodyParagraph(actor + " изменил исполнителя задачи.")
                            .parameters(parameters)
                            .action(openTask(after)));
        }
    }

    public void sendAuthorChanged(Task before, Task after, UUID actorUserId) {
        String actor = displayName(actorUserId);
        String title = after.getTitle();

        send(
                after.getAuthorId(),
                actorUserId,
                "Вы назначены автором " + TASK_QUOTE_OPEN + title + "»",
                EmailContentView.builder()
                        .heading(AUTHOR_HEADING)
                        .bodyParagraph(actor + " назначил вас автором задачи.")
                        .parameters(withDeadline(after))
                        .action(openTask(after)));

        sendRoleRemoved(
                before.getAuthorId(),
                actorUserId,
                after,
                "Вы больше не автор " + TASK_QUOTE_OPEN + title + "»",
                actor + " снял вас с роли автора задачи.");

        UUID observerId = after.getAssigneeId();
        if (!isSameUser(observerId, before.getAuthorId()) && !isSameUser(observerId, after.getAuthorId())) {
            List<EmailParameterView> parameters = baseParameters(after);
            parameters.add(new EmailParameterView("Новый автор", displayName(after.getAuthorId())));
            parameters.add(new EmailParameterView("Прошлый автор", displayName(before.getAuthorId())));
            send(
                    observerId,
                    actorUserId,
                    "Автор " + TASK_QUOTE_OPEN + title + CHANGED_SUFFIX,
                    EmailContentView.builder()
                            .heading(TASK_UPDATED_HEADING)
                            .bodyParagraph(actor + " изменил автора задачи.")
                            .parameters(parameters)
                            .action(openTask(after)));
        }
    }

    public void sendStatusChanged(Task after, UUID actorUserId) {
        List<EmailParameterView> parameters = baseParameters(after);
        parameters.add(new EmailParameterView("Новый статус", after.getStatus().getDisplayName()));

        sendToParticipants(
                after,
                actorUserId,
                "Статус " + TASK_QUOTE_OPEN + after.getTitle() + CHANGED_SUFFIX,
                TASK_UPDATED_HEADING,
                displayName(actorUserId) + " изменил статус задачи.",
                parameters);
    }

    public void sendDeadlineChanged(Task after, UUID actorUserId) {
        List<EmailParameterView> parameters = baseParameters(after);
        parameters.add(new EmailParameterView("Новый дедлайн", formatDeadline(after.getDeadlineAt())));

        sendToParticipants(
                after,
                actorUserId,
                "Дедлайн " + TASK_QUOTE_OPEN + after.getTitle() + CHANGED_SUFFIX,
                TASK_UPDATED_HEADING,
                displayName(actorUserId) + " изменил дедлайн задачи.",
                parameters);
    }

    public void sendTaskArchived(Task before, Task after, UUID actorUserId) {
        boolean solved = before.getStatus() == TaskStatus.DONE;
        String actor = displayName(actorUserId);
        String title = after.getTitle();

        sendToParticipants(
                after,
                actorUserId,
                solved
                        ? TASK_PREFIX + title + "» решена и перенесена в архив"
                        : TASK_PREFIX + title + "» удалена и перенесена в архив",
                solved ? "Задача перенесена в архив" : "Задача удалена",
                solved
                        ? actor + " перенес решенную задачу «" + title + "» в архив."
                        : actor + " удалил задачу «" + title
                                + "». Задача была перенесена в архив с пометкой «удалена».",
                List.of());
    }

    public void sendTaskRestored(Task after, UUID actorUserId) {
        sendToParticipants(
                after,
                actorUserId,
                TASK_PREFIX + after.getTitle() + "» восстановлена",
                "Задача восстановлена из архива",
                displayName(actorUserId) + " восстановил задачу «" + after.getTitle() + "» из архива.",
                List.of());
    }

    public void sendCommentCreated(Task task, UUID actorUserId, Long commentId) {
        EmailActionView action = new EmailActionView(
                "Открыть комментарий", messageSender.baseUrl() + TASKS_PATH + task.getId() + "#comment-" + commentId);
        String subject = "Новый комментарий к " + TASK_QUOTE_OPEN + task.getTitle() + "»";
        String body = displayName(actorUserId) + " оставил комментарий к задаче.";

        send(
                task.getAuthorId(),
                actorUserId,
                subject,
                EmailContentView.builder()
                        .heading(TASK_UPDATED_HEADING)
                        .bodyParagraph(body)
                        .parameters(baseParameters(task))
                        .action(action));

        if (!isSameUser(task.getAuthorId(), task.getAssigneeId())) {
            send(
                    task.getAssigneeId(),
                    actorUserId,
                    subject,
                    EmailContentView.builder()
                            .heading(TASK_UPDATED_HEADING)
                            .bodyParagraph(body)
                            .parameters(baseParameters(task))
                            .action(action));
        }
    }

    private void sendRoleRemoved(UUID recipientId, UUID actorUserId, Task task, String subject, String body) {
        if (recipientId == null || isSameUser(recipientId, actorUserId)) {
            return;
        }

        boolean hasAccess = hasTaskAccess(task, recipientId);
        send(
                recipientId,
                actorUserId,
                subject,
                EmailContentView.builder()
                        .heading(TASK_UPDATED_HEADING)
                        .bodyParagraph(hasAccess ? body : body + NO_ACCESS_NOTE)
                        .parameters(baseParameters(task))
                        .action(hasAccess ? openTask(task) : null));
    }

    private void sendToParticipants(
            Task task,
            UUID actorUserId,
            String subject,
            String heading,
            String body,
            List<EmailParameterView> parameters) {
        EmailContentView.EmailContentViewBuilder builder = EmailContentView.builder()
                .heading(heading)
                .bodyParagraph(body)
                .parameters(parameters)
                .action(openTask(task));

        send(task.getAuthorId(), actorUserId, subject, builder);
        if (!isSameUser(task.getAuthorId(), task.getAssigneeId())) {
            send(task.getAssigneeId(), actorUserId, subject, builder);
        }
    }

    private void send(
            UUID recipientId, UUID actorUserId, String subject, EmailContentView.EmailContentViewBuilder builder) {
        User recipient = recipientId == null || isSameUser(recipientId, actorUserId)
                ? null
                : userRepository.findById(recipientId);
        if (recipient == null) {
            return;
        }

        try {
            messageSender.send(recipient.getEmail(), subject, builder.build());
        } catch (TransactionalEmailSendingException ex) {
            if (log.isWarnEnabled()) {
                log.warn(
                        "Не удалось отправить письмо о задаче: errorType={}",
                        ex.getClass().getSimpleName());
            }
        }
    }

    private boolean hasTaskAccess(Task task, UUID userId) {
        try {
            return taskPermissionService.canViewTask(task, userId);
        } catch (TeamMemberNotFoundException ex) {
            return false;
        }
    }

    private EmailActionView openTask(Task task) {
        return new EmailActionView(OPEN_TASK_LABEL, messageSender.baseUrl() + TASKS_PATH + task.getId());
    }

    private List<EmailParameterView> baseParameters(Task task) {
        List<EmailParameterView> parameters = new ArrayList<>();
        parameters.add(new EmailParameterView(TASK_LABEL, task.getTitle()));
        parameters.add(new EmailParameterView(TEAM_LABEL, teamName(task)));

        return parameters;
    }

    private List<EmailParameterView> withDeadline(Task task) {
        List<EmailParameterView> parameters = baseParameters(task);
        String deadline = formatDeadline(task.getDeadlineAt());
        if (deadline != null) {
            parameters.add(new EmailParameterView(DEADLINE_LABEL, deadline));
        }

        return parameters;
    }

    private String teamName(Task task) {
        Team team = teamRepository.findById(task.getTeamId());

        return team == null ? "" : team.getName();
    }

    private String displayName(UUID userId) {
        if (userId == null) {
            return "";
        }

        User user = userRepository.findById(userId);
        if (user == null) {
            return "";
        }

        return user.getName() == null || user.getName().isBlank() ? user.getEmail() : user.getName();
    }

    private String formatDeadline(Instant deadlineAt) {
        return deadlineAt == null ? null : DEADLINE_FORMATTER.format(deadlineAt.atOffset(ZoneOffset.UTC));
    }

    private boolean isSameUser(UUID first, UUID second) {
        return first != null && first.equals(second);
    }
}
