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
    void membersPageShouldShowOnlyVisibleTasksCount() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId() + "/members").with(user(new AuthUser(member))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Задачи (1)")))
                .andExpect(content().string(not(containsString("Задачи (2)"))));
    }

    @Test
    void ownerShouldDeleteTeamAfterThreeConfirmations() throws Exception {
        final TeamInvitation invitation =
                teamInvitationService.create(team.getId(), "delete-team@test.com", owner.getId());
        List<Task> tasks = taskService.findByTeamId(team.getId());
        commentService.create(tasks.getFirst().getId(), owner.getId(), "Historical comment");
        final int membersCount = dsl.fetchCount(TEAM_MEMBERS, TEAM_MEMBERS.TEAM_ID.eq(team.getId()));
        final int tasksCount = dsl.fetchCount(TASKS, TASKS.TEAM_ID.eq(team.getId()));
        final int tagsCount = dsl.fetchCount(TEAM_TAGS, TEAM_TAGS.TEAM_ID.eq(team.getId()));
        final int commentsCount = dsl.fetchCount(COMMENTS);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/teams/" + team.getId() + "/delete")
                        .session(session)
                        .with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Вы уверены, что хотите удалить команду?")));

        mockMvc.perform(post("/teams/" + team.getId() + "/delete")
                        .session(session)
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("confirmation", "yes"))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string(containsString("Все участники потеряют доступ к команде и её задачам. Продолжить?")));

        mockMvc.perform(post("/teams/" + team.getId() + "/delete")
                        .session(session)
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("confirmation", "yes"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Это последнее подтверждение. Удалить команду?")));

        mockMvc.perform(post("/teams/" + team.getId() + "/delete")
                        .session(session)
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("confirmation", "yes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams"));

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
        mockMvc.perform(get("/teams/" + team.getId() + "/delete").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(view().name("teams/not-found"));
    }

    @Test
    void directDeleteRequestShouldNotBypassConfirmations() throws Exception {
        mockMvc.perform(post("/teams/" + team.getId() + "/delete")
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("confirmation", "yes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId()));

        assertFalse(teamService.findById(team.getId()).isDeleted());
    }

    @Test
    void noShouldCancelTeamDeletion() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/teams/" + team.getId() + "/delete")
                        .session(session)
                        .with(user(new AuthUser(owner))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/teams/" + team.getId() + "/delete")
                        .session(session)
                        .with(csrf())
                        .with(user(new AuthUser(owner)))
                        .param("confirmation", "no"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + team.getId()));

        assertFalse(teamService.findById(team.getId()).isDeleted());
    }

    @Test
    void memberShouldNotSeeOrOpenTeamDeletion() throws Exception {
        mockMvc.perform(get("/teams/" + team.getId()).with(user(new AuthUser(member))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/teams/" + team.getId() + "/delete"))));

        mockMvc.perform(get("/teams/" + team.getId() + "/delete").with(user(new AuthUser(member))))
                .andExpect(status().isOk())
                .andExpect(view().name("teams/not-found"));

        mockMvc.perform(post("/teams/" + team.getId() + "/delete")
                        .with(csrf())
                        .with(user(new AuthUser(member)))
                        .param("confirmation", "yes"))
                .andExpect(status().isOk())
                .andExpect(view().name("teams/not-found"));

        assertFalse(teamService.findById(team.getId()).isDeleted());
    }

    @Test
    void deleteTeamShouldRequireCsrf() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/teams/" + team.getId() + "/delete")
                        .session(session)
                        .with(user(new AuthUser(owner))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/teams/" + team.getId() + "/delete")
                        .session(session)
                        .with(user(new AuthUser(owner)))
                        .param("confirmation", "yes"))
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
                .andExpect(flash().attribute(
                                "successMessage", "Приглашение создано. Ссылку-приглашение можно отправить лично."));

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
                .andExpect(flash().attribute(
                                "successMessage", "Приглашение создано. Ссылку-приглашение можно отправить лично."));

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
                .andExpect(flash().attribute(
                                "successMessage", "Приглашение создано. Ссылку-приглашение можно отправить лично."));

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
                .andExpect(redirectedUrl("/teams/" + team.getId() + "/invite"));

        verify(invitationEmailService).sendInvitation(any(TeamInvitation.class), any(Team.class), any(User.class));
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
                                        + "Вы можете пока только просматривать команду."));

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

        assertEquals(
                TeamInvitationStatus.PENDING,
                teamInvitationRepository.findByToken(invitation.getToken()).getStatus());
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
                .andExpect(flash().attribute("successMessage", "Приглашение отменено."));

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
                                        + "Вы можете пока только просматривать команду."));

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
                .andExpect(flash().attribute("errorMessage", "Отменить можно только ожидающее приглашение."));

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
                .andExpect(content().string(containsString("Отправить письмо повторно")))
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
    void authenticatedUserShouldSeeInvitationDecisionPage() throws Exception {
        User invitedUser = userService.create("invited-member@test.com", "Invited member", "qwerty");
        TeamInvitation invitation = teamInvitationService.create(team.getId(), invitedUser.getEmail(), owner.getId());

        mockMvc.perform(get("/invitations/" + invitation.getToken()).with(user(new AuthUser(invitedUser))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Приглашение в команду")))
                .andExpect(content().string(containsString("Вас пригласил team-owner@test.com")))
                .andExpect(content().string(not(containsString("Owner"))))
                .andExpect(content().string(containsString("Project Team")))
                .andExpect(content().string(containsString("Принять приглашение")))
                .andExpect(content().string(containsString("Отклонить приглашение")));
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
                .andExpect(redirectedUrl("/teams"));

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
                .andExpect(redirectedUrl("/teams"));

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
                .andExpect(redirectedUrl("/teams"));

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
                .andExpect(content().string(containsString("Ссылка приглашения недействительна.")))
                .andExpect(content().string(containsString("Войти")))
                .andExpect(content().string(containsString("Зарегистрироваться")))
                .andExpect(content().string(not(containsString("К моим задачам"))));
    }

    @Test
    void invalidInvitationShouldShowTasksLinkForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/invitations/unknown-token").with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ссылка приглашения недействительна.")))
                .andExpect(content().string(containsString("К моим задачам")))
                .andExpect(content().string(not(containsString("Зарегистрироваться"))));
    }

    @Test
    void expiredInvitationShouldShowInvalidPageAndBeCanceled() throws Exception {
        TeamInvitation invitation = teamInvitationRepository.save(createInvitation(
                "expired-invite@test.com",
                TeamInvitationStatus.PENDING,
                Instant.now().minusSeconds(60)));

        mockMvc.perform(get("/invitations/" + invitation.getToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ссылка приглашения недействительна.")));

        assertEquals(
                TeamInvitationStatus.CANCELED,
                teamInvitationRepository.findByToken(invitation.getToken()).getStatus());
    }

    @Test
    void acceptedInvitationShouldShowInvalidPage() throws Exception {
        TeamInvitation invitation = teamInvitationRepository.save(
                createInvitation("accepted-invite@test.com", TeamInvitationStatus.ACCEPTED));

        mockMvc.perform(get("/invitations/" + invitation.getToken()).with(user(new AuthUser(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ссылка приглашения недействительна.")));
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
