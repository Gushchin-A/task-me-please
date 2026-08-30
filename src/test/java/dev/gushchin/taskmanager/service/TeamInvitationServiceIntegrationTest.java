package dev.gushchin.taskmanager.service;

import static dev.gushchin.taskmanager.jooq.Tables.COMMENTS;
import static dev.gushchin.taskmanager.jooq.Tables.TASKS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAMS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_INVITATIONS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_MEMBERS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_TAGS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.exception.TeamInvitationNotPendingException;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamInvitationRepository;
import dev.gushchin.taskmanager.repository.TeamMemberRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TeamInvitationServiceIntegrationTest extends IntegrationTestBase {
    @Autowired
    private DSLContext dsl;

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private TeamInvitationService teamInvitationService;

    @Autowired
    private TeamInvitationRepository teamInvitationRepository;

    private User owner;
    private Team team;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        owner = userService.create("invite-owner@test.com", "Owner", "qwerty");
        team = teamService.create("Project Team", owner.getId());
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void createShouldGenerateUniqueTokenAndExpiration() {
        TeamInvitation firstInvitation =
                teamInvitationService.create(team.getId(), "first-member@test.com", owner.getId());
        TeamInvitation secondInvitation =
                teamInvitationService.create(team.getId(), "second-member@test.com", owner.getId());
        long expirationDays = Duration.between(firstInvitation.getCreatedAt(), firstInvitation.getExpiresAt())
                .toDays();

        assertNotEquals(firstInvitation.getToken(), secondInvitation.getToken());
        assertEquals(TeamInvitationStatus.PENDING, firstInvitation.getStatus());
        assertEquals(TeamInvitationService.EXPIRATION_DAYS, expirationDays);
    }

    @Test
    void findByTokenShouldCancelExpiredPendingInvitation() {
        TeamInvitation expiredInvitation = teamInvitationRepository.save(createInvitation(
                "expired-member@test.com",
                TeamInvitationStatus.PENDING,
                Instant.now().minusSeconds(60)));

        TeamInvitation invitation = teamInvitationService.findByToken(expiredInvitation.getToken());

        assertEquals(TeamInvitationStatus.EXPIRED, invitation.getStatus());
        assertEquals(
                TeamInvitationStatus.EXPIRED,
                teamInvitationRepository
                        .findByToken(expiredInvitation.getToken())
                        .getStatus());
    }

    @Test
    void acceptShouldRestoreRemovedMember() {
        User invitedUser = userService.create("removed-member@test.com", "Removed member", "qwerty");
        teamMemberService.addMember(team.getId(), invitedUser.getId());
        teamMemberService.removeMember(team.getId(), invitedUser.getId(), owner.getId());
        TeamInvitation invitation = teamInvitationService.create(team.getId(), invitedUser.getEmail(), owner.getId());

        TeamInvitation acceptedInvitation = teamInvitationService.accept(invitation.getToken(), invitedUser.getId());
        TeamMember restoredMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), invitedUser.getId());

        assertEquals(TeamInvitationStatus.ACCEPTED, acceptedInvitation.getStatus());
        assertFalse(restoredMember.isDeleted());
    }

    @Test
    void terminalInvitationShouldNotBeProcessedAgain() {
        User invitedUser = userService.create("accepted-member@test.com", "Accepted member", "qwerty");
        TeamInvitation acceptedInvitation = teamInvitationRepository.save(createInvitation(
                invitedUser.getEmail(),
                TeamInvitationStatus.ACCEPTED,
                Instant.now().plusSeconds(60)));

        assertThrows(
                TeamInvitationNotPendingException.class,
                () -> teamInvitationService.accept(acceptedInvitation.getToken(), invitedUser.getId()));

        List<TeamInvitation> invitations = teamInvitationRepository.findByTeamId(team.getId());

        assertEquals(TeamInvitationStatus.ACCEPTED, invitations.getFirst().getStatus());
        assertTrue(teamMemberService.findByTeamId(team.getId()).stream()
                .noneMatch(teamMember -> teamMember.getUserId().equals(invitedUser.getId())));
    }

    @Test
    void createShouldCancelExpiredPendingInvitationBeforeCreatingNewOne() {
        String invitedEmail = "expired-duplicate@test.com";
        teamInvitationRepository.save(createInvitation(
                invitedEmail, TeamInvitationStatus.PENDING, Instant.now().minusSeconds(60)));

        TeamInvitation newInvitation = teamInvitationService.create(team.getId(), invitedEmail, owner.getId());
        List<TeamInvitation> invitations = teamInvitationRepository.findByTeamId(team.getId());

        assertEquals(2, invitations.size());
        assertEquals(TeamInvitationStatus.EXPIRED, invitations.getFirst().getStatus());
        assertEquals(TeamInvitationStatus.PENDING, newInvitation.getStatus());
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
