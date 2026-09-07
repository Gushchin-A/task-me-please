package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamRepository;
import dev.gushchin.taskmanager.repository.UserRepository;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TaskEmailServiceTest {
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID ASSIGNEE_ID = UUID.randomUUID();

    private final TransactionalEmailSender emailSender = mock(TransactionalEmailSender.class);
    private final TaskPermissionService taskPermissionService = mock(TaskPermissionService.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private final TaskEmailService taskEmailService = new TaskEmailService(
            new EmailMessageSender(
                    emailSender,
                    new EmailTemplateRenderer(TemplateEngine.createPrecompiled(ContentType.Html)),
                    new EmailTextRenderer(),
                    createAppProperties()),
            taskPermissionService,
            teamRepository,
            userRepository);

    @Test
    void sendTaskCreatedShouldWriteToAssigneeWithParameters() {
        stubUsers();
        Task task = createTask(AUTHOR_ID, ASSIGNEE_ID, TaskStatus.OPEN);

        taskEmailService.sendTaskCreated(task, ACTOR_ID);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("assignee@test.com"),
                        eq("У вас новая задача «Подготовить релиз на Render»"),
                        htmlCaptor.capture(),
                        anyString());

        String html = htmlCaptor.getValue();
        assertAll(
                () -> assertTrue(html.contains("Новая задача")),
                () -> assertTrue(html.contains("Андрей создал задачу и назначил вас исполнителем.")),
                () -> assertTrue(html.contains("Задача:</b> Подготовить релиз на Render")),
                () -> assertTrue(html.contains("Команда:</b> Креативный заводиксвс")),
                () -> assertTrue(html.contains("Дедлайн:</b> 9 сентября 2026")));
    }

    @Test
    void sendTaskCreatedShouldNotWriteToActor() {
        stubUsers();
        Task task = createTask(ACTOR_ID, ACTOR_ID, TaskStatus.OPEN);

        taskEmailService.sendTaskCreated(task, ACTOR_ID);

        verify(emailSender, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void removedAssigneeWithAccessShouldGetButton() {
        stubUsers();
        Task before = createTask(AUTHOR_ID, ASSIGNEE_ID, TaskStatus.OPEN);
        Task after = createTask(AUTHOR_ID, AUTHOR_ID, TaskStatus.OPEN);
        when(taskPermissionService.canViewTask(any(Task.class), eq(ASSIGNEE_ID)))
                .thenReturn(true);

        taskEmailService.sendAssigneeChanged(before, after, ACTOR_ID);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("assignee@test.com"),
                        eq("Вы больше не исполнитель задачи «Подготовить релиз на Render»"),
                        htmlCaptor.capture(),
                        anyString());

        String html = htmlCaptor.getValue();
        assertAll(
                () -> assertTrue(html.contains("Андрей снял вас с роли исполнителя задачи.")),
                () -> assertFalse(html.contains("У вас больше нет доступа к этой задаче")),
                () -> assertTrue(html.contains("Открыть задачу")));
    }

    @Test
    void removedAssigneeWithoutAccessShouldGetNoticeInsteadOfButton() {
        stubUsers();
        Task before = createTask(AUTHOR_ID, ASSIGNEE_ID, TaskStatus.OPEN);
        Task after = createTask(AUTHOR_ID, AUTHOR_ID, TaskStatus.OPEN);
        when(taskPermissionService.canViewTask(any(Task.class), eq(ASSIGNEE_ID)))
                .thenReturn(false);

        taskEmailService.sendAssigneeChanged(before, after, ACTOR_ID);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("assignee@test.com"),
                        eq("Вы больше не исполнитель задачи «Подготовить релиз на Render»"),
                        htmlCaptor.capture(),
                        anyString());

        String html = htmlCaptor.getValue();
        assertAll(
                () -> assertTrue(html.contains(
                        "Андрей снял вас с роли исполнителя задачи. " + "У вас больше нет доступа к этой задаче.")),
                () -> assertFalse(html.contains("Открыть задачу")),
                () -> assertFalse(html.contains("bgcolor=\"#1f883d\"")));
    }

    @Test
    void sendTaskArchivedShouldUseSolvedWordingForDoneTask() {
        stubUsers();
        Task before = createTask(AUTHOR_ID, ASSIGNEE_ID, TaskStatus.DONE);
        Task after = createTask(AUTHOR_ID, ASSIGNEE_ID, TaskStatus.DONE);

        taskEmailService.sendTaskArchived(before, after, ACTOR_ID);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("author@test.com"),
                        eq("Задача «Подготовить релиз на Render» решена и перенесена в архив"),
                        htmlCaptor.capture(),
                        anyString());

        assertTrue(htmlCaptor.getValue().contains("Задача перенесена в архив"));
    }

    @Test
    void sendTaskArchivedShouldUseDeletedWordingForUnfinishedTask() {
        stubUsers();
        Task before = createTask(AUTHOR_ID, ASSIGNEE_ID, TaskStatus.IN_PROGRESS);
        Task after = createTask(AUTHOR_ID, ASSIGNEE_ID, TaskStatus.IN_PROGRESS);

        taskEmailService.sendTaskArchived(before, after, ACTOR_ID);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("author@test.com"),
                        eq("Задача «Подготовить релиз на Render» удалена и перенесена в архив"),
                        htmlCaptor.capture(),
                        anyString());

        assertTrue(htmlCaptor.getValue().contains("Задача удалена"));
    }

    @Test
    void sendStatusChangedShouldListNewStatus() {
        stubUsers();
        Task after = createTask(AUTHOR_ID, ASSIGNEE_ID, TaskStatus.IN_PROGRESS);

        taskEmailService.sendStatusChanged(after, ACTOR_ID);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("author@test.com"),
                        eq("Статус задачи «Подготовить релиз на Render» изменен"),
                        htmlCaptor.capture(),
                        anyString());

        assertTrue(htmlCaptor.getValue().contains("Новый статус:</b> В работе"));
    }

    @Test
    void sendStatusChangedShouldReachBothParticipants() {
        stubUsers();
        Task after = createTask(AUTHOR_ID, ASSIGNEE_ID, TaskStatus.DONE);

        taskEmailService.sendStatusChanged(after, ACTOR_ID);

        verify(emailSender).send(eq("author@test.com"), anyString(), anyString(), anyString());
        verify(emailSender).send(eq("assignee@test.com"), anyString(), anyString(), anyString());
    }

    private void stubUsers() {
        when(userRepository.findById(ACTOR_ID)).thenReturn(createUser(ACTOR_ID, "actor@test.com", "Андрей"));
        when(userRepository.findById(AUTHOR_ID)).thenReturn(createUser(AUTHOR_ID, "author@test.com", "Чубакка"));
        when(userRepository.findById(ASSIGNEE_ID)).thenReturn(createUser(ASSIGNEE_ID, "assignee@test.com", "Йода"));
        when(teamRepository.findById(7L))
                .thenReturn(new Team(7L, "Креативный заводиксвс", ACTOR_ID, Instant.now(), Instant.now(), false));
    }

    private AppProperties createAppProperties() {
        AppProperties appProperties = new AppProperties();
        appProperties.setBaseUrl("https://task-me-please.test");

        return appProperties;
    }

    private Task createTask(UUID authorId, UUID assigneeId, TaskStatus status) {
        Task task = new Task();
        task.setId(94L);
        task.setTeamId(7L);
        task.setAuthorId(authorId);
        task.setAssigneeId(assigneeId);
        task.setTitle("Подготовить релиз на Render");
        task.setStatus(status);
        task.setDeadlineAt(LocalDate.of(2026, 9, 9).atStartOfDay().toInstant(ZoneOffset.UTC));

        return task;
    }

    private User createUser(UUID id, String email, String name) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);

        return user;
    }
}
