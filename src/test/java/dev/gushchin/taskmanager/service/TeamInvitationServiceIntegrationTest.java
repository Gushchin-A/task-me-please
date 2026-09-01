package dev.gushchin.taskmanager.service;

import static dev.gushchin.taskmanager.jooq.Tables.COMMENTS;
import static dev.gushchin.taskmanager.jooq.Tables.NOTIFICATION_EVENTS;
import static dev.gushchin.taskmanager.jooq.Tables.TASKS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAMS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_INVITATIONS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_MEMBERS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_TAGS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static dev.gushchin.taskmanager.jooq.Tables.USER_NOTIFICATIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.exception.TeamInvitationNotPendingException;
import dev.gushchin.taskmanager.model.NotificationEventType;
import dev.gushchin.taskmanager.model.NotificationSort;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamInvitationRepository;
import dev.gushchin.taskmanager.repository.TeamMemberRepository;
import dev.gushchin.taskmanager.view.NotificationPage;
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

    @Autowired
    private NotificationService notificationService;

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
    void createAndResendShouldKeepSingleActionableNotificationForInvitedUser() {
        User invitedUser = userService.create("known-member@test.com", "Known member", "qwerty");
        final TeamInvitation invitation =
                teamInvitationService.create(team.getId(), invitedUser.getEmail(), owner.getId());

        assertLatestType(owner, NotificationEventType.TEAM_INVITATION_CREATED);
        assertLatestType(invitedUser, NotificationEventType.TEAM_INVITATION_CREATED);
        assertEquals(1, notificationService.getCounts(invitedUser.getId()).all());
        NotificationPage createdPage =
                notificationService.findPage(invitedUser.getId(), false, NotificationSort.NEWEST, null);
        assertTrue(createdPage.items().getFirst().invitationAction().active());
        assertEquals(
                invitation.getToken(),
                createdPage.items().getFirst().invitationAction().token());

        teamInvitationService.resend(invitation.getId(), team.getId(), owner.getId());

        assertLatestType(owner, NotificationEventType.TEAM_INVITATION_RESENT);
        assertLatestType(invitedUser, NotificationEventType.TEAM_INVITATION_CREATED);
        assertEquals(1, notificationService.getCounts(invitedUser.getId()).all());
        NotificationPage resentPage =
                notificationService.findPage(invitedUser.getId(), false, NotificationSort.NEWEST, null);
        assertTrue(resentPage.items().getFirst().invitationAction().active());
        assertNotEquals(
                invitation.getToken(),
                resentPage.items().getFirst().invitationAction().token());

        teamInvitationService.decline(
                resentPage.items().getFirst().invitationAction().token(), invitedUser.getId());

        NotificationPage declinedPage =
                notificationService.findPage(invitedUser.getId(), false, NotificationSort.OLDEST, null);
        assertNull(declinedPage.items().getFirst().invitationAction());
    }

    @Test
    void registrationShouldClaimInvitationNotificationByEmail() {
        String invitedEmail = "new-member@test.com";
        teamInvitationService.create(team.getId(), invitedEmail, owner.getId());

        User registeredUser = userService.create(invitedEmail, "New member", "qwerty");

        assertLatestType(registeredUser, NotificationEventType.TEAM_INVITATION_CREATED);
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

        int eventsAfterExpiration = dsl.fetchCount(NOTIFICATION_EVENTS);
        teamInvitationService.findByToken(expiredInvitation.getToken());

        assertEquals(eventsAfterExpiration, dsl.fetchCount(NOTIFICATION_EVENTS));
        assertLatestType(owner, NotificationEventType.TEAM_INVITATION_EXPIRED);
    }

    @Test
    void expirationProcessorShouldExpirePendingInvitationExactlyOnce() {
        TeamInvitation expiredInvitation = teamInvitationRepository.save(createInvitation(
                "scheduled-expiration@test.com",
                TeamInvitationStatus.PENDING,
                Instant.now().minusSeconds(60)));

        assertEquals(1, teamInvitationService.expirePendingInvitations());
        assertEquals(0, teamInvitationService.expirePendingInvitations());
        assertEquals(
                TeamInvitationStatus.EXPIRED,
                teamInvitationRepository
                        .findByToken(expiredInvitation.getToken())
                        .getStatus());
        assertLatestType(owner, NotificationEventType.TEAM_INVITATION_EXPIRED);
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
    void acceptShouldSendOneRelevantNotificationToEachRecipient() {
        User invitedUser = userService.create("accepted-member@test.com", "Accepted member", "qwerty");
        User otherMember = userService.create("other-member@test.com", "Other member", "qwerty");
        teamMemberService.addMember(team.getId(), otherMember.getId());
        TeamInvitation invitation = teamInvitationService.create(team.getId(), invitedUser.getEmail(), owner.getId());
        int ownerNotifications = notificationService.getCounts(owner.getId()).all();
        int invitedUserNotifications =
                notificationService.getCounts(invitedUser.getId()).all();
        int otherMemberNotifications =
                notificationService.getCounts(otherMember.getId()).all();

        teamInvitationService.accept(invitation.getToken(), invitedUser.getId());

        assertEquals(
                ownerNotifications + 1,
                notificationService.getCounts(owner.getId()).all());
        assertEquals(
                invitedUserNotifications + 1,
                notificationService.getCounts(invitedUser.getId()).all());
        assertEquals(
                otherMemberNotifications + 1,
                notificationService.getCounts(otherMember.getId()).all());
        assertLatestNotification(
                owner,
                NotificationEventType.TEAM_INVITATION_ACCEPTED,
                "accepted-member@test.com принял приглашение в команду «Project Team»");
        assertLatestNotification(
                invitedUser, NotificationEventType.TEAM_MEMBER_JOINED, "Вы присоединились к команде «Project Team»");
        assertLatestNotification(
                otherMember,
                NotificationEventType.TEAM_MEMBER_JOINED,
                "Accepted member присоединился к команде «Project Team»");
    }

    @Test
    void declineShouldUseInvitedEmailInOwnerNotification() {
        User invitedUser = userService.create("declined-member@test.com", "Declined member", "qwerty");
        TeamInvitation invitation = teamInvitationService.create(team.getId(), invitedUser.getEmail(), owner.getId());

        teamInvitationService.decline(invitation.getToken(), invitedUser.getId());

        assertLatestNotification(
                owner,
                NotificationEventType.TEAM_INVITATION_DECLINED,
                "declined-member@test.com отклонил приглашение в команду «Project Team»");
        assertLatestNotification(
                invitedUser,
                NotificationEventType.TEAM_INVITATION_DECLINED,
                "Вы отклонили приглашение в команду «Project Team»");
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

    private void assertLatestType(User user, NotificationEventType expectedType) {
        NotificationPage page = notificationService.findPage(user.getId(), false, NotificationSort.NEWEST, null);
        assertFalse(page.items().isEmpty());
        assertEquals(expectedType, page.items().getFirst().event().getType());
    }

    private void assertLatestNotification(User user, NotificationEventType expectedType, String expectedMessage) {
        NotificationPage page = notificationService.findPage(user.getId(), false, NotificationSort.NEWEST, null);
        assertFalse(page.items().isEmpty());
        assertEquals(expectedType, page.items().getFirst().event().getType());
        assertEquals(expectedMessage, page.items().getFirst().message());
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
}
