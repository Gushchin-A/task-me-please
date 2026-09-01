package dev.gushchin.taskmanager.service;

import static dev.gushchin.taskmanager.jooq.Tables.COMMENTS;
import static dev.gushchin.taskmanager.jooq.Tables.NOTIFICATION_EVENTS;
import static dev.gushchin.taskmanager.jooq.Tables.TASKS;
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
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamTag;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.view.NotificationPage;
import java.time.LocalDate;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TaskNotificationIntegrationTest extends IntegrationTestBase {
    private static final String PASSWORD = "qwerty";

    @Autowired
    private CommentService commentService;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskService taskService;

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
    void taskAndCommentActionsShouldNotifyOnlyRelevantTaskParticipants() {
        User owner = createUser("owner@test.com", "Owner");
        User author = createUser("author@test.com", "Author");
        User firstAssignee = createUser("first-assignee@test.com", "First Assignee");
        User secondAssignee = createUser("second-assignee@test.com", "Second Assignee");
        User otherMember = createUser("other@test.com", "Other");
        Team team = teamService.create("Team", owner.getId(), List.of("Work"));
        teamMemberService.addMember(team.getId(), author.getId());
        teamMemberService.addMember(team.getId(), firstAssignee.getId());
        teamMemberService.addMember(team.getId(), secondAssignee.getId());
        teamMemberService.addMember(team.getId(), otherMember.getId());
        TeamTag tag = teamTagService.findByTeamId(team.getId()).getFirst();

        final Task task = taskService.create(
                team.getId(), author.getId(), firstAssignee.getId(), "Notification task", null, null, tag.getId());

        assertLatestType(owner, NotificationEventType.TASK_CREATED);
        assertLatestType(author, NotificationEventType.TASK_CREATED);
        assertLatestType(firstAssignee, NotificationEventType.TASK_CREATED);
        assertEquals(0, notificationService.getCounts(otherMember.getId()).all());

        taskService.updateAssignee(task.getId(), secondAssignee.getId(), owner.getId());

        assertLatestType(owner, NotificationEventType.TASK_ASSIGNEE_CHANGED);
        assertLatestType(author, NotificationEventType.TASK_ASSIGNEE_CHANGED);
        assertLatestType(firstAssignee, NotificationEventType.TASK_ASSIGNEE_CHANGED);
        assertLatestType(secondAssignee, NotificationEventType.TASK_ASSIGNEE_CHANGED);

        commentService.create(task.getId(), author.getId(), "Comment");

        assertLatestType(owner, NotificationEventType.COMMENT_CREATED);
        assertLatestType(author, NotificationEventType.COMMENT_CREATED);
        assertLatestType(secondAssignee, NotificationEventType.COMMENT_CREATED);
        assertLatestType(firstAssignee, NotificationEventType.TASK_ASSIGNEE_CHANGED);
        assertEquals(0, notificationService.getCounts(otherMember.getId()).all());
    }

    @Test
    void deniedTaskActionShouldNotCreateNotification() {
        User owner = createUser("owner@test.com", "Owner");
        User author = createUser("author@test.com", "Author");
        User assignee = createUser("assignee@test.com", "Assignee");
        User otherMember = createUser("other@test.com", "Other");
        Team team = teamService.create("Team", owner.getId(), List.of("Work"));
        teamMemberService.addMember(team.getId(), author.getId());
        teamMemberService.addMember(team.getId(), assignee.getId());
        teamMemberService.addMember(team.getId(), otherMember.getId());
        TeamTag tag = teamTagService.findByTeamId(team.getId()).getFirst();
        Task task = taskService.create(
                team.getId(), author.getId(), assignee.getId(), "Notification task", null, null, tag.getId());
        int eventsBeforeDeniedAction = dsl.fetchCount(NOTIFICATION_EVENTS);

        assertThrows(
                AccessDeniedForTaskException.class,
                () -> taskService.updateStatus(task.getId(), TaskStatus.DONE, otherMember.getId()));

        assertEquals(eventsBeforeDeniedAction, dsl.fetchCount(NOTIFICATION_EVENTS));
    }

    @Test
    void statusAndDeadlineNotificationsShouldUseRussianDisplayValues() {
        User owner = createUser("owner@test.com", "Owner");
        Team team = teamService.create("Team", owner.getId(), List.of("Work"));
        TeamTag tag = teamTagService.findByTeamId(team.getId()).getFirst();
        Task task = taskService.create(
                team.getId(),
                owner.getId(),
                owner.getId(),
                "Notification task",
                null,
                LocalDate.of(2026, 9, 8),
                tag.getId());

        taskService.updateStatus(task.getId(), TaskStatus.IN_PROGRESS, owner.getId());

        assertLatestMessage(owner, "Вы изменили статус задачи «Notification task»: Открыто → В работе");

        taskService.updateDeadline(task.getId(), LocalDate.of(2026, 9, 9), owner.getId());

        assertLatestMessage(owner, "Вы изменили дедлайн задачи «Notification task»: 9 сентября 2026");
    }

    @Test
    void completedTaskArchiveShouldUseArchiveTextForEveryRecipient() {
        User owner = createUser("owner@test.com", "Owner");
        User member = createUser("member@test.com", "Member");
        Team team = teamService.create("Team", owner.getId(), List.of("Work"));
        teamMemberService.addMember(team.getId(), member.getId());
        TeamTag tag = teamTagService.findByTeamId(team.getId()).getFirst();
        Task task = taskService.create(
                team.getId(), member.getId(), member.getId(), "Completed task", null, null, tag.getId());
        taskService.updateStatus(task.getId(), TaskStatus.DONE, owner.getId());

        taskService.archive(task.getId(), owner.getId());

        assertLatestMessage(owner, "Вы перенесли в архив задачу «Completed task»");
        assertLatestMessage(member, "Задача «Completed task» перенесена в архив пользователем Owner");
    }

    private void assertLatestType(User user, NotificationEventType expectedType) {
        NotificationPage page = notificationService.findPage(user.getId(), false, NotificationSort.NEWEST, null);
        assertFalse(page.items().isEmpty());
        assertEquals(expectedType, page.items().getFirst().event().getType());
    }

    private void assertLatestMessage(User user, String expectedMessage) {
        NotificationPage page = notificationService.findPage(user.getId(), false, NotificationSort.NEWEST, null);
        assertFalse(page.items().isEmpty());
        assertEquals(expectedMessage, page.items().getFirst().message());
    }

    private User createUser(String email, String name) {
        return userService.create(email, name, PASSWORD);
    }

    private void cleanDatabase() {
        dsl.deleteFrom(USER_NOTIFICATIONS).execute();
        dsl.deleteFrom(NOTIFICATION_EVENTS).execute();
        dsl.deleteFrom(COMMENTS).execute();
        dsl.deleteFrom(TASKS).execute();
        dsl.deleteFrom(TEAM_TAGS).execute();
        dsl.deleteFrom(TEAM_MEMBERS).execute();
        dsl.deleteFrom(TEAMS).execute();
        dsl.deleteFrom(USERS).execute();
    }
}
