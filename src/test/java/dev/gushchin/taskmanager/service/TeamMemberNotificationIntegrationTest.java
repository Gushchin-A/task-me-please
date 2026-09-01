package dev.gushchin.taskmanager.service;

import static dev.gushchin.taskmanager.jooq.Tables.NOTIFICATION_EVENTS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAMS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_MEMBERS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_TAGS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static dev.gushchin.taskmanager.jooq.Tables.USER_NOTIFICATIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.exception.AccessDeniedForTaskException;
import dev.gushchin.taskmanager.model.NotificationEventType;
import dev.gushchin.taskmanager.model.NotificationSort;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamTaskVisibility;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.view.NotificationPage;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TeamMemberNotificationIntegrationTest extends IntegrationTestBase {
    private static final String PASSWORD = "qwerty";

    @Autowired
    private DSLContext dsl;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void visibilityChangeShouldNotifyOwnerAndAffectedMemberOnly() {
        User owner = createUser("owner@test.com", "Owner");
        User affectedMember = createUser("affected@test.com", "Affected");
        User otherMember = createUser("other@test.com", "Other");
        Team team = teamService.create("Team", owner.getId(), List.of("Work"));
        teamMemberService.addMember(team.getId(), affectedMember.getId());
        teamMemberService.addMember(team.getId(), otherMember.getId());

        teamMemberService.updateTaskVisibility(
                team.getId(), affectedMember.getId(), TeamTaskVisibility.ALL_TASKS, owner.getId());

        assertLatestType(owner, NotificationEventType.TEAM_TASK_VISIBILITY_GRANTED);
        assertLatestType(affectedMember, NotificationEventType.TEAM_TASK_VISIBILITY_GRANTED);
        assertEquals(0, notificationService.getCounts(otherMember.getId()).all());
    }

    @Test
    void removingMemberShouldNotifyRemainingMembersAndRemovedMember() {
        User owner = createUser("owner@test.com", "Owner");
        User removedMember = createUser("removed@test.com", "Removed");
        User otherMember = createUser("other@test.com", "Other");
        Team team = teamService.create("Team", owner.getId(), List.of("Work"));
        teamMemberService.addMember(team.getId(), removedMember.getId());
        teamMemberService.addMember(team.getId(), otherMember.getId());

        teamMemberService.removeMember(team.getId(), removedMember.getId(), owner.getId());

        assertLatestType(owner, NotificationEventType.TEAM_MEMBER_REMOVED);
        assertLatestType(removedMember, NotificationEventType.TEAM_MEMBER_REMOVED);
        assertLatestType(otherMember, NotificationEventType.TEAM_MEMBER_REMOVED);
    }

    @Test
    void leavingTeamShouldNotifyRemainingMembersAndFormerMember() {
        User owner = createUser("owner@test.com", "Owner");
        User leavingMember = createUser("leaving@test.com", "Leaving");
        Team team = teamService.create("Team", owner.getId(), List.of("Work"));
        teamMemberService.addMember(team.getId(), leavingMember.getId());

        teamMemberService.leaveTeam(team.getId(), leavingMember.getId());

        assertLatestType(owner, NotificationEventType.TEAM_MEMBER_LEFT);
        assertLatestType(leavingMember, NotificationEventType.TEAM_MEMBER_LEFT);
    }

    @Test
    void deniedVisibilityChangeShouldNotCreateNotification() {
        User owner = createUser("owner@test.com", "Owner");
        User member = createUser("member@test.com", "Member");
        User otherMember = createUser("other@test.com", "Other");
        Team team = teamService.create("Team", owner.getId(), List.of("Work"));
        teamMemberService.addMember(team.getId(), member.getId());
        teamMemberService.addMember(team.getId(), otherMember.getId());
        int eventsBeforeDeniedAction = dsl.fetchCount(NOTIFICATION_EVENTS);

        assertThrows(
                AccessDeniedForTaskException.class,
                () -> teamMemberService.updateTaskVisibility(
                        team.getId(), member.getId(), TeamTaskVisibility.ALL_TASKS, otherMember.getId()));

        assertEquals(eventsBeforeDeniedAction, dsl.fetchCount(NOTIFICATION_EVENTS));
    }

    private void assertLatestType(User user, NotificationEventType expectedType) {
        NotificationPage page = notificationService.findPage(user.getId(), false, NotificationSort.NEWEST, null);
        assertFalse(page.items().isEmpty());
        assertEquals(expectedType, page.items().getFirst().event().getType());
    }

    private User createUser(String email, String name) {
        return userService.create(email, name, PASSWORD);
    }

    private void cleanDatabase() {
        dsl.deleteFrom(USER_NOTIFICATIONS).execute();
        dsl.deleteFrom(NOTIFICATION_EVENTS).execute();
        dsl.deleteFrom(TEAM_TAGS).execute();
        dsl.deleteFrom(TEAM_MEMBERS).execute();
        dsl.deleteFrom(TEAMS).execute();
        dsl.deleteFrom(USERS).execute();
    }
}
