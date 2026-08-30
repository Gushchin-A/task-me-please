package dev.gushchin.taskmanager.controller;

import static dev.gushchin.taskmanager.jooq.Tables.COMMENTS;
import static dev.gushchin.taskmanager.jooq.Tables.TASKS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAMS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_INVITATIONS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_MEMBERS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_TAGS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.exception.AccessDeniedForTaskException;
import dev.gushchin.taskmanager.exception.TeamMemberNotFoundException;
import dev.gushchin.taskmanager.model.Comment;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamTag;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.CommentRepository;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.CommentService;
import dev.gushchin.taskmanager.service.TaskService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.TeamTagService;
import dev.gushchin.taskmanager.service.UserService;
import java.time.Instant;
import java.time.LocalDate;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class TaskPageControllerIntegrationTest extends IntegrationTestBase {
    private static final LocalDate DEADLINE_DATE = LocalDate.of(2035, 1, 20);

    private static final Instant COMMENT_CREATED_AT = Instant.parse("2026-06-15T16:17:18.176447Z");

    @Autowired
    private DSLContext dsl;

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TeamTagService teamTagService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentService commentService;

    private User owner;
    private User secondUser;
    private Team team;
    private TeamTag kinopoiskTag;
    private TeamTag plusTag;
    private Task task;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        owner = userService.create("owner@test.com", "Owner", "qwerty");

        secondUser = userService.create("second@test.com", "Second", "qwerty");

        team = teamService.create("Project Team", owner.getId());
        teamMemberService.addMember(team.getId(), secondUser.getId());
        kinopoiskTag = teamTagService.create(team.getId(), "Кинопоиск");
        plusTag = teamTagService.create(team.getId(), "Плюс");

        task = taskService.create(
                team.getId(),
                owner.getId(),
                secondUser.getId(),
                "Important task",
                "Task description",
                DEADLINE_DATE,
                kinopoiskTag.getId());
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void showTaskPageReturnOkAndContent() throws Exception {
        // given
        taskService.updateStatus(task.getId(), TaskStatus.DONE, owner.getId());
        taskService.archive(task.getId(), owner.getId());

        Comment comment = new Comment(
                null, task.getId(), owner.getId(), "Initial comment", COMMENT_CREATED_AT, COMMENT_CREATED_AT, false);

        Comment savedComment = commentRepository.save(comment);

        // when
        mockMvc.perform(get("/tasks/" + task.getId())
                        .with(user(new AuthUser(owner)))
                        .flashAttr("successMessage", "Задача восстановлена из архива"))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-flash-message")))
                .andExpect(content().string(containsString("Задача восстановлена из архива")))
                .andExpect(content().string(containsString("Important task")))
                .andExpect(content().string(containsString("Task description")))
                .andExpect(content().string(containsString("task-detail-status-archive\">Архив")))
                .andExpect(content().string(containsString("Задача была выполнена и перенесена в архив")))
                .andExpect(content().string(containsString("Вернуть из архива")))
                .andExpect(content().string(containsString("20 января 2035")))
                .andExpect(content().string(containsString("Кинопоиск")))
                .andExpect(content().string(containsString("Initial comment")))
                .andExpect(content().string(containsString("class=\"task-detail-layout\"")))
                .andExpect(content().string(containsString("class=\"task-parameters-panel\"")))
                .andExpect(content().string(containsString("class=\"task-conversation\"")))
                .andExpect(content().string(containsString("class=\"comment-avatar\"")))
                .andExpect(content().string(containsString("id=\"task-description\"")))
                .andExpect(content().string(containsString("task-description-status-not-relevant")))
                .andExpect(content().string(containsString("id=\"comment-" + savedComment.getId() + "\"")))
                .andExpect(content().string(containsString("data-task-anchor-copy=\"task-description\"")))
                .andExpect(content()
                        .string(containsString("data-task-anchor-copy=\"comment-" + savedComment.getId() + "\"")))
                .andExpect(content().string(not(containsString("data-task-title-edit-open"))))
                .andExpect(content().string(containsString("data-comment-edit-form")))
                .andExpect(content().string(containsString("disabled data-changed-value-submit")))
                .andExpect(content().string(not(containsString("class=\"comment-create-form\""))))
                .andExpect(content().string(not(containsString("placeholder=\"Оставьте комментарий\""))))
                .andExpect(content().string(containsString(">O</span>")))
                .andExpect(content().string(containsString("15 июня 2026 18:17")));
    }

    @Test
    void archivedOpenTaskShouldShowDeletedSummaryAndHideRestoreActionFromAssignee() throws Exception {
        taskService.archive(task.getId(), owner.getId());

        mockMvc.perform(get("/tasks/" + task.getId()).with(user(new AuthUser(secondUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("task-detail-status-archive\">Архив")))
                .andExpect(content().string(containsString("task-detail-status-open\">Открыто")))
                .andExpect(content().string(containsString("Задача была удалена")))
                .andExpect(content().string(not(containsString("Вернуть из архива"))))
                .andExpect(content().string(not(containsString("data-task-title-edit-open"))))
                .andExpect(content().string(not(containsString("class=\"comment-create-form\""))));
    }

    @Test
    void assigneeShouldSeeStatusEditingAndReadOnlyTaskParameters() throws Exception {
        mockMvc.perform(get("/tasks/" + task.getId()).with(user(new AuthUser(secondUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-tooltip=\"Изменить параметры задачи\"")))
                .andExpect(content().string(containsString("name=\"status\"")))
                .andExpect(content().string(containsString("task-parameter-read-only")))
                .andExpect(content().string(containsString("name=\"deadlineDate\"")))
                .andExpect(content().string(containsString("name=\"authorId\"")))
                .andExpect(content().string(containsString("name=\"assigneeId\"")))
                .andExpect(content().string(containsString("name=\"tagId\"")))
                .andExpect(content().string(not(containsString("data-task-parameters-state-action"))))
                .andExpect(content().string(not(containsString("Удалить задачу"))))
                .andExpect(content().string(not(containsString("Перенести в архив"))));
    }

    @Test
    void taskPageShouldShowArchiveActionForDoneTaskAndDeleteActionForOtherStatuses() throws Exception {
        mockMvc.perform(get("/tasks/" + task.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Удалить задачу")))
                .andExpect(content().string(containsString("data-task-parameters-state-action")))
                .andExpect(content().string(containsString("name=\"returnTo\" value=\"team\"")))
                .andExpect(content().string(containsString(">Команда</span>")))
                .andExpect(content()
                        .string(containsString(
                                "class=\"task-parameter-value task-parameter-team\" href=\"/teams/" + team.getId())))
                .andExpect(content().string(not(containsString("Перенести в архив"))));

        taskService.updateStatus(task.getId(), TaskStatus.DONE, owner.getId());

        mockMvc.perform(get("/tasks/" + task.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Перенести в архив")))
                .andExpect(content().string(not(containsString("Удалить задачу"))));
    }

    @Test
    void inlineUpdatesShouldRedirectAndUpdateTask() throws Exception {
        // given
        Long taskId = task.getId();

        // when
        mockMvc.perform(post("/tasks/" + taskId + "/status")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("status", TaskStatus.IN_PROGRESS.name())
                        .param("returnTo", "task"))
                // then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/" + taskId));

        // when
        mockMvc.perform(post("/tasks/" + taskId + "/author")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("authorId", secondUser.getId().toString())
                        .param("returnTo", "task"))
                // then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/" + taskId));

        // when
        mockMvc.perform(post("/tasks/" + taskId + "/assignee")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("assigneeId", owner.getId().toString())
                        .param("returnTo", "task"))
                // then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/" + taskId));

        // when
        mockMvc.perform(post("/tasks/" + taskId + "/deadline")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("deadlineDate", "2035-01-20")
                        .param("returnTo", "task"))
                // then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/" + taskId));

        // when
        mockMvc.perform(post("/tasks/" + taskId + "/tag")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("tagId", plusTag.getId().toString())
                        .param("returnTo", "task"))
                // then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/" + taskId));

        // when
        mockMvc.perform(get("/tasks/" + taskId).with(user(new AuthUser(owner))))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(TaskStatus.IN_PROGRESS.name())))
                .andExpect(content().string(containsString("Second")))
                .andExpect(content().string(containsString("Owner")))
                .andExpect(content().string(containsString("20 января 2035")))
                .andExpect(content().string(containsString("Плюс")));
    }

    @Test
    void commentsAndArchiveShouldWork() throws Exception {
        // given
        Long taskId = task.getId();
        taskService.updateStatus(taskId, TaskStatus.DONE, owner.getId());

        // when
        mockMvc.perform(post("/tasks/" + taskId + "/comments")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("message", "Created comment"))
                // then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/" + taskId));

        Comment createdComment = commentRepository.findByTaskId(taskId).getFirst();

        // when
        mockMvc.perform(get("/tasks/" + taskId).with(user(new AuthUser(owner))))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Created comment")));

        // when
        mockMvc.perform(post("/tasks/" + taskId + "/comments/" + createdComment.getId() + "/edit")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("message", "Edited comment"))
                // then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/" + taskId));

        // when
        mockMvc.perform(get("/tasks/" + taskId).with(user(new AuthUser(owner))))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edited comment")))
                .andExpect(content().string(containsString("Отредактировано")));

        // when
        mockMvc.perform(post("/tasks/" + taskId + "/comments/" + createdComment.getId() + "/delete")
                        .with(csrf())
                        .with(user(new AuthUser(owner))))
                // then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/" + taskId));

        // when
        mockMvc.perform(get("/tasks/" + taskId).with(user(new AuthUser(owner))))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Edited comment"))));

        // when
        mockMvc.perform(post("/tasks/" + taskId + "/archive")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("returnTo", "team"))
                // then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId()))
                .andExpect(flash().attribute("successMessage", "Задача была перенесена в архив"));

        // when
        mockMvc.perform(get("/tasks/" + taskId).with(user(new AuthUser(owner))))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Архив")))
                .andExpect(content().string(containsString("Вернуть из архива")));

        // when
        mockMvc.perform(post("/tasks/" + taskId + "/restore")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("returnTo", "task"))
                // then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/" + taskId))
                .andExpect(flash().attribute("successMessage", "Задача восстановлена из архива"));

        // when
        mockMvc.perform(get("/tasks/" + taskId).with(user(new AuthUser(owner))))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Перенести в архив")));
    }

    @Test
    void archivingUnfinishedTaskShouldShowDeletedMessage() throws Exception {
        mockMvc.perform(post("/tasks/" + task.getId() + "/archive")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("returnTo", "team"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId()))
                .andExpect(flash().attribute("successMessage", "Задача удалена и перенесена в архив"));
    }

    @Test
    void newTaskPageShouldShowLinearPrimerForm() throws Exception {
        User longNameMember = userService.create("long-name-member@test.com", "Александр Сергеевич", "qwerty");
        teamMemberService.addMember(team.getId(), longNameMember.getId());

        mockMvc.perform(get("/tasks/new?teamId=" + team.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Создание новой задачи")))
                .andExpect(content().string(containsString("class=\"task-create-form\"")))
                .andExpect(content().string(containsString("name=\"teamId\"")))
                .andExpect(content().string(containsString("maxlength=\"100\"")))
                .andExpect(content().string(containsString("0</span> / 100 символов")))
                .andExpect(content().string(containsString("Выберите команду")))
                .andExpect(content().string(containsString("Выберите исполнителя")))
                .andExpect(content().string(containsString("Выберите тег")))
                .andExpect(content().string(containsString("Александр Серге...")))
                .andExpect(content().string(containsString("Отменить")))
                .andExpect(content().string(containsString("title=\"Еще не реализовано :(\"")))
                .andExpect(content().string(not(containsString("class=\"form-card\""))));
    }

    @Test
    void cardEditShouldUpdateFieldsWithoutChangingDescription() throws Exception {
        mockMvc.perform(post("/tasks/" + task.getId() + "/edit")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("title", "Updated title")
                        .param("status", TaskStatus.IN_PROGRESS.name())
                        .param("authorId", secondUser.getId().toString())
                        .param("assigneeId", owner.getId().toString())
                        .param("deadlineDate", LocalDate.of(2035, 2, 10).toString())
                        .param("tagId", plusTag.getId().toString())
                        .param("returnTo", "tasks"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"));

        Task updatedTask = taskService.findById(task.getId());

        assertEquals("Updated title", updatedTask.getTitle());
        assertEquals("Task description", updatedTask.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.getStatus());
        assertEquals(secondUser.getId(), updatedTask.getAuthorId());
        assertEquals(owner.getId(), updatedTask.getAssigneeId());
        assertEquals(plusTag.getId(), updatedTask.getTagId());
    }

    @Test
    void taskAuthorShouldAtomicallyChangeAuthorAndAssigneeFromCardEndpoints() throws Exception {
        Task myTasksTask = taskService.create(
                team.getId(),
                secondUser.getId(),
                owner.getId(),
                "My tasks card",
                "Task description",
                DEADLINE_DATE,
                kinopoiskTag.getId());

        mockMvc.perform(post("/tasks/" + myTasksTask.getId() + "/edit")
                        .with(csrf())
                        .with(user(new AuthUser(secondUser)))
                        .param("title", myTasksTask.getTitle())
                        .param("status", TaskStatus.IN_PROGRESS.name())
                        .param("authorId", owner.getId().toString())
                        .param("assigneeId", secondUser.getId().toString())
                        .param("deadlineDate", DEADLINE_DATE.toString())
                        .param("tagId", kinopoiskTag.getId().toString())
                        .param("returnTo", "tasks"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"));

        Task updatedMyTasksTask = taskService.findById(myTasksTask.getId());

        assertEquals(owner.getId(), updatedMyTasksTask.getAuthorId());
        assertEquals(secondUser.getId(), updatedMyTasksTask.getAssigneeId());

        Task teamTask = taskService.create(
                team.getId(),
                secondUser.getId(),
                secondUser.getId(),
                "Team card",
                "Task description",
                DEADLINE_DATE,
                kinopoiskTag.getId());

        mockMvc.perform(post("/tasks/" + teamTask.getId() + "/edit")
                        .with(csrf())
                        .with(user(new AuthUser(secondUser)))
                        .param("title", teamTask.getTitle())
                        .param("status", TaskStatus.DONE.name())
                        .param("authorId", owner.getId().toString())
                        .param("assigneeId", owner.getId().toString())
                        .param("deadlineDate", DEADLINE_DATE.toString())
                        .param("tagId", kinopoiskTag.getId().toString())
                        .param("returnTo", "team"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId()));

        Task updatedTeamTask = taskService.findById(teamTask.getId());

        assertEquals(owner.getId(), updatedTeamTask.getAuthorId());
        assertEquals(owner.getId(), updatedTeamTask.getAssigneeId());
        assertEquals(TaskStatus.DONE, updatedTeamTask.getStatus());
    }

    @Test
    void taskAuthorShouldSeeAuthorFieldAndAccessWarningInCardEditForm() throws Exception {
        Task authoredTask = taskService.create(
                team.getId(),
                secondUser.getId(),
                owner.getId(),
                "Authored task",
                "Task description",
                DEADLINE_DATE,
                kinopoiskTag.getId());

        mockMvc.perform(get("/tasks").with(user(new AuthUser(secondUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"task-author-" + authoredTask.getId() + "\"")))
                .andExpect(content().string(containsString("name=\"authorId\"")))
                .andExpect(content().string(containsString("Выберите автора")))
                .andExpect(content().string(containsString("После смены автора вы потеряете доступ к этой задаче")));

        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(secondUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("После смены автора вы потеряете доступ к этой задаче")));

        mockMvc.perform(get("/tasks/" + authoredTask.getId()).with(user(new AuthUser(secondUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"authorId\"")))
                .andExpect(content().string(containsString("После смены автора вы потеряете доступ к этой задаче")));
    }

    @Test
    void taskAuthorShouldSeeEditWarningWhenAlsoAssignedToTask() throws Exception {
        Task authoredTask = taskService.create(
                team.getId(),
                secondUser.getId(),
                secondUser.getId(),
                "Self-assigned task",
                "Task description",
                DEADLINE_DATE,
                kinopoiskTag.getId());

        mockMvc.perform(get("/tasks/" + authoredTask.getId()).with(user(new AuthUser(secondUser))))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string(containsString(
                                "data-tooltip=\"После смены автора вы не сможете редактировать задачу\"")));
    }

    @Test
    void taskAssigneeShouldSeeStatusOnlyCardEditorAndReadOnlyParameterTooltips() throws Exception {
        mockMvc.perform(get("/tasks").with(user(new AuthUser(secondUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-task-edit-open")))
                .andExpect(content().string(containsString("action=\"/tasks/" + task.getId() + "/status\"")))
                .andExpect(content().string(containsString("name=\"status\"")))
                .andExpect(content().string(not(containsString("name=\"authorId\""))))
                .andExpect(content().string(not(containsString("name=\"assigneeId\""))));

        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(secondUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-task-edit-open")))
                .andExpect(content().string(containsString("action=\"/tasks/" + task.getId() + "/status\"")));

        mockMvc.perform(get("/tasks/" + task.getId()).with(user(new AuthUser(secondUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Редактирование доступно только автору задачи")));
    }

    @Test
    void taskPageTitleEditShouldRedirectBackToTask() throws Exception {
        mockMvc.perform(post("/tasks/" + task.getId() + "/edit")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("title", "Task page title")
                        .param("status", TaskStatus.IN_PROGRESS.name())
                        .param("assigneeId", owner.getId().toString())
                        .param("deadlineDate", LocalDate.of(2035, 2, 10).toString())
                        .param("tagId", plusTag.getId().toString())
                        .param("returnTo", "task"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/" + task.getId()));

        Task updatedTask = taskService.findById(task.getId());

        assertEquals("Task page title", updatedTask.getTitle());
    }

    @Test
    void createTaskShouldRequireDeadline() throws Exception {
        mockMvc.perform(post("/tasks")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("teamId", team.getId().toString())
                        .param("assigneeId", owner.getId().toString())
                        .param("title", "Task without deadline")
                        .param("description", "Description")
                        .param("tagId", kinopoiskTag.getId().toString()))
                .andExpect(status().isBadRequest());

        assertEquals(1, dsl.fetchCount(TASKS));
    }

    @Test
    void removedMemberShouldNotOpenTeamOrTask() throws Exception {
        teamMemberService.removeMember(team.getId(), secondUser.getId(), owner.getId());

        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(secondUser))))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("class=\"error-page-code\""))))
                .andExpect(content().string(containsString("Такая страница не найдена")))
                .andExpect(content().string(not(containsString("Important task"))));

        mockMvc.perform(get("/tasks/" + task.getId()).with(user(new AuthUser(secondUser))))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("class=\"error-page\"")))
                .andExpect(content().string(not(containsString("class=\"error-page-code\""))))
                .andExpect(content().string(containsString("Такая страница не найдена")))
                .andExpect(content().string(not(containsString("Important task"))));
    }

    @Test
    void removedMemberShouldNotCreateTask() throws Exception {
        teamMemberService.removeMember(team.getId(), secondUser.getId(), owner.getId());

        mockMvc.perform(post("/tasks")
                        .with(csrf())
                        .with(user(new AuthUser(secondUser)))
                        .param("teamId", team.getId().toString())
                        .param("assigneeId", secondUser.getId().toString())
                        .param("title", "Created by removed member")
                        .param("description", "Description")
                        .param("deadlineDate", DEADLINE_DATE.toString())
                        .param("tagId", kinopoiskTag.getId().toString()))
                .andExpect(status().isNotFound());

        assertEquals(1, dsl.fetchCount(TASKS));
        assertThrows(
                TeamMemberNotFoundException.class,
                () -> taskService.create(
                        team.getId(),
                        secondUser.getId(),
                        owner.getId(),
                        "Created through service",
                        "Description",
                        DEADLINE_DATE,
                        kinopoiskTag.getId()));
    }

    @Test
    void removedMemberShouldNotUpdateTaskFields() throws Exception {
        teamMemberService.removeMember(team.getId(), secondUser.getId(), owner.getId());

        mockMvc.perform(post("/tasks/" + task.getId() + "/status")
                        .with(csrf())
                        .with(user(new AuthUser(secondUser)))
                        .param("status", TaskStatus.IN_PROGRESS.name())
                        .param("returnTo", "task"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/tasks/" + task.getId() + "/author")
                        .with(csrf())
                        .with(user(new AuthUser(secondUser)))
                        .param("authorId", owner.getId().toString())
                        .param("returnTo", "task"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/tasks/" + task.getId() + "/assignee")
                        .with(csrf())
                        .with(user(new AuthUser(secondUser)))
                        .param("assigneeId", owner.getId().toString())
                        .param("returnTo", "task"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/tasks/" + task.getId() + "/deadline")
                        .with(csrf())
                        .with(user(new AuthUser(secondUser)))
                        .param("deadlineDate", DEADLINE_DATE.toString())
                        .param("returnTo", "task"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/tasks/" + task.getId() + "/tag")
                        .with(csrf())
                        .with(user(new AuthUser(secondUser)))
                        .param("tagId", plusTag.getId().toString())
                        .param("returnTo", "task"))
                .andExpect(status().isNotFound());

        Task unchangedTask = taskService.findById(task.getId());

        assertEquals(TaskStatus.OPEN, unchangedTask.getStatus());
        assertEquals(owner.getId(), unchangedTask.getAuthorId());
        assertEquals(secondUser.getId(), unchangedTask.getAssigneeId());
        assertEquals(kinopoiskTag.getId(), unchangedTask.getTagId());
    }

    @Test
    void removedMemberShouldNotArchiveOrRestoreTask() throws Exception {
        taskService.updateStatus(task.getId(), TaskStatus.DONE, owner.getId());
        teamMemberService.removeMember(team.getId(), secondUser.getId(), owner.getId());

        mockMvc.perform(post("/tasks/" + task.getId() + "/archive")
                        .with(csrf())
                        .with(user(new AuthUser(secondUser)))
                        .param("returnTo", "task"))
                .andExpect(status().isNotFound());

        assertFalse(taskService.findById(task.getId()).isArchived());

        taskService.archive(task.getId(), owner.getId());

        mockMvc.perform(post("/tasks/" + task.getId() + "/restore")
                        .with(csrf())
                        .with(user(new AuthUser(secondUser)))
                        .param("returnTo", "task"))
                .andExpect(status().isNotFound());

        assertTrue(taskService.findById(task.getId()).isArchived());
    }

    @Test
    void onlyArchiverOrTeamOwnerShouldRestoreTask() {
        Task memberTask = taskService.create(
                team.getId(),
                secondUser.getId(),
                owner.getId(),
                "Member task",
                "Description",
                DEADLINE_DATE,
                kinopoiskTag.getId());

        taskService.archive(memberTask.getId(), owner.getId());

        assertThrows(
                AccessDeniedForTaskException.class,
                () -> taskService.restoreFromArchive(memberTask.getId(), secondUser.getId()));

        taskService.restoreFromArchive(memberTask.getId(), owner.getId());
        taskService.archive(memberTask.getId(), secondUser.getId());
        taskService.restoreFromArchive(memberTask.getId(), secondUser.getId());

        assertFalse(taskService.findById(memberTask.getId()).isArchived());
    }

    @Test
    void removedMemberShouldNotCreateEditOrDeleteComments() throws Exception {
        Comment comment = commentService.create(task.getId(), secondUser.getId(), "Before removal");

        teamMemberService.removeMember(team.getId(), secondUser.getId(), owner.getId());

        mockMvc.perform(post("/tasks/" + task.getId() + "/comments")
                        .with(csrf())
                        .with(user(new AuthUser(secondUser)))
                        .param("message", "After removal"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/tasks/" + task.getId() + "/comments/" + comment.getId() + "/edit")
                        .with(csrf())
                        .with(user(new AuthUser(secondUser)))
                        .param("message", "Edited after removal"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/tasks/" + task.getId() + "/comments/" + comment.getId() + "/delete")
                        .with(csrf())
                        .with(user(new AuthUser(secondUser))))
                .andExpect(status().isNotFound());

        assertEquals(
                "Before removal", commentRepository.findById(comment.getId()).getMessage());
        assertFalse(commentRepository.findById(comment.getId()).isDeleted());
        assertThrows(
                TeamMemberNotFoundException.class,
                () -> commentService.create(task.getId(), secondUser.getId(), "Service create"));
        assertThrows(
                TeamMemberNotFoundException.class,
                () -> commentService.updateMessage(comment.getId(), "Service edit", secondUser.getId()));
        assertThrows(
                TeamMemberNotFoundException.class,
                () -> commentService.deleteById(comment.getId(), secondUser.getId()));
    }

    @Test
    void removingMemberShouldKeepExistingTaskLinks() {
        teamMemberService.removeMember(team.getId(), secondUser.getId(), owner.getId());

        Task unchangedTask = taskService.findById(task.getId());

        assertEquals(owner.getId(), unchangedTask.getAuthorId());
        assertEquals(secondUser.getId(), unchangedTask.getAssigneeId());
    }

    @Test
    void ownerShouldSeeAuthorAndAssigneeSelectorsWhenOnlyOwnerRemains() throws Exception {
        teamMemberService.removeMember(team.getId(), secondUser.getId(), owner.getId());

        mockMvc.perform(get("/tasks/" + task.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/tasks/" + task.getId() + "/author\"")))
                .andExpect(content().string(containsString("name=\"authorId\"")))
                .andExpect(content().string(containsString("action=\"/tasks/" + task.getId() + "/assignee\"")))
                .andExpect(content().string(containsString("name=\"assigneeId\"")))
                .andExpect(content().string(containsString("Owner")));
    }

    @Test
    void formerAssigneeShouldBeMarkedAndExcludedFromNewTaskAssignees() throws Exception {
        teamMemberService.removeMember(team.getId(), secondUser.getId(), owner.getId());

        mockMvc.perform(get("/tasks/" + task.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Second")))
                .andExpect(content().string(containsString("task-card-former-member")))
                .andExpect(content().string(containsString("Пользователь был удалён из команды")));

        mockMvc.perform(get("/tasks/new?teamId=" + team.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Second"))))
                .andExpect(content().string(containsString("Owner")));
    }

    @Test
    void ownerShouldReplaceFormerAuthor() throws Exception {
        Task authorTask = taskService.create(
                team.getId(),
                secondUser.getId(),
                owner.getId(),
                "Former author task",
                "Description",
                DEADLINE_DATE,
                kinopoiskTag.getId());

        teamMemberService.removeMember(team.getId(), secondUser.getId(), owner.getId());

        mockMvc.perform(post("/tasks/" + authorTask.getId() + "/author")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("authorId", owner.getId().toString())
                        .param("returnTo", "task"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/" + authorTask.getId()));

        assertEquals(owner.getId(), taskService.findById(authorTask.getId()).getAuthorId());
    }

    @Test
    void activeAuthorShouldReplaceFormerAssignee() throws Exception {
        User activeAuthor = userService.create("active-author@test.com", "Active author", "qwerty");
        teamMemberService.addMember(team.getId(), activeAuthor.getId());

        Task assigneeTask = taskService.create(
                team.getId(),
                activeAuthor.getId(),
                secondUser.getId(),
                "Former assignee task",
                "Description",
                DEADLINE_DATE,
                kinopoiskTag.getId());

        teamMemberService.removeMember(team.getId(), secondUser.getId(), owner.getId());

        mockMvc.perform(post("/tasks/" + assigneeTask.getId() + "/assignee")
                        .with(csrf())
                        .with(user(new AuthUser(activeAuthor)))
                        .param("assigneeId", owner.getId().toString())
                        .param("returnTo", "task"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/" + assigneeTask.getId()));

        assertEquals(owner.getId(), taskService.findById(assigneeTask.getId()).getAssigneeId());
    }

    @Test
    void restoredMemberShouldNotBeMarkedAsFormer() throws Exception {
        teamMemberService.removeMember(team.getId(), secondUser.getId(), owner.getId());
        teamMemberService.addMember(team.getId(), secondUser.getId());

        mockMvc.perform(get("/tasks/" + task.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Second")))
                .andExpect(content().string(not(containsString("Пользователь был удалён из команды"))));
    }

    private void cleanDatabase() {
        dsl.deleteFrom(COMMENTS).execute();
        dsl.deleteFrom(TASKS).execute();
        dsl.deleteFrom(TEAM_INVITATIONS).execute();
        dsl.deleteFrom(TEAM_TAGS).execute();
        dsl.deleteFrom(TEAM_MEMBERS).execute();
        dsl.deleteFrom(TEAMS).execute();
        dsl.deleteFrom(USERS).execute();
    }
}
