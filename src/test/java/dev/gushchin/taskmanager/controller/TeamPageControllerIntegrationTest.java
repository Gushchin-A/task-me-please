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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamTag;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.TaskService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.TeamTagService;
import dev.gushchin.taskmanager.service.UserService;
import java.time.LocalDate;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class TeamPageControllerIntegrationTest extends IntegrationTestBase {
    private static final LocalDate DEADLINE_DATE = LocalDate.of(2035, 1, 20);

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

    private User owner;
    private User member;
    private Team team;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        owner = userService.create("team-owner@test.com", "Owner", "qwerty");

        member = userService.create("team-member@test.com", "Member", "qwerty");

        team = teamService.create("Project Team", owner.getId());

        teamMemberService.addMember(team.getId(), member.getId());

        TeamTag kinopoiskTag = teamTagService.create(team.getId(), "Кинопоиск");
        TeamTag plusTag = teamTagService.create(team.getId(), "Плюс");

        taskService.create(
                team.getId(),
                owner.getId(),
                member.getId(),
                "Visible task",
                "Visible to member",
                DEADLINE_DATE,
                kinopoiskTag.getId());

        taskService.create(
                team.getId(),
                owner.getId(),
                owner.getId(),
                "Hidden task",
                "Not visible to member",
                DEADLINE_DATE,
                plusTag.getId());
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void membersPageShouldShowOnlyVisibleTasksCount() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId() + "/members").with(user(new AuthUser(member))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Задачи (1)")))
                .andExpect(content().string(not(containsString("Задачи (2)"))));
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
