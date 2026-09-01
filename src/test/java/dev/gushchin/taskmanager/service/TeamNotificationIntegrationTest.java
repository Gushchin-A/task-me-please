package dev.gushchin.taskmanager.service;

import static dev.gushchin.taskmanager.jooq.Tables.NOTIFICATION_EVENTS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAMS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_MEMBERS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_TAGS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static dev.gushchin.taskmanager.jooq.Tables.USER_NOTIFICATIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.model.NotificationEventType;
import dev.gushchin.taskmanager.model.NotificationSort;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamTag;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.view.NotificationPage;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TeamNotificationIntegrationTest extends IntegrationTestBase {
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
    private TeamTagService teamTagService;

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
    void teamAndTagActionsShouldNotifyAllTeamMembers() {
        User owner = createUser("owner@test.com", "Owner");
        User member = createUser("member@test.com", "Member");
        Team team = teamService.create("Team", owner.getId(), List.of());

        assertLatestType(owner, NotificationEventType.TEAM_CREATED);

        teamMemberService.addMember(team.getId(), member.getId());
        teamService.rename(team.getId(), "Renamed team", owner.getId());

        assertLatestType(owner, NotificationEventType.TEAM_RENAMED);
        assertLatestType(member, NotificationEventType.TEAM_RENAMED);

        TeamTag tag = teamTagService.create(team.getId(), "Work", owner.getId());
        assertLatestType(member, NotificationEventType.TEAM_TAG_CREATED);

        teamTagService.rename(tag.getId(), team.getId(), "Important", owner.getId());
        assertLatestType(member, NotificationEventType.TEAM_TAG_RENAMED);

        teamTagService.delete(tag.getId(), team.getId(), owner.getId());
        assertLatestType(member, NotificationEventType.TEAM_TAG_DELETED);

        teamService.delete(team.getId(), owner.getId());
        assertLatestType(owner, NotificationEventType.TEAM_DELETED);
        assertLatestType(member, NotificationEventType.TEAM_DELETED);
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
