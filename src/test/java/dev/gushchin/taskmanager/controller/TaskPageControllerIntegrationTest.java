package dev.gushchin.taskmanager.controller;

import static dev.gushchin.taskmanager.jooq.Tables.COMMENTS;
import static dev.gushchin.taskmanager.jooq.Tables.TASKS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAMS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_INVITATIONS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_MEMBERS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.model.Comment;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskCategory;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.CommentRepository;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.TaskService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
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
    private CommentRepository commentRepository;

    private User owner;
    private User secondUser;
    private Team team;
    private Task task;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        owner = userService.create("owner@test.com", "Owner", "qwerty");
        secondUser = userService.create("second@test.com", "Second", "qwerty");

        team = teamService.create("Project Team", owner.getId());
        teamMemberService.addMember(team.getId(), secondUser.getId());

        task = taskService.create(
                team.getId(),
                owner.getId(),
                secondUser.getId(),
                "Important task",
                "Task description",
                DEADLINE_DATE,
                TaskCategory.KINOPOISK);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void showTaskPageReturnOkAndContent() throws Exception {
        // given
        taskService.updateStatus(task.getId(), TaskStatus.DONE);
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
        mockMvc.perform(post("/tasks/" + taskId + "/category")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("category", TaskCategory.PLUS.name())
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
                .andExpect(content().string(containsString(TaskCategory.PLUS.name())));
    }

    @Test
    void commentsAndArchiveShouldWork() throws Exception {
        // given
        Long taskId = task.getId();
        taskService.updateStatus(taskId, TaskStatus.DONE);

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

    private void cleanDatabase() {
        dsl.deleteFrom(COMMENTS).execute();
        dsl.deleteFrom(TASKS).execute();
        dsl.deleteFrom(TEAM_INVITATIONS).execute();
        dsl.deleteFrom(TEAM_MEMBERS).execute();
        dsl.deleteFrom(TEAMS).execute();
        dsl.deleteFrom(USERS).execute();
    }
}
