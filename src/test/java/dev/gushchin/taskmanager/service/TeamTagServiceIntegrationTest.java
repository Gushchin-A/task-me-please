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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.gushchin.taskmanager.IntegrationTestBase;
import dev.gushchin.taskmanager.exception.TeamTagAlreadyExistsException;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamTag;
import dev.gushchin.taskmanager.model.User;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TeamTagServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamTagService teamTagService;

    private Team team;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        User owner = userService.create("tag-owner@test.com", "Owner", "qwerty");
        team = teamService.create("Project Team", owner.getId());
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void createShouldTrimNormalizeAndSaveTag() {
        TeamTag createdTag = teamTagService.create(team.getId(), "  Кинопоиск  ");

        assertNotNull(createdTag.getId());
        assertEquals(team.getId(), createdTag.getTeamId());
        assertEquals("Кинопоиск", createdTag.getName());
        assertEquals("кинопоиск", createdTag.getNormalizedName());
        assertFalse(createdTag.isDeleted());

        List<TeamTag> teamTags = teamTagService.findByTeamId(team.getId());

        assertEquals(1, teamTags.size());
        assertEquals(createdTag.getId(), teamTags.getFirst().getId());
    }

    @Test
    void createShouldRejectDuplicateNormalizedName() {
        teamTagService.create(team.getId(), "Кинопоиск");

        assertThrows(TeamTagAlreadyExistsException.class, () -> teamTagService.create(team.getId(), "  КИНОПОИСК  "));
    }

    @Test
    void renameShouldUpdateTagWithoutChangingItsId() {
        TeamTag tag = teamTagService.create(team.getId(), "Кинопоиск");

        TeamTag renamedTag = teamTagService.rename(tag.getId(), team.getId(), "  Видео  ");

        assertEquals(tag.getId(), renamedTag.getId());
        assertEquals("Видео", renamedTag.getName());
        assertEquals("видео", renamedTag.getNormalizedName());
    }

    @Test
    void renameShouldRejectDuplicateNormalizedName() {
        TeamTag tag = teamTagService.create(team.getId(), "Кинопоиск");
        teamTagService.create(team.getId(), "Музыка");

        assertThrows(
                TeamTagAlreadyExistsException.class,
                () -> teamTagService.rename(tag.getId(), team.getId(), "  МУЗЫКА  "));
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
