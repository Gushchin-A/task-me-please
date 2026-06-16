package dev.gushchin.taskmanager.controller;

import static dev.gushchin.taskmanager.jooq.Tables.COMMENTS;
import static dev.gushchin.taskmanager.jooq.Tables.TASKS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAMS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_INVITATIONS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_MEMBERS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskCategory;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.TaskService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.UserService;
import java.time.LocalDate;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
public class MyTasksPageControllerIntegrationTest extends IntegrationTestBase {
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

    private User owner;
    private User secondUser;
    private Team firstTeam;
    private Team secondTeam;
    private Task authorTask;
    private Task assigneeTask;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        owner = userService.create("owner-my-tasks@test.com", "Owner", "qwerty");
        secondUser = userService.create("second-my-tasks@test.com", "Second", "qwerty");

        firstTeam = teamService.create("First Team", owner.getId());
        secondTeam = teamService.create("Second Team", owner.getId());

        teamMemberService.addMember(firstTeam.getId(), secondUser.getId());
        teamMemberService.addMember(secondTeam.getId(), secondUser.getId());

        authorTask = taskService.create(
                firstTeam.getId(),
                owner.getId(),
                secondUser.getId(),
                "Owner author task",
                "Description",
                LocalDate.of(2035, 1, 20),
                TaskCategory.KINOPOISK);

        assigneeTask = taskService.create(
                secondTeam.getId(),
                secondUser.getId(),
                owner.getId(),
                "Owner assignee task",
                "Description",
                LocalDate.of(2035, 2, 20),
                TaskCategory.PLUS);

        taskService.updateStatus(assigneeTask.getId(), TaskStatus.DONE);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void myTasksPageShouldShowTasksFromDifferentTeams() throws Exception {
        mockMvc.perform(get("/tasks").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Мои задачи")))
                .andExpect(content().string(containsString("Owner author task")))
                .andExpect(content().string(containsString("Owner assignee task")))
                .andExpect(content().string(containsString("First Team")))
                .andExpect(content().string(containsString("Second Team")));
    }

    @Test
    void myTasksPageShouldFilterByStatus() throws Exception {
        mockMvc.perform(get("/tasks?status=DONE").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Owner assignee task")))
                .andExpect(content().string(not(containsString("Owner author task"))));
    }

    @Test
    void myTasksPageShouldFilterByTeam() throws Exception {
        mockMvc.perform(get("/tasks?teamId=" + firstTeam.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Owner author task")))
                .andExpect(content().string(not(containsString("Owner assignee task"))));
    }

    @Test
    void myTasksPageShouldFilterByAuthorRole() throws Exception {
        mockMvc.perform(get("/tasks?role=AUTHOR").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Owner author task")))
                .andExpect(content().string(not(containsString("Owner assignee task"))));
    }

    @Test
    void myTasksPageShouldFilterByAssigneeRole() throws Exception {
        mockMvc.perform(get("/tasks?role=ASSIGNEE").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Owner assignee task")))
                .andExpect(content().string(not(containsString("Owner author task"))));
    }

    @Test
    void myTasksPageShouldOpenWithSortByTeam() throws Exception {
        mockMvc.perform(get("/tasks?sort=TEAM").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("First Team")))
                .andExpect(content().string(containsString("Second Team")))
                .andExpect(content().string(containsString("Команда")));
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
