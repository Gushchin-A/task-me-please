package dev.gushchin.taskmanager.service;

import static dev.gushchin.taskmanager.jooq.Tables.NOTIFICATION_EVENTS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAMS;
import static dev.gushchin.taskmanager.jooq.Tables.TEAM_MEMBERS;
import static dev.gushchin.taskmanager.jooq.Tables.USERS;
import static dev.gushchin.taskmanager.jooq.Tables.USER_NOTIFICATIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.model.NotificationRecipients;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.User;
import java.time.Instant;
import java.util.Set;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificationRecipientPolicyIntegrationTest extends IntegrationTestBase {
    private static final String PASSWORD = "qwerty";

    @Autowired
    private DSLContext dsl;

    @Autowired
    private NotificationRecipientPolicy notificationRecipientPolicy;

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
    void forTaskShouldIncludeOwnerActorAuthorAndAssigneeWithoutOtherMembers() {
        User owner = createUser("owner@test.com", "Owner");
        User author = createUser("author@test.com", "Author");
        User assignee = createUser("assignee@test.com", "Assignee");
        User actor = createUser("actor@test.com", "Actor");
        User otherMember = createUser("other@test.com", "Other");
        Team team = teamService.create("Team", owner.getId());
        teamMemberService.addMember(team.getId(), author.getId());
        teamMemberService.addMember(team.getId(), assignee.getId());
        teamMemberService.addMember(team.getId(), actor.getId());
        teamMemberService.addMember(team.getId(), otherMember.getId());
        Task task = new Task(
                1L,
                team.getId(),
                author.getId(),
                assignee.getId(),
                "Task",
                null,
                null,
                TaskStatus.OPEN,
                null,
                Instant.now(),
                Instant.now(),
                null,
                false,
                false);

        NotificationRecipients recipients = notificationRecipientPolicy.forTask(task, actor.getId());

        assertEquals(Set.of(owner.getId(), author.getId(), assignee.getId(), actor.getId()), recipients.userIds());
        assertEquals(Set.of(), recipients.emails());
    }

    @Test
    void forInvitationShouldPreferExistingUserAndKeepUnknownNormalizedEmail() {
        User owner = createUser("owner@test.com", "Owner");
        User existingUser = createUser("existing@test.com", "Existing");

        NotificationRecipients existingRecipients =
                notificationRecipientPolicy.forInvitation(owner.getId(), " EXISTING@test.com ");
        NotificationRecipients unknownRecipients =
                notificationRecipientPolicy.forInvitation(owner.getId(), " UNKNOWN@test.com ");

        assertEquals(Set.of(owner.getId(), existingUser.getId()), existingRecipients.userIds());
        assertEquals(Set.of(), existingRecipients.emails());
        assertEquals(Set.of(owner.getId()), unknownRecipients.userIds());
        assertEquals(Set.of("unknown@test.com"), unknownRecipients.emails());
    }

    private User createUser(String email, String name) {
        return userService.create(email, name, PASSWORD);
    }

    private void cleanDatabase() {
        dsl.deleteFrom(USER_NOTIFICATIONS).execute();
        dsl.deleteFrom(NOTIFICATION_EVENTS).execute();
        dsl.deleteFrom(TEAM_MEMBERS).execute();
        dsl.deleteFrom(TEAMS).execute();
        dsl.deleteFrom(USERS).execute();
    }
}
