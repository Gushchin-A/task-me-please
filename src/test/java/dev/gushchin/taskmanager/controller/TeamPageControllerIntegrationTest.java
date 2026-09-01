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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.exception.InvitationEmailSendingException;
import dev.gushchin.taskmanager.exception.TeamMemberNotFoundException;
import dev.gushchin.taskmanager.exception.TeamNotFoundException;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamTag;
import dev.gushchin.taskmanager.model.TeamTaskVisibility;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamInvitationRepository;
import dev.gushchin.taskmanager.repository.TeamMemberRepository;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.CommentService;
import dev.gushchin.taskmanager.service.InvitationEmailService;
import dev.gushchin.taskmanager.service.TaskService;
import dev.gushchin.taskmanager.service.TeamInvitationService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.TeamTagService;
import dev.gushchin.taskmanager.service.UserService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

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
    private CommentService commentService;

    @Autowired
    private TeamTagService teamTagService;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private TeamInvitationService teamInvitationService;

    @Autowired
    private TeamInvitationRepository teamInvitationRepository;

    @MockitoBean
    private InvitationEmailService invitationEmailService;

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
    void newTeamPageShouldShowFlatPrimerForm() throws Exception {
        mockMvc.perform(get("/teams/new").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(view().name("teams/new"))
                .andExpect(content().string(containsString("Создание новой команды")))
                .andExpect(content().string(containsString("Вернуться назад")))
                .andExpect(content().string(containsString("class=\"team-create-form\"")))
                .andExpect(content().string(containsString("Длина одного тега до 30 символов.")))
                .andExpect(content().string(not(containsString("Обязательные поля отмечены"))))
                .andExpect(content().string(not(containsString("class=\"form-card\""))));
    }

    @Test
    void teamsPageShouldShowSuccessFlash() throws Exception {
        mockMvc.perform(get("/teams")
                        .with(user(new AuthUser(owner)))
                        .flashAttr("successMessage", "Команда успешно удалена"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-flash-message")))
                .andExpect(content().string(containsString("Команда успешно удалена")));
    }

    @Test
    void membersPageShouldUseSharedTeamNavigation() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId() + "/members").with(user(new AuthUser(member))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"local-tabs team-tabs\"")))
                .andExpect(content().string(containsString("aria-label=\"Разделы команды\"")))
                .andExpect(content().string(containsString("href=\"/teams/" + team.getId() + "/archive\"")))
                .andExpect(content().string(not(containsString("href=\"#\""))))
                .andExpect(content().string(containsString("Участники")))
                .andExpect(content().string(containsString("Покинуть команду")))
                .andExpect(content().string(not(containsString("Приглашения"))))
                .andExpect(content().string(not(containsString("class=\"nav-count\""))));
    }

    @Test
    void teamNavigationShouldRemainStableBetweenActiveAndArchivePages() throws Exception {
        Task archivedTask = taskService.findByTeamId(team.getId()).getFirst();
        taskService.updateStatus(archivedTask.getId(), TaskStatus.DONE, owner.getId());
        taskService.archive(archivedTask.getId(), owner.getId());

        mockMvc.perform(get("/teams/" + team.getId())
                        .with(user(new AuthUser(owner)))
                        .flashAttr("successMessage", "Задача была перенесена в архив"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-flash-message")))
                .andExpect(content().string(containsString("Задача была перенесена в архив")))
                .andExpect(content().string(containsString("class=\"local-tabs team-tabs\"")))
                .andExpect(content().string(containsString("class=\"local-tab-active\"")))
                .andExpect(content().string(containsString("class=\"task-count\">Задачи (1)")))
                .andExpect(content().string(not(containsString("class=\"nav-count\""))));

        mockMvc.perform(get("/teams/" + team.getId() + "/archive").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"local-tabs team-tabs\"")))
                .andExpect(content().string(containsString("class=\"local-tab-active\"")))
                .andExpect(content().string(containsString("class=\"task-count\">Задачи (1)")))
                .andExpect(content().string(not(containsString("class=\"nav-count\""))));
    }

    @Test
    void archivedTeamTaskShouldNotHighlightOverdueDeadline() throws Exception {
        Task archivedTask = taskService.findByTeamId(team.getId()).getFirst();
        taskService.updateDeadline(archivedTask.getId(), LocalDate.now().minusDays(1), owner.getId());
        taskService.archive(archivedTask.getId(), owner.getId());

        mockMvc.perform(get("/teams/" + team.getId() + "/archive").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(archivedTask.getTitle())))
                .andExpect(content().string(containsString("task-card-archived")))
                .andExpect(content().string(containsString("task-card-status-not-relevant")))
                .andExpect(content().string(not(containsString("task-card-status-open"))))
                .andExpect(content().string(containsString("task-card-archive-event-deleted")))
                .andExpect(content().string(containsString("Была удалена")))
                .andExpect(content().string(not(containsString("task-card-footer-archive-with-team"))))
                .andExpect(content().string(not(containsString("<time"))))
                .andExpect(content().string(not(containsString("task-card-deadline-urgent"))));
    }

    @Test
    void ownerShouldOpenTeamSettings() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId() + "/settings").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(view().name("teams/settings"))
                .andExpect(content().string(containsString("maxlength=\"100\"")))
                .andExpect(content().string(containsString("/ 100 символов")))
                .andExpect(content().string(containsString("Основные")))
                .andExpect(content().string(containsString("Название команды")))
                .andExpect(content().string(containsString("Длина одного тега до 30 символов.")))
                .andExpect(content().string(containsString("settings?section=delete")))
                .andExpect(content().string(not(containsString("class=\"team-settings-delete\""))))
                .andExpect(content().string(not(containsString("class=\"task-toolbar\""))));
    }

    @Test
    void ownerShouldOpenTeamDeletionSettings() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId() + "/settings")
                        .param("section", "delete")
                        .with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(view().name("teams/settings"))
                .andExpect(content().string(containsString("<h1>Удаление команды</h1>")))
                .andExpect(content().string(containsString("После удаления команды она и все ее задачи")))
                .andExpect(content().string(containsString("data-team-delete-dialog")))
                .andExpect(content().string(containsString("data-team-delete-open")))
                .andExpect(content()
                        .string(containsString("Вы уверены, что хотите удалить команду «" + team.getName() + "»?")))
                .andExpect(content()
                        .string(containsString(
                                "Если да, напишите <strong>«я хочу удалить команду»</strong> в поле ниже.")))
                .andExpect(content().string(containsString("Введите проверочный текст")))
                .andExpect(content().string(containsString("name=\"confirmationText\"")))
                .andExpect(content().string(containsString("data-team-delete-submit")))
                .andExpect(content().string(not(containsString("Название команды"))));
    }

    @Test
    void memberShouldOpenMembersAsDefaultTeamSettings() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId() + "/settings").with(user(new AuthUser(member))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/members"));
    }

    @Test
    void memberShouldOpenLeaveTeamSettings() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId() + "/settings")
                        .param("section", "leave")
                        .with(user(new AuthUser(member))))
                .andExpect(status().isOk())
                .andExpect(view().name("teams/settings"))
                .andExpect(content().string(containsString("<h1>Покинуть команду</h1>")))
                .andExpect(content().string(containsString("все ее задачи, включая ваши, будут недоступны")))
                .andExpect(content().string(containsString("data-team-leave-dialog")))
                .andExpect(content().string(containsString("data-team-leave-open")))
                .andExpect(content()
                        .string(containsString("Вы уверены, что хотите покинуть команду «" + team.getName() + "»?")))
                .andExpect(content().string(containsString("«да хочу выйти из команды»")))
                .andExpect(content().string(not(containsString("Основные настройки"))))
                .andExpect(content().string(not(containsString("Приглашения"))));
    }

    @Test
    void ownerShouldRenameTeam() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/settings/name")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("name", "Renamed Team"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/settings"))
                .andExpect(flash().attribute("successMessage", "Название успешно изменено"));

        assertEquals("Renamed Team", teamService.findById(team.getId()).getName());
    }

    @Test
    void ownerShouldNotRenameTeamToNameLongerThanOneHundredCharacters() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/settings/name")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("name", "a".repeat(101)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/settings"))
                .andExpect(flash().attribute("errorMessage", "Название команды не должно быть длиннее 100 символов"));

        assertEquals("Project Team", teamService.findById(team.getId()).getName());
    }

    @Test
    void createShouldRejectTeamNameLongerThanOneHundredCharacters() throws Exception {
        mockMvc.perform(post("/teams")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("name", "a".repeat(101)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/new"))
                .andExpect(flash().attribute("errorMessage", "Название команды не должно быть длиннее 100 символов"));
    }

    @Test
    void ownerShouldAddAndRenameTagFromSettings() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/settings/tags")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("name", "Новости"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/settings"))
                .andExpect(flash().attribute("successMessage", "Тег успешно добавлен"));

        TeamTag tag = teamTagService.findByTeamId(team.getId()).stream()
                .filter(teamTag -> "Новости".equals(teamTag.getName()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/teams/" + team.getId() + "/settings/tags/" + tag.getId() + "/rename")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("name", "События"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/settings"))
                .andExpect(flash().attribute("successMessage", "Тег успешно изменен"));

        assertEquals("События", teamTagService.findById(tag.getId()).getName());
    }

    @Test
    void teamPageShouldShowNavigationActionsAndTaskCards() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"local-tabs team-tabs\"")))
                .andExpect(content().string(containsString("class=\"app-team-context\"")))
                .andExpect(content().string(containsString("class=\"app-team-link\" href=\"/teams/" + team.getId())))
                .andExpect(content().string(containsString("data-tooltip=\"Сменить команду\"")))
                .andExpect(content().string(containsString("class=\"team-switcher-menu\"")))
                .andExpect(content().string(containsString("class=\"team-switcher-all\" href=\"/teams\"")))
                .andExpect(content().string(containsString("href=\"/teams/" + team.getId() + "/settings\"")))
                .andExpect(content().string(not(containsString("aria-label=\"Выбрать команду\""))))
                .andExpect(content().string(not(containsString("Пригласить"))))
                .andExpect(content().string(not(containsString("Удалить команду"))))
                .andExpect(content().string(not(containsString("Участники"))))
                .andExpect(content().string(containsString("class=\"primer-select-trigger task-toolbar-trigger\"")))
                .andExpect(content().string(containsString("class=\"primer-select-options task-filter-options\"")))
                .andExpect(content().string(containsString("Выберите фильтры")))
                .andExpect(content().string(containsString("aria-label=\"Закрыть фильтры\"")))
                .andExpect(content().string(containsString("aria-label=\"Закрыть сортировку\"")))
                .andExpect(content().string(containsString("data-filter-select")))
                .andExpect(content().string(not(containsString("Все статусы"))))
                .andExpect(content().string(not(containsString("Все исполнители"))))
                .andExpect(content().string(not(containsString("Все авторы"))))
                .andExpect(content().string(not(containsString("Все теги"))))
                .andExpect(content().string(not(containsString(">Применить</button>"))))
                .andExpect(content().string(containsString("Сначала новые")))
                .andExpect(content().string(containsString("Сначала старые")))
                .andExpect(content().string(containsString("Ближайший дедлайн")))
                .andExpect(content().string(containsString("Поздний дедлайн")))
                .andExpect(content().string(containsString("class=\"button button-primary task-create-action\"")))
                .andExpect(content().string(containsString("class=\"task-grid\"")))
                .andExpect(content().string(containsString("Visible task")))
                .andExpect(content().string(not(containsString("Всего задач в команде"))));
    }

    @Test
    void taskCardMenuShouldUseArchiveForDoneTaskAndDeleteForOtherStatuses() throws Exception {
        Task doneTask = taskService.findByTeamId(team.getId()).stream()
                .filter(task -> "Visible task".equals(task.getTitle()))
                .findFirst()
                .orElseThrow();
        taskService.updateStatus(doneTask.getId(), TaskStatus.DONE, owner.getId());

        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Перенести в архив")))
                .andExpect(content().string(containsString("Удалить задачу")));
    }

    @Test
    void teamWithoutTasksShouldShowSharedBlankSlateWithoutTaskActions() throws Exception {
        Team emptyTeam = teamService.create("Empty Team", owner.getId());

        mockMvc.perform(get("/teams/" + emptyTeam.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"empty-state task-empty-state\"")))
                .andExpect(content().string(containsString("class=\"empty-state-icon\"")))
                .andExpect(content().string(containsString("В команде пока нет задач")))
                .andExpect(content().string(containsString("Создайте первую задачу и назначьте ответственного.")))
                .andExpect(content().string(not(containsString("class=\"task-toolbar\""))))
                .andExpect(content().string(containsString("class=\"page-actions tasks-onboarding-actions\"")))
                .andExpect(content().string(containsString("class=\"button button-primary task-create-action\"")))
                .andExpect(content().string(containsString("href=\"/tasks/new?teamId=" + emptyTeam.getId() + "\"")));
    }

    @Test
    void memberWithoutVisibleTasksShouldSeePersonalTeamBlankSlate() throws Exception {
        Task visibleTask = taskService.findByTeamId(team.getId()).stream()
                .filter(task -> "Visible task".equals(task.getTitle()))
                .findFirst()
                .orElseThrow();
        taskService.updateAssignee(visibleTask.getId(), owner.getId(), owner.getId());

        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(member))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("В этой команде у вас пока нет задач")))
                .andExpect(content()
                        .string(containsString(
                                "Дождитесь, пока вам поставят задачу, или создайте свою и назначьте ответственного.")))
                .andExpect(content().string(not(containsString("В команде пока нет задач"))));
    }

    @Test
    void emptyTeamArchiveShouldShowSharedArchiveBlankSlate() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId() + "/archive").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"empty-state task-empty-state\"")))
                .andExpect(content().string(containsString("class=\"empty-state-icon\"")))
                .andExpect(content().string(containsString("В архиве пока что пусто")))
                .andExpect(content()
                        .string(containsString(
                                "Сюда вы сможете перенести выполненные, неактуальные или удаленные задачи")));
    }

    @Test
    void teamWithOnlyArchivedTasksShouldShowSharedActiveTasksBlankSlate() throws Exception {
        List<Task> tasks = taskService.findByTeamId(team.getId());

        for (Task task : tasks) {
            taskService.updateStatus(task.getId(), TaskStatus.DONE, owner.getId());
            taskService.archive(task.getId(), owner.getId());
        }

        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"task-toolbar\"")))
                .andExpect(content().string(containsString("class=\"empty-state task-empty-state\"")))
                .andExpect(content().string(containsString("class=\"empty-state-icon\"")))
                .andExpect(content().string(containsString("В команде пока нет задач")))
                .andExpect(content().string(containsString("Создайте первую задачу и назначьте ответственного.")));
    }

    @Test
    void ownerShouldDeleteTeamAfterConfirmationText() throws Exception {
        final TeamInvitation invitation =
                teamInvitationService.create(team.getId(), "delete-team@test.com", owner.getId());
        List<Task> tasks = taskService.findByTeamId(team.getId());
        commentService.create(tasks.getFirst().getId(), owner.getId(), "Historical comment");
        final int membersCount = dsl.fetchCount(TEAM_MEMBERS, TEAM_MEMBERS.TEAM_ID.eq(team.getId()));
        final int tasksCount = dsl.fetchCount(TASKS, TASKS.TEAM_ID.eq(team.getId()));
        final int tagsCount = dsl.fetchCount(TEAM_TAGS, TEAM_TAGS.TEAM_ID.eq(team.getId()));
        final int commentsCount = dsl.fetchCount(COMMENTS);
        mockMvc.perform(post("/teams/" + team.getId() + "/delete")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("confirmationText", "я хочу удалить команду"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams"))
                .andExpect(flash().attribute("successMessage", "Команда успешно удалена"));

        assertThrows(TeamNotFoundException.class, () -> teamService.findById(team.getId()));
        assertEquals(membersCount, dsl.fetchCount(TEAM_MEMBERS, TEAM_MEMBERS.TEAM_ID.eq(team.getId())));
        assertEquals(tasksCount, dsl.fetchCount(TASKS, TASKS.TEAM_ID.eq(team.getId())));
        assertEquals(tagsCount, dsl.fetchCount(TEAM_TAGS, TEAM_TAGS.TEAM_ID.eq(team.getId())));
        assertEquals(commentsCount, dsl.fetchCount(COMMENTS));
        assertEquals(
                invitation.getId(),
                dsl.selectFrom(TEAM_INVITATIONS)
                        .where(TEAM_INVITATIONS.TEAM_ID.eq(team.getId()))
                        .fetchOne()
                        .getId());
        assertFalse(tasks.isEmpty());

        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(member))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/tasks/" + tasks.getFirst().getId()).with(user(new AuthUser(member))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/invitations/" + invitation.getToken()).with(user(new AuthUser(owner))))
                .andExpect(status().isNotFound());
    }

    @Test
    void incorrectConfirmationTextShouldNotDeleteTeam() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/delete")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("confirmationText", "удалить команду"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/settings?section=delete"))
                .andExpect(flash().attribute("errorMessage", "Проверочный текст введен неверно"));

        assertFalse(teamService.findById(team.getId()).isDeleted());
    }

    @Test
    void memberShouldNotSeeOrOpenTeamDeletion() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(member))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/teams/" + team.getId() + "/delete"))));

        mockMvc.perform(post("/teams/" + team.getId() + "/delete")
                        .with(csrf())
                        .with(user(new AuthUser(member)))
                        .param("confirmationText", "я хочу удалить команду"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));

        assertFalse(teamService.findById(team.getId()).isDeleted());
    }

    @Test
    void deleteTeamShouldRequireCsrf() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/delete")
                        .with(user(new AuthUser(owner)))
                        .param("confirmationText", "я хочу удалить команду"))
                .andExpect(status().isForbidden());

        assertFalse(teamService.findById(team.getId()).isDeleted());
    }

    @Test
    void ownerShouldRemoveMember() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/members/" + member.getId() + "/remove")
                        .with(csrf())
                        .with(user(new AuthUser(owner))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/members"))
                .andExpect(flash().attribute("successMessage", "Участник удалён из команды"));

        TeamMember removedMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), member.getId());

        assertTrue(removedMember.isDeleted());
        assertThrows(TeamMemberNotFoundException.class, () -> teamMemberService.findById(team.getId(), member.getId()));
        assertFalse(teamMemberService.findByTeamId(team.getId()).stream()
                .anyMatch(teamMember -> teamMember.getUserId().equals(member.getId())));
    }

    @Test
    void ownerShouldChangeMemberTaskVisibility() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/members/" + member.getId() + "/visibility")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("taskVisibility", TeamTaskVisibility.ALL_TASKS.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/members"))
                .andExpect(flash().attributeCount(0));

        assertEquals(
                TeamTaskVisibility.ALL_TASKS,
                teamMemberService.findById(team.getId(), member.getId()).getTaskVisibility());
    }

    @Test
    void teamTaskCountsShouldRespectMemberTaskVisibility() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(member))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"task-count\">Задачи (1)")));

        teamMemberService.updateTaskVisibility(
                team.getId(), member.getId(), TeamTaskVisibility.ALL_TASKS, owner.getId());

        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(member))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"task-count\">Задачи (2)")));

        List<Task> tasks = taskService.findByTeamId(team.getId());

        for (Task task : tasks) {
            taskService.archive(task.getId(), owner.getId());
        }

        teamMemberService.updateTaskVisibility(
                team.getId(), member.getId(), TeamTaskVisibility.OWN_TASKS, owner.getId());

        mockMvc.perform(get("/teams/" + team.getId() + "/archive").with(user(new AuthUser(member))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"task-count\">Задачи (1)")));

        teamMemberService.updateTaskVisibility(
                team.getId(), member.getId(), TeamTaskVisibility.ALL_TASKS, owner.getId());

        mockMvc.perform(get("/teams/" + team.getId() + "/archive").with(user(new AuthUser(member))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"task-count\">Задачи (2)")));
    }

    @Test
    void ownerShouldChangeMemberTaskVisibilityWithoutRedirectForAsynchronousRequest() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/members/" + member.getId() + "/visibility")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("taskVisibility", TeamTaskVisibility.ALL_TASKS.name()))
                .andExpect(status().isNoContent())
                .andExpect(flash().attributeCount(0));

        assertEquals(
                TeamTaskVisibility.ALL_TASKS,
                teamMemberService.findById(team.getId(), member.getId()).getTaskVisibility());
    }

    @Test
    void memberShouldNotChangeTaskVisibility() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/members/" + owner.getId() + "/visibility")
                        .with(csrf())
                        .with(user(new AuthUser(member)))
                        .param("taskVisibility", TeamTaskVisibility.OWN_TASKS.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/members"))
                .andExpect(flash().attribute("errorMessage", "Только owner команды может изменять видимость задач"));

        assertEquals(
                TeamTaskVisibility.ALL_TASKS,
                teamMemberService.findById(team.getId(), owner.getId()).getTaskVisibility());
    }

    @Test
    void memberShouldNotRemoveAnotherMember() throws Exception {
        User anotherMember = userService.create("another-member@test.com", "Another member", "qwerty");
        teamMemberService.addMember(team.getId(), anotherMember.getId());

        mockMvc.perform(post("/teams/" + team.getId() + "/members/" + anotherMember.getId() + "/remove")
                        .with(csrf())
                        .with(user(new AuthUser(member))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/members"))
                .andExpect(flash().attribute("errorMessage", "Только owner команды может удалять участников"));

        TeamMember activeMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), anotherMember.getId());

        assertFalse(activeMember.isDeleted());
    }

    @Test
    void ownerShouldNotRemoveOwner() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/members/" + owner.getId() + "/remove")
                        .with(csrf())
                        .with(user(new AuthUser(owner))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/members"))
                .andExpect(flash().attribute("errorMessage", "Только owner команды может удалять участников"));

        TeamMember ownerMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), owner.getId());

        assertFalse(ownerMember.isDeleted());
    }

    @Test
    void repeatedRemoveShouldReturnErrorAndKeepMemberDeleted() throws Exception {
        teamMemberService.removeMember(team.getId(), member.getId(), owner.getId());

        mockMvc.perform(post("/teams/" + team.getId() + "/members/" + member.getId() + "/remove")
                        .with(csrf())
                        .with(user(new AuthUser(owner))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/members"))
                .andExpect(flash().attribute("errorMessage", "Участника не удалось удалить"));

        TeamMember removedMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), member.getId());

        assertTrue(removedMember.isDeleted());
    }

    @Test
    void removeMemberShouldRequireCsrf() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/members/" + member.getId() + "/remove")
                        .with(user(new AuthUser(owner))))
                .andExpect(status().isForbidden());

        TeamMember activeMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), member.getId());

        assertFalse(activeMember.isDeleted());
    }

    @Test
    void ownerShouldSeeRemoveActionForMemberButNotForOwner() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId() + "/members").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-team-member-remove-open")))
                .andExpect(content().string(containsString("data-member-id=\"" + member.getId() + "\"")))
                .andExpect(content().string(not(containsString("data-member-id=\"" + owner.getId() + "\""))))
                .andExpect(content().string(containsString("data-team-member-remove-dialog")));
    }

    @Test
    void memberShouldNotSeeRemoveActions() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId() + "/members").with(user(new AuthUser(member))))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string(containsString("Актуальный список участников команды «" + team.getName() + "»")))
                .andExpect(content().string(containsString("team-member-list-read-only")))
                .andExpect(content().string(containsString(owner.getEmail())))
                .andExpect(content().string(containsString("Владелец")))
                .andExpect(content().string(containsString("Покинуть команду")))
                .andExpect(content().string(not(containsString("data-team-visibility-form"))))
                .andExpect(content().string(not(containsString("data-team-member-remove-open"))))
                .andExpect(content().string(not(containsString("data-team-member-remove-dialog"))));
    }

    @Test
    void memberShouldSeeAllTasksBadgeOnlyOnOwnRow() throws Exception {
        User otherMember = userService.create("other-member@test.com", "Other member", "qwerty");
        teamMemberService.addMember(team.getId(), otherMember.getId());
        teamMemberService.updateTaskVisibility(
                team.getId(), member.getId(), TeamTaskVisibility.ALL_TASKS, owner.getId());
        teamMemberService.updateTaskVisibility(
                team.getId(), otherMember.getId(), TeamTaskVisibility.ALL_TASKS, owner.getId());

        MvcResult result = mockMvc.perform(
                        get("/teams/" + team.getId() + "/members").with(user(new AuthUser(member))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Видит все задачи")))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertEquals(1, response.split("Видит все задачи", -1).length - 1);
    }

    @Test
    void memberShouldLeaveTeamAfterConfirmation() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/leave")
                        .with(csrf())
                        .with(user(new AuthUser(member)))
                        .param("confirmationText", "да хочу выйти из команды"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams"))
                .andExpect(flash().attribute("successMessage", "Вы покинули команду «" + team.getName() + "»"));

        TeamMember formerMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), member.getId());

        assertTrue(formerMember.isDeleted());
    }

    @Test
    void memberShouldRemainInTeamWhenLeaveConfirmationIsInvalid() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/leave")
                        .with(csrf())
                        .with(user(new AuthUser(member)))
                        .param("confirmationText", "да"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/settings?section=leave"))
                .andExpect(flash().attribute("errorMessage", "Проверочный текст введен неверно"));

        TeamMember activeMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), member.getId());

        assertFalse(activeMember.isDeleted());
    }

    @Test
    void ownerShouldNotLeaveTeam() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/leave")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("confirmationText", "да хочу выйти из команды"))
                .andExpect(status().isNotFound());

        TeamMember ownerMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), owner.getId());

        assertFalse(ownerMember.isDeleted());
    }

    @Test
    void leaveTeamShouldRequireCsrf() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/leave")
                        .with(user(new AuthUser(member)))
                        .param("confirmationText", "да хочу выйти из команды"))
                .andExpect(status().isForbidden());

        TeamMember activeMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), member.getId());

        assertFalse(activeMember.isDeleted());
    }

    @Test
    void ownerShouldSeeMembersTableWhenOnlyOwnerRemains() throws Exception {
        teamMemberService.removeMember(team.getId(), member.getId(), owner.getId());

        mockMvc.perform(get("/teams/" + team.getId() + "/members").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"team-member-list\"")))
                .andExpect(content().string(containsString("Владелец")))
                .andExpect(content().string(containsString("Видит все задачи")));
    }

    @Test
    void ownerShouldInviteRemovedMember() throws Exception {
        teamMemberService.removeMember(team.getId(), member.getId(), owner.getId());

        mockMvc.perform(post("/teams/" + team.getId() + "/members")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("email", member.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/invite"))
                .andExpect(flash().attribute("successMessage", "Приглашение успешно создано"));

        List<TeamInvitation> invitations = teamInvitationRepository.findByTeamId(team.getId());

        assertEquals(1, invitations.size());
        assertEquals(member.getEmail(), invitations.getFirst().getInvitedEmail());
    }

    @Test
    void ownerShouldCreatePendingInvitation() throws Exception {
        String invitedEmail = "new-member@test.com";

        mockMvc.perform(post("/teams/" + team.getId() + "/members")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("email", invitedEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/invite"))
                .andExpect(flash().attribute("successMessage", "Приглашение успешно создано"));

        List<TeamInvitation> invitations = teamInvitationRepository.findByTeamId(team.getId());
        TeamInvitation invitation = invitations.getFirst();

        assertEquals(1, invitations.size());
        assertEquals(invitedEmail, invitation.getInvitedEmail());
        assertEquals(TeamInvitationStatus.PENDING, invitation.getStatus());
        assertEquals(
                TeamInvitationService.EXPIRATION_DAYS,
                Duration.between(invitation.getCreatedAt(), invitation.getExpiresAt())
                        .toDays());
        assertFalse(invitation.getToken().isBlank());
        verify(invitationEmailService).sendInvitation(any(TeamInvitation.class), any(Team.class), any(User.class));
    }

    @Test
    void ownerShouldKeepInvitationWhenEmailSendingFails() throws Exception {
        String invitedEmail = "smtp-failure@test.com";
        doThrow(new InvitationEmailSendingException(new RuntimeException()))
                .when(invitationEmailService)
                .sendInvitation(any(TeamInvitation.class), any(Team.class), any(User.class));

        mockMvc.perform(post("/teams/" + team.getId() + "/members")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("email", invitedEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/invite"))
                .andExpect(flash().attribute("successMessage", "Приглашение успешно создано"));

        List<TeamInvitation> invitations = teamInvitationRepository.findByTeamId(team.getId());

        assertEquals(1, invitations.size());
        assertEquals(TeamInvitationStatus.PENDING, invitations.getFirst().getStatus());
    }

    @Test
    void ownerShouldResendPendingInvitation() throws Exception {
        TeamInvitation invitation =
                teamInvitationRepository.save(createInvitation("resend-member@test.com", TeamInvitationStatus.PENDING));
        reset(invitationEmailService);

        mockMvc.perform(post("/teams/" + team.getId() + "/invitations/" + invitation.getId() + "/resend")
                        .with(csrf())
                        .with(user(new AuthUser(owner))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/invite"))
                .andExpect(flash().attribute(
                                "successMessage",
                                "Приглашение отправлено повторно. Прошлая ссылка больше недействительна"));

        verify(invitationEmailService).sendInvitation(any(TeamInvitation.class), any(Team.class), any(User.class));
        TeamInvitation resentInvitation =
                teamInvitationRepository.findByTeamId(team.getId()).getFirst();
        assertNull(teamInvitationRepository.findByToken(invitation.getToken()));
        assertNotEquals(invitation.getToken(), resentInvitation.getToken());
        assertEquals(
                TeamInvitationService.EXPIRATION_DAYS,
                Duration.between(resentInvitation.getUpdatedAt(), resentInvitation.getExpiresAt())
                        .toDays());
    }

    @Test
    void memberShouldNotResendInvitation() throws Exception {
        TeamInvitation invitation =
                teamInvitationRepository.save(createInvitation("member-resend@test.com", TeamInvitationStatus.PENDING));
        reset(invitationEmailService);

        mockMvc.perform(post("/teams/" + team.getId() + "/invitations/" + invitation.getId() + "/resend")
                        .with(csrf())
                        .with(user(new AuthUser(member))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/invite"))
                .andExpect(flash().attribute(
                                "errorMessage",
                                "Только owner команды может приглашать новых участников. "
                                        + "Вы можете пока только просматривать команду"));

        verify(invitationEmailService, never()).sendInvitation(any(), any(), any());
    }

    @Test
    void emailSendingFailureShouldNotCancelResentInvitation() throws Exception {
        TeamInvitation invitation =
                teamInvitationRepository.save(createInvitation("failed-resend@test.com", TeamInvitationStatus.PENDING));
        doThrow(new InvitationEmailSendingException(new RuntimeException()))
                .when(invitationEmailService)
                .sendInvitation(any(TeamInvitation.class), any(Team.class), any(User.class));

        mockMvc.perform(post("/teams/" + team.getId() + "/invitations/" + invitation.getId() + "/resend")
                        .with(csrf())
                        .with(user(new AuthUser(owner))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/invite"));

        TeamInvitation resentInvitation =
                teamInvitationRepository.findByTeamId(team.getId()).getFirst();
        assertEquals(TeamInvitationStatus.PENDING, resentInvitation.getStatus());
        assertNotEquals(invitation.getToken(), resentInvitation.getToken());
    }

    @Test
    void memberShouldNotCreateInvitation() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/members")
                        .with(csrf())
                        .with(user(new AuthUser(member)))
                        .param("email", "new-member@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/invite"))
                .andExpect(flash().attribute(
                                "errorMessage",
                                "Только owner команды может приглашать новых участников. "
                                        + "Вы можете пока только просматривать команду"));

        assertTrue(teamInvitationRepository.findByTeamId(team.getId()).isEmpty());
    }

    @Test
    void ownerShouldNotInviteActiveMember() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/members")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("email", member.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/invite"))
                .andExpect(flash().attribute("errorMessage", "Пользователь уже состоит в команде"));

        assertTrue(teamInvitationRepository.findByTeamId(team.getId()).isEmpty());
    }

    @Test
    void ownerShouldNotCreateInvitationWithInvalidEmail() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/members")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("email", "invalid-email"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/invite"))
                .andExpect(flash().attribute("errorMessage", "Введите корректный email"));

        assertTrue(teamInvitationRepository.findByTeamId(team.getId()).isEmpty());
    }

    @Test
    void ownerShouldNotCreateDuplicatePendingInvitation() throws Exception {
        String invitedEmail = "new-member@test.com";

        mockMvc.perform(post("/teams/" + team.getId() + "/members")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("email", invitedEmail))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/teams/" + team.getId() + "/members")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("email", invitedEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/invite"))
                .andExpect(flash().attribute("errorMessage", "Приглашение на этот email уже отправлено"));

        assertEquals(1, teamInvitationRepository.findByTeamId(team.getId()).size());
    }

    @Test
    void invitePageShouldShowInvitationHistoryWithPendingLinksOnly() throws Exception {
        TeamInvitation pendingInvitation = teamInvitationRepository.save(
                createInvitation("pending-member@test.com", TeamInvitationStatus.PENDING));
        TeamInvitation acceptedInvitation = teamInvitationRepository.save(
                createInvitation("accepted-member@test.com", TeamInvitationStatus.ACCEPTED));

        mockMvc.perform(get("/teams/" + team.getId() + "/invite").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ссылка действует 7 дней")))
                .andExpect(content().string(containsString("История приглашений")))
                .andExpect(content().string(containsString(pendingInvitation.getInvitedEmail())))
                .andExpect(content().string(containsString("/invitations/" + pendingInvitation.getToken())))
                .andExpect(content().string(containsString(acceptedInvitation.getInvitedEmail())))
                .andExpect(content().string(not(containsString("/invitations/" + acceptedInvitation.getToken()))));
    }

    @Test
    void invitePageShouldKeepInvitationOrderAfterStatusChange() throws Exception {
        TeamInvitation firstInvitation =
                teamInvitationRepository.save(createInvitation("first-member@test.com", TeamInvitationStatus.PENDING));
        TeamInvitation secondInvitation =
                teamInvitationRepository.save(createInvitation("second-member@test.com", TeamInvitationStatus.PENDING));
        TeamInvitation thirdInvitation =
                teamInvitationRepository.save(createInvitation("third-member@test.com", TeamInvitationStatus.PENDING));

        mockMvc.perform(post("/teams/" + team.getId() + "/invitations/" + secondInvitation.getId() + "/cancel")
                        .with(csrf())
                        .with(user(new AuthUser(owner))))
                .andExpect(status().is3xxRedirection());

        String response = mockMvc.perform(
                        get("/teams/" + team.getId() + "/invite").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(response.indexOf(firstInvitation.getInvitedEmail())
                < response.indexOf(secondInvitation.getInvitedEmail()));
        assertTrue(response.indexOf(secondInvitation.getInvitedEmail())
                < response.indexOf(thirdInvitation.getInvitedEmail()));
    }

    @Test
    void ownerShouldCancelPendingInvitation() throws Exception {
        TeamInvitation invitation = teamInvitationRepository.save(
                createInvitation("pending-member@test.com", TeamInvitationStatus.PENDING));

        mockMvc.perform(post("/teams/" + team.getId() + "/invitations/" + invitation.getId() + "/cancel")
                        .with(csrf())
                        .with(user(new AuthUser(owner))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/invite"))
                .andExpect(flash().attribute("successMessage", "Приглашение успешно отменено"));

        assertEquals(
                TeamInvitationStatus.CANCELED,
                teamInvitationRepository.findByToken(invitation.getToken()).getStatus());
    }

    @Test
    void memberShouldNotCancelInvitation() throws Exception {
        TeamInvitation invitation = teamInvitationRepository.save(
                createInvitation("pending-member@test.com", TeamInvitationStatus.PENDING));

        mockMvc.perform(post("/teams/" + team.getId() + "/invitations/" + invitation.getId() + "/cancel")
                        .with(csrf())
                        .with(user(new AuthUser(member))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/invite"))
                .andExpect(flash().attribute(
                                "errorMessage",
                                "Только owner команды может приглашать новых участников. "
                                        + "Вы можете пока только просматривать команду"));

        assertEquals(
                TeamInvitationStatus.PENDING,
                teamInvitationRepository.findByToken(invitation.getToken()).getStatus());
    }

    @Test
    void terminalInvitationShouldNotBeCanceled() throws Exception {
        TeamInvitation invitation = teamInvitationRepository.save(
                createInvitation("accepted-member@test.com", TeamInvitationStatus.ACCEPTED));

        mockMvc.perform(post("/teams/" + team.getId() + "/invitations/" + invitation.getId() + "/cancel")
                        .with(csrf())
                        .with(user(new AuthUser(owner))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/invite"))
                .andExpect(flash().attribute("errorMessage", "Отменить можно только ожидающее приглашение"));

        assertEquals(
                TeamInvitationStatus.ACCEPTED,
                teamInvitationRepository.findByToken(invitation.getToken()).getStatus());
    }

    @Test
    void invitePageShouldShowCancelActionOnlyForPendingInvitations() throws Exception {
        TeamInvitation pendingInvitation = teamInvitationRepository.save(
                createInvitation("pending-member@test.com", TeamInvitationStatus.PENDING));
        TeamInvitation acceptedInvitation = teamInvitationRepository.save(
                createInvitation("accepted-member@test.com", TeamInvitationStatus.ACCEPTED));

        mockMvc.perform(get("/teams/" + team.getId() + "/invite").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<th>Действие</th>"))))
                .andExpect(content().string(containsString("Отправить повторно")))
                .andExpect(content().string(containsString("Отменить приглашение")))
                .andExpect(content()
                        .string(containsString(
                                "/teams/" + team.getId() + "/invitations/" + pendingInvitation.getId() + "/cancel")))
                .andExpect(content()
                        .string(not(containsString(
                                "/teams/" + team.getId() + "/invitations/" + acceptedInvitation.getId() + "/cancel"))));
    }

    @Test
    void cancelInvitationShouldRequireCsrf() throws Exception {
        TeamInvitation invitation = teamInvitationRepository.save(
                createInvitation("pending-member@test.com", TeamInvitationStatus.PENDING));

        mockMvc.perform(post("/teams/" + team.getId() + "/invitations/" + invitation.getId() + "/cancel")
                        .with(user(new AuthUser(owner))))
                .andExpect(status().isForbidden());

        assertEquals(
                TeamInvitationStatus.PENDING,
                teamInvitationRepository.findByToken(invitation.getToken()).getStatus());
    }

    @Test
    void authenticatedUserShouldOpenInvitationOverTasksPage() throws Exception {
        User invitedUser = userService.create("invited-member@test.com", "Invited member", "qwerty");
        TeamInvitation invitation = teamInvitationService.create(team.getId(), invitedUser.getEmail(), owner.getId());

        mockMvc.perform(get("/invitations/" + invitation.getToken()).with(user(new AuthUser(invitedUser))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks?invitation=" + invitation.getToken()));

        mockMvc.perform(get("/tasks?invitation=" + invitation.getToken()).with(user(new AuthUser(invitedUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Мои задачи")))
                .andExpect(content().string(containsString("Приглашение в команду")))
                .andExpect(content().string(containsString("team-owner@test.com пригласил вас")))
                .andExpect(content().string(not(containsString("Owner"))))
                .andExpect(content().string(containsString("Project Team")))
                .andExpect(content().string(containsString("Принять")))
                .andExpect(content().string(containsString("Отклонить")))
                .andExpect(content().string(containsString("data-invitation-decision-dialog")))
                .andExpect(content().string(containsString("data-invitation-decision-close")));

        assertEquals(
                TeamInvitationStatus.PENDING,
                teamInvitationRepository.findByToken(invitation.getToken()).getStatus());
    }

    @Test
    void anonymousUserShouldReturnToValidInvitationAfterLogin() throws Exception {
        TeamInvitation invitation =
                teamInvitationService.create(team.getId(), "invited-member@test.com", owner.getId());

        mockMvc.perform(get("/invitations/" + invitation.getToken()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?redirect=/invitations/" + invitation.getToken() + "&invite=" + invitation.getToken()));
    }

    @Test
    void validUserShouldAcceptInvitation() throws Exception {
        User invitedUser = userService.create("accepted-invite@test.com", "Accepted invite", "qwerty");
        TeamInvitation invitation = teamInvitationService.create(team.getId(), invitedUser.getEmail(), owner.getId());

        mockMvc.perform(post("/invitations/" + invitation.getToken() + "/accept")
                        .with(csrf())
                        .with(user(new AuthUser(invitedUser))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId()))
                .andExpect(flash().attribute(
                                "successMessage", "Приглашение принято. Теперь вы состоите в команде «Project Team»"));

        assertEquals(
                TeamInvitationStatus.ACCEPTED,
                teamInvitationRepository.findByToken(invitation.getToken()).getStatus());
        assertTrue(teamMemberService.isActiveMember(team.getId(), invitedUser.getId()));
    }

    @Test
    void validUserShouldDeclineInvitation() throws Exception {
        User invitedUser = userService.create("declined-invite@test.com", "Declined invite", "qwerty");
        TeamInvitation invitation = teamInvitationService.create(team.getId(), invitedUser.getEmail(), owner.getId());

        mockMvc.perform(post("/invitations/" + invitation.getToken() + "/decline")
                        .with(csrf())
                        .with(user(new AuthUser(invitedUser))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"))
                .andExpect(flash().attributeCount(0));

        assertEquals(
                TeamInvitationStatus.DECLINED,
                teamInvitationRepository.findByToken(invitation.getToken()).getStatus());
        assertFalse(teamMemberService.isActiveMember(team.getId(), invitedUser.getId()));
    }

    @Test
    void activeMemberShouldOpenTeamWithoutChangingInvitation() throws Exception {
        TeamInvitation invitation =
                teamInvitationService.create(team.getId(), "another-invited-member@test.com", owner.getId());

        mockMvc.perform(get("/invitations/" + invitation.getToken()).with(user(new AuthUser(member))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId()));

        assertEquals(
                TeamInvitationStatus.PENDING,
                teamInvitationRepository.findByToken(invitation.getToken()).getStatus());
    }

    @Test
    void activeMemberShouldNotAcceptInvitation() throws Exception {
        TeamInvitation invitation =
                teamInvitationService.create(team.getId(), "another-invited-member@test.com", owner.getId());

        mockMvc.perform(post("/invitations/" + invitation.getToken() + "/accept")
                        .with(csrf())
                        .with(user(new AuthUser(member))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId()));

        assertEquals(
                TeamInvitationStatus.PENDING,
                teamInvitationRepository.findByToken(invitation.getToken()).getStatus());
    }

    @Test
    void anyNonMemberWithLiveLinkShouldAcceptInvitation() throws Exception {
        User invitedUser = userService.create("link-holder@test.com", "Link holder", "qwerty");
        TeamInvitation invitation =
                teamInvitationService.create(team.getId(), "mistyped-email@test.com", owner.getId());

        mockMvc.perform(post("/invitations/" + invitation.getToken() + "/accept")
                        .with(csrf())
                        .with(user(new AuthUser(invitedUser))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId()));

        assertEquals(
                TeamInvitationStatus.ACCEPTED,
                teamInvitationRepository.findByToken(invitation.getToken()).getStatus());
        assertTrue(teamMemberService.isActiveMember(team.getId(), invitedUser.getId()));
    }

    @Test
    void invalidInvitationShouldShowPublicInvalidPageForAnonymousUser() throws Exception {
        TeamInvitation invitation = teamInvitationRepository.save(
                createInvitation("canceled-invite@test.com", TeamInvitationStatus.CANCELED));

        mockMvc.perform(get("/invitations/" + invitation.getToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ссылка приглашения недействительна")))
                .andExpect(content().string(not(containsString("Приглашение было принято"))))
                .andExpect(content().string(containsString("Вернуться на главную")));
    }

    @Test
    void invalidInvitationShouldShowTasksLinkForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/invitations/unknown-token").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ссылка приглашения недействительна")))
                .andExpect(content().string(containsString("Вернуться на главную")));
    }

    @Test
    void expiredInvitationShouldShowInvalidPageAndBeExpired() throws Exception {
        TeamInvitation invitation = teamInvitationRepository.save(createInvitation(
                "expired-invite@test.com",
                TeamInvitationStatus.PENDING,
                Instant.now().minusSeconds(60)));

        mockMvc.perform(get("/invitations/" + invitation.getToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ссылка приглашения недействительна")));

        assertEquals(
                TeamInvitationStatus.EXPIRED,
                teamInvitationRepository.findByToken(invitation.getToken()).getStatus());
    }

    @Test
    void acceptedInvitationShouldShowInvalidPage() throws Exception {
        TeamInvitation invitation = teamInvitationRepository.save(
                createInvitation("accepted-invite@test.com", TeamInvitationStatus.ACCEPTED));

        mockMvc.perform(get("/invitations/" + invitation.getToken()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ссылка приглашения недействительна")));
    }

    @Test
    void invitationAcceptShouldRequireCsrf() throws Exception {
        User invitedUser = userService.create("csrf-invite@test.com", "Csrf invite", "qwerty");
        TeamInvitation invitation = teamInvitationService.create(team.getId(), invitedUser.getEmail(), owner.getId());

        mockMvc.perform(post("/invitations/" + invitation.getToken() + "/accept")
                        .with(user(new AuthUser(invitedUser))))
                .andExpect(status().isForbidden());

        assertEquals(
                TeamInvitationStatus.PENDING,
                teamInvitationRepository.findByToken(invitation.getToken()).getStatus());
    }

    @Test
    void userWithoutTeamAccessShouldSeeNotFoundAfterLoginRedirect() throws Exception {
        User outsider = userService.create("outsider@test.com", "Outsider", "qwerty");
        dsl.update(USERS)
                .set(USERS.EMAIL_VERIFIED, true)
                .set(USERS.EMAIL_VERIFIED_AT, OffsetDateTime.now())
                .where(USERS.ID.eq(outsider.getId()))
                .execute();

        MvcResult anonymousResult = mockMvc.perform(get("/teams/" + team.getId()))
                .andExpect(status().isFound())
                .andReturn();

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .session((MockHttpSession) anonymousResult.getRequest().getSession())
                        .param("username", outsider.getEmail())
                        .param("password", "qwerty"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/teams/" + team.getId() + "?continue"))
                .andReturn();

        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(outsider))))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("class=\"error-page-code\""))))
                .andExpect(content().string(containsString("Такая страница не найдена")));

        mockMvc.perform(get("/teams/" + team.getId()).queryParam("continue", "").session((MockHttpSession)
                        loginResult.getRequest().getSession()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Такая страница не найдена")));
    }

    @Test
    void formerAssigneeShouldAppearInActiveTaskFilters() throws Exception {
        teamMemberService.removeMember(team.getId(), member.getId(), owner.getId());

        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("assigneeId=" + member.getId())))
                .andExpect(content().string(containsString("Member")))
                .andExpect(content().string(containsString("color: red;")))
                .andExpect(content().string(containsString("Пользователь был удалён из команды")));
    }

    @Test
    void formerMemberShouldDisappearFromActiveTaskFiltersAfterReassignment() throws Exception {
        teamMemberService.removeMember(team.getId(), member.getId(), owner.getId());

        Task visibleTask = taskService.findByTeamId(team.getId()).stream()
                .filter(task -> task.getAssigneeId().equals(member.getId()))
                .findFirst()
                .orElseThrow();

        taskService.updateAssignee(visibleTask.getId(), owner.getId(), owner.getId());

        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("assigneeId=" + member.getId()))));
    }

    @Test
    void formerMemberWithOnlyArchivedTasksShouldNotAppearInActiveTaskFilters() throws Exception {
        Task visibleTask = taskService.findByTeamId(team.getId()).stream()
                .filter(task -> task.getAssigneeId().equals(member.getId()))
                .findFirst()
                .orElseThrow();

        taskService.updateStatus(visibleTask.getId(), TaskStatus.DONE, owner.getId());
        taskService.archive(visibleTask.getId(), owner.getId());
        teamMemberService.removeMember(team.getId(), member.getId(), owner.getId());

        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("assigneeId=" + member.getId()))));
    }

    private void cleanDatabase() {
        dsl.deleteFrom(USER_NOTIFICATIONS).execute();
        dsl.deleteFrom(NOTIFICATION_EVENTS).execute();
        dsl.deleteFrom(COMMENTS).execute();
        dsl.deleteFrom(TASKS).execute();
        dsl.deleteFrom(TEAM_INVITATIONS).execute();
        dsl.deleteFrom(TEAM_TAGS).execute();
        dsl.deleteFrom(TEAM_MEMBERS).execute();
        dsl.deleteFrom(TEAMS).execute();
        dsl.deleteFrom(USERS).execute();
    }

    private TeamInvitation createInvitation(String invitedEmail, TeamInvitationStatus status) {
        return createInvitation(
                invitedEmail, status, Instant.now().plus(Duration.ofDays(TeamInvitationService.EXPIRATION_DAYS)));
    }

    private TeamInvitation createInvitation(String invitedEmail, TeamInvitationStatus status, Instant expiresAt) {
        Instant now = Instant.now();

        return new TeamInvitation(
                null,
                team.getId(),
                owner.getId(),
                invitedEmail,
                "token-" + invitedEmail,
                status,
                expiresAt,
                now,
                now,
                false);
    }
}
