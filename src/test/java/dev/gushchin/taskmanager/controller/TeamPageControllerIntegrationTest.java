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
import dev.gushchin.taskmanager.exception.TeamMemberNotFoundException;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamTag;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamInvitationRepository;
import dev.gushchin.taskmanager.repository.TeamMemberRepository;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.TaskService;
import dev.gushchin.taskmanager.service.TeamInvitationService;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.TeamTagService;
import dev.gushchin.taskmanager.service.UserService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
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
    private TeamTagService teamTagService;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private TeamInvitationRepository teamInvitationRepository;

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

    @Test
    void ownerShouldRemoveMember() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/members/" + member.getId() + "/remove")
                        .with(csrf())
                        .with(user(new AuthUser(owner))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/members"))
                .andExpect(flash().attribute("successMessage", "Участник удалён из команды."));

        TeamMember removedMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), member.getId());

        assertTrue(removedMember.isDeleted());
        assertThrows(TeamMemberNotFoundException.class, () -> teamMemberService.findById(team.getId(), member.getId()));
        assertFalse(teamMemberService.findByTeamId(team.getId()).stream()
                .anyMatch(teamMember -> teamMember.getUserId().equals(member.getId())));
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
                .andExpect(flash().attribute("errorMessage", "Только owner команды может удалять участников."));

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
                .andExpect(flash().attribute("errorMessage", "Только owner команды может удалять участников."));

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
                .andExpect(flash().attribute("errorMessage", "Участника не удалось удалить."));

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
    void ownerShouldSeeRemoveActionOnlyForMembers() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId() + "/members").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string(containsString("/teams/" + team.getId() + "/members/" + member.getId() + "/remove")))
                .andExpect(content()
                        .string(not(
                                containsString("/teams/" + team.getId() + "/members/" + owner.getId() + "/remove"))));
    }

    @Test
    void ownerShouldSeeMembersTableWhenOnlyOwnerRemains() throws Exception {
        teamMemberService.removeMember(team.getId(), member.getId(), owner.getId());

        mockMvc.perform(get("/teams/" + team.getId() + "/members").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Кроме вас в команде пока никого нет.")))
                .andExpect(content().string(containsString("<table>")))
                .andExpect(content().string(containsString("Owner")))
                .andExpect(content().string(containsString("OWNER")));
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
                .andExpect(flash().attribute("successMessage", "Приглашение отправлено."));

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
                .andExpect(flash().attribute("successMessage", "Приглашение отправлено."));

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
                                        + "Вы можете пока только просматривать команду."));

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
                .andExpect(flash().attribute("errorMessage", "Пользователь уже состоит в этой команде."));

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
                .andExpect(flash().attribute("errorMessage", "Приглашение на этот email уже отправлено."));

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
                .andExpect(content().string(containsString("Ссылка-приглашение действует 30 дней.")))
                .andExpect(content().string(containsString("Статус отправленных приглашений")))
                .andExpect(content().string(containsString(pendingInvitation.getInvitedEmail())))
                .andExpect(content().string(containsString("/invitations/" + pendingInvitation.getToken())))
                .andExpect(content().string(containsString(acceptedInvitation.getInvitedEmail())))
                .andExpect(content().string(not(containsString("/invitations/" + acceptedInvitation.getToken()))));
    }

    @Test
    void userWithoutTeamAccessShouldSeeNotFoundAfterLoginRedirect() throws Exception {
        User outsider = userService.create("outsider@test.com", "Outsider", "qwerty");

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
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Такая страница не найдена")));

        mockMvc.perform(get("/teams/" + team.getId()).queryParam("continue", "").session((MockHttpSession)
                        loginResult.getRequest().getSession()))
                .andExpect(status().isOk())
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
        dsl.deleteFrom(COMMENTS).execute();
        dsl.deleteFrom(TASKS).execute();
        dsl.deleteFrom(TEAM_INVITATIONS).execute();
        dsl.deleteFrom(TEAM_TAGS).execute();
        dsl.deleteFrom(TEAM_MEMBERS).execute();
        dsl.deleteFrom(TEAMS).execute();
        dsl.deleteFrom(USERS).execute();
    }

    private TeamInvitation createInvitation(String invitedEmail, TeamInvitationStatus status) {
        Instant now = Instant.now();

        return new TeamInvitation(
                null,
                team.getId(),
                owner.getId(),
                invitedEmail,
                "token-" + invitedEmail,
                status,
                now.plus(Duration.ofDays(TeamInvitationService.EXPIRATION_DAYS)),
                now,
                now,
                false);
    }
}
