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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.gushchin.taskmanager.IntegrationTestBase;
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

        commentRepository.save(comment);

        // when
        mockMvc.perform(get("/tasks/" + task.getId()).with(user(new AuthUser(owner))))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Important task")))
                .andExpect(content().string(containsString("Task description")))
                .andExpect(content().string(containsString("Архив")))
                .andExpect(content().string(containsString("Вернуть из архива")))
                .andExpect(content().string(containsString("20 января 2035")))
                .andExpect(content().string(containsString("Кинопоиск")))
                .andExpect(content().string(containsString("Initial comment")))
                .andExpect(content().string(containsString("15 июня 2026 18:17")));
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
                        .param("returnTo", "task"))
                // then
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/" + taskId));

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
                .andExpect(redirectedUrl("/tasks/" + taskId));

        // when
        mockMvc.perform(get("/tasks/" + taskId).with(user(new AuthUser(owner))))
                // then
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Перенести в архив")));
    }

    @Test
    void removedMemberShouldNotOpenTeamOrTask() throws Exception {
        teamMemberService.removeMember(team.getId(), secondUser.getId(), owner.getId());

        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(secondUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Такая страница не найдена")))
                .andExpect(content().string(not(containsString("Important task"))));

        mockMvc.perform(get("/tasks/" + task.getId()).with(user(new AuthUser(secondUser))))
                .andExpect(status().isNotFound());
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
                .andExpect(content().string(containsString("color: red;")))
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
