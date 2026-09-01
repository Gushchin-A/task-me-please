package dev.gushchin.taskmanager.controller;

import static dev.gushchin.taskmanager.jooq.Tables.COMMENTS;
import static dev.gushchin.taskmanager.jooq.Tables.NOTIFICATION_EVENTS;
import static dev.gushchin.taskmanager.jooq.Tables.TASKS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAMS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_INVITATIONS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_MEMBERS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_TAGS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static dev.gushchin.taskmanager.jooq.Tables.USER_NOTIFICATIONS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskStatus;
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
class MyTasksPageControllerIntegrationTest extends IntegrationTestBase {
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
    private User secondUser;
    private Team firstTeam;
    private Team secondTeam;
    private TeamTag firstTeamTag;
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

        firstTeamTag = teamTagService.create(firstTeam.getId(), "Кинопоиск");
        TeamTag secondTeamTag = teamTagService.create(secondTeam.getId(), "Плюс");

        taskService.create(
                firstTeam.getId(),
                owner.getId(),
                secondUser.getId(),
                "Owner author task",
                "Description",
                LocalDate.of(2035, 1, 20),
                firstTeamTag.getId());

        assigneeTask = taskService.create(
                secondTeam.getId(),
                secondUser.getId(),
                owner.getId(),
                "Owner assignee task",
                "Description",
                LocalDate.of(2035, 2, 20),
                secondTeamTag.getId());

        taskService.updateStatus(assigneeTask.getId(), TaskStatus.DONE, secondUser.getId());
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
                .andExpect(content().string(containsString("TASKMEPLEASE")))
                .andExpect(content().string(containsString("class=\"app-header\"")))
                .andExpect(content().string(containsString("href=\"#main-content\"")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(not(containsString("class=\"app-sidebar\""))))
                .andExpect(content().string(containsString("class=\"app-content app-content-wide\"")))
                .andExpect(content().string(not(containsString("<h1>Мои задачи</h1>"))))
                .andExpect(content().string(not(containsString("class=\"page-actions\""))))
                .andExpect(content().string(containsString("class=\"task-toolbar\"")))
                .andExpect(content().string(containsString("Выберите фильтры")))
                .andExpect(content().string(not(containsString(">Исполнитель</span>"))))
                .andExpect(content().string(containsString(">Автор</span>")))
                .andExpect(content().string(containsString(">Тег</span>")))
                .andExpect(content().string(containsString(">Команда</span>")))
                .andExpect(content().string(containsString(">Моя роль</span>")))
                .andExpect(content().string(containsString(">Автор</a>")))
                .andExpect(content().string(containsString(">Исполнитель</a>")))
                .andExpect(content().string(containsString("Сортировать задачи")))
                .andExpect(content().string(containsString("Сначала новые")))
                .andExpect(content().string(containsString("Сначала старые")))
                .andExpect(content().string(containsString("Ближайший дедлайн")))
                .andExpect(content().string(containsString("Поздний дедлайн")))
                .andExpect(content().string(containsString("class=\"button button-primary task-create-action\"")))
                .andExpect(content().string(containsString("class=\"toolbar-popover task-filter-popover\"")))
                .andExpect(content().string(containsString("class=\"task-grid\"")))
                .andExpect(content().string(containsString("class=\"task-card task-card-status-")))
                .andExpect(content().string(containsString("class=\"task-card-header\"")))
                .andExpect(content().string(containsString("<details class=\"task-card-actions\">")))
                .andExpect(content().string(containsString("class=\"task-card-footer\"")))
                .andExpect(content().string(not(containsString("<time datetime="))))
                .andExpect(content().string(containsString("Открыть задачу")))
                .andExpect(content().string(containsString("Скопировать ссылку")))
                .andExpect(content().string(containsString("class=\"task-card-actions-divider\"")))
                .andExpect(content().string(containsString("Изменить задачу")))
                .andExpect(content().string(containsString("Удалить задачу")))
                .andExpect(content().string(containsString("Задача будет перемещена в архив")))
                .andExpect(content().string(containsString("Изменение задачи")))
                .andExpect(content().string(containsString("Название задачи")))
                .andExpect(content().string(containsString("/ 100 символов")))
                .andExpect(content().string(containsString("data-task-edit-select")))
                .andExpect(content().string(containsString("data-selected-avatar")))
                .andExpect(content().string(containsString("Выберите исполнителя")))
                .andExpect(content().string(containsString("Для редактирования описания откройте")))
                .andExpect(content().string(containsString("страницу задачи")))
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
    void myTasksPageShouldFilterByAuthorAssigneeAndTag() throws Exception {
        mockMvc.perform(get("/tasks?authorId=" + owner.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Owner author task")))
                .andExpect(content().string(not(containsString("Owner assignee task"))));

        mockMvc.perform(get("/tasks?assigneeId=" + owner.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Owner assignee task")))
                .andExpect(content().string(not(containsString("Owner author task"))));

        mockMvc.perform(get("/tasks?tagId=" + firstTeamTag.getId()).with(user(new AuthUser(owner))))
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
    void myTasksPageShouldOpenWithOldestFirstSort() throws Exception {
        mockMvc.perform(get("/tasks?sort=OLDEST").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("First Team")))
                .andExpect(content().string(containsString("Second Team")))
                .andExpect(content().string(containsString("Сначала старые")));
    }

    @Test
    void taskPagesWithoutHistoryShouldHideWorkspaceNavigation() throws Exception {
        deleteNotifications();
        dsl.deleteFrom(TASKS).execute();

        mockMvc.perform(get("/tasks").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"empty-state task-empty-state\"")))
                .andExpect(content().string(containsString("class=\"empty-state-icon\"")))
                .andExpect(content().string(not(containsString("class=\"local-tabs\""))))
                .andExpect(content().string(not(containsString("class=\"task-count\""))))
                .andExpect(content().string(not(containsString("class=\"toolbar-popover\""))))
                .andExpect(content().string(containsString("class=\"button button-primary task-create-action\"")))
                .andExpect(content().string(containsString("Задачи не найдены")));

        mockMvc.perform(get("/tasks/archive").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"empty-state task-empty-state\"")))
                .andExpect(content().string(containsString("class=\"empty-state-icon\"")))
                .andExpect(content().string(not(containsString("class=\"local-tabs\""))))
                .andExpect(content().string(not(containsString("class=\"task-count\""))))
                .andExpect(content().string(not(containsString("class=\"toolbar-popover\""))))
                .andExpect(content().string(containsString("class=\"button button-primary task-create-action\"")))
                .andExpect(content().string(containsString("В архиве пока что пусто")))
                .andExpect(content()
                        .string(containsString(
                                "Сюда вы сможете перенести выполненные, неактуальные или удаленные задачи")));
    }

    @Test
    void taskPagesWithoutTeamsShouldOfferTeamCreation() throws Exception {
        deleteNotifications();
        dsl.deleteFrom(TASKS).execute();
        dsl.deleteFrom(TEAM_INVITATIONS).execute();
        dsl.deleteFrom(TEAM_TAGS).execute();
        dsl.deleteFrom(TEAM_MEMBERS).execute();
        dsl.deleteFrom(TEAMS).execute();

        mockMvc.perform(get("/tasks").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("class=\"local-tabs\""))))
                .andExpect(content().string(not(containsString("class=\"task-count\""))))
                .andExpect(content().string(not(containsString("class=\"toolbar-popover\""))))
                .andExpect(content().string(containsString("class=\"button button-primary team-create-action\"")))
                .andExpect(content().string(containsString("href=\"/teams/new\"")))
                .andExpect(content().string(containsString("class=\"empty-state-icons\"")))
                .andExpect(content().string(containsString("class=\"empty-state-group-icon\"")))
                .andExpect(content().string(containsString("Актуальные задачи и команды не найдены")))
                .andExpect(content().string(containsString("Чтобы работать с задачами, создайте команду")))
                .andExpect(content().string(containsString("попросите добавить вас в существующую")))
                .andExpect(content().string(not(containsString("добавить вас в существующую команду"))));
    }

    @Test
    void archiveWithoutTasksShouldKeepWorkspaceNavigationWhenTaskHistoryExists() throws Exception {
        mockMvc.perform(get("/tasks/archive").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"local-tabs\"")))
                .andExpect(content().string(containsString("class=\"task-count\">Задачи (0)")))
                .andExpect(content().string(containsString("class=\"toolbar-popover task-filter-popover\"")))
                .andExpect(content().string(containsString(">Моя роль</span>")))
                .andExpect(content().string(containsString("href=\"/tasks/archive?role=AUTHOR\"")))
                .andExpect(content().string(containsString("href=\"/tasks/archive?role=ASSIGNEE\"")))
                .andExpect(content().string(containsString("class=\"button button-primary task-create-action\"")))
                .andExpect(content().string(containsString("В архиве пока что пусто")))
                .andExpect(content()
                        .string(containsString(
                                "Сюда вы сможете перенести выполненные, неактуальные или удаленные задачи")));
    }

    @Test
    void archivedTaskShouldNotHighlightOverdueDeadline() throws Exception {
        taskService.updateDeadline(assigneeTask.getId(), LocalDate.now().minusDays(1), secondUser.getId());
        taskService.archive(assigneeTask.getId(), secondUser.getId());

        mockMvc.perform(get("/tasks/archive").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Owner assignee task")))
                .andExpect(content().string(containsString("task-card-archived")))
                .andExpect(content().string(containsString("task-card-status-not-relevant")))
                .andExpect(content().string(not(containsString("task-card-status-done"))))
                .andExpect(content().string(containsString("task-card-archive-event-resolved")))
                .andExpect(content().string(containsString("task-card-footer-archive-with-team")))
                .andExpect(content().string(containsString("Решена")))
                .andExpect(content().string(not(containsString("<time"))))
                .andExpect(content().string(not(containsString("task-card-deadline-urgent"))));
    }

    @Test
    void removedMemberShouldNotSeeRemovedTeamInTeamsPage() throws Exception {
        teamMemberService.removeMember(firstTeam.getId(), secondUser.getId(), owner.getId());

        mockMvc.perform(get("/teams").with(user(new AuthUser(secondUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("First Team"))))
                .andExpect(content().string(containsString("Second Team")));
    }

    @Test
    void teamsPageShouldUseTeamList() throws Exception {
        mockMvc.perform(get("/teams").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"workspace-page teams-page\"")))
                .andExpect(content().string(containsString("class=\"teams-toolbar\"")))
                .andExpect(content().string(containsString("Команды (2)")))
                .andExpect(content().string(containsString("class=\"team-list\"")))
                .andExpect(content().string(containsString("class=\"team-list-item\"")))
                .andExpect(content().string(containsString("class=\"team-list-name\"")))
                .andExpect(content().string(containsString("Всего задач: 1")))
                .andExpect(content().string(containsString("Владелец команды")))
                .andExpect(content().string(not(containsString("team-list-mark"))))
                .andExpect(content().string(not(containsString("team-list-arrow"))))
                .andExpect(content().string(not(containsString("data-tooltip=\"Открыть команду\""))));
    }

    @Test
    void teamsPageShouldShowRelevantTaskCountForMember() throws Exception {
        mockMvc.perform(get("/teams").with(user(new AuthUser(secondUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Актуальные задачи: 1")))
                .andExpect(content().string(containsString("Участник команды")))
                .andExpect(content().string(not(containsString("Всего задач:"))));
    }

    @Test
    void emptyTeamsPageShouldUseSharedBlankSlate() throws Exception {
        deleteNotifications();
        dsl.deleteFrom(TASKS).execute();
        dsl.deleteFrom(TEAM_INVITATIONS).execute();
        dsl.deleteFrom(TEAM_TAGS).execute();
        dsl.deleteFrom(TEAM_MEMBERS).execute();
        dsl.deleteFrom(TEAMS).execute();

        mockMvc.perform(get("/teams").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"teams-toolbar teams-toolbar-empty\"")))
                .andExpect(content().string(containsString("class=\"button button-primary team-create-action\"")))
                .andExpect(content().string(containsString("class=\"empty-state team-empty-state\"")))
                .andExpect(content().string(containsString("class=\"empty-state-icon\"")))
                .andExpect(content().string(containsString("Команд пока нет")))
                .andExpect(content().string(not(containsString("Команды (0)"))))
                .andExpect(content()
                        .string(containsString("Создайте команду, чтобы распределять задачи и работать вместе")));
    }

    @Test
    void removedMemberShouldNotSeeRemovedTeamTasksInMyTasksPage() throws Exception {
        teamMemberService.removeMember(firstTeam.getId(), secondUser.getId(), owner.getId());

        mockMvc.perform(get("/tasks").with(user(new AuthUser(secondUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Owner author task"))))
                .andExpect(content().string(containsString("Owner assignee task")));
    }

    private void cleanDatabase() {
        deleteNotifications();
        dsl.deleteFrom(COMMENTS).execute();
        dsl.deleteFrom(TASKS).execute();
        dsl.deleteFrom(TEAM_INVITATIONS).execute();
        dsl.deleteFrom(TEAM_TAGS).execute();
        dsl.deleteFrom(TEAM_MEMBERS).execute();
        dsl.deleteFrom(TEAMS).execute();
        dsl.deleteFrom(USERS).execute();
    }

    private void deleteNotifications() {
        dsl.deleteFrom(USER_NOTIFICATIONS).execute();
        dsl.deleteFrom(NOTIFICATION_EVENTS).execute();
    }
}
