package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.gushchin.taskmanager.exception.AccessDeniedForTaskException;
import dev.gushchin.taskmanager.exception.InvalidTeamNameException;
import dev.gushchin.taskmanager.exception.TeamNotFoundException;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamMemberRole;
import dev.gushchin.taskmanager.model.TeamTag;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamMemberRepository;
import dev.gushchin.taskmanager.repository.TeamRepository;
import dev.gushchin.taskmanager.repository.TeamTagRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TeamServiceTest {
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final UserService userService = mock(UserService.class);
    private final TeamMemberRepository teamMemberRepository = mock(TeamMemberRepository.class);
    private final TeamTagRepository teamTagRepository = mock(TeamTagRepository.class);

    private final TeamService teamService =
            new TeamService(teamRepository, userService, teamMemberRepository, teamTagRepository);

    @Test
    void createShouldReturnSavedTeam() {
        // given
        final UUID createdBy = UUID.randomUUID();
        User creator = new User();
        creator.setId(createdBy);

        when(userService.findById(createdBy)).thenReturn(creator);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocationOnMock -> {
            Team team = invocationOnMock.getArgument(0);
            team.setId(1L);

            return team;
        });

        when(teamMemberRepository.save(any(TeamMember.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // when
        Team createdTeam = teamService.create("My Team", createdBy);

        // then
        assertNotNull(createdTeam.getId());
        assertEquals("My Team", createdTeam.getName());
        assertEquals(createdBy, createdTeam.getCreatedBy());
        assertFalse(createdTeam.isDeleted());

        verify(userService).findById(createdBy);
        verify(teamRepository).save(any(Team.class));
        verify(teamMemberRepository).save(any(TeamMember.class));
        verifyNoInteractions(teamTagRepository);
    }

    @Test
    void createShouldAddOwnerMembership() {
        // given
        UUID createdBy = UUID.randomUUID();

        when(userService.findById(createdBy)).thenReturn(new User());
        when(teamRepository.save(any(Team.class))).thenAnswer(invocationOnMock -> {
            Team team = invocationOnMock.getArgument(0);
            team.setId(10L);
            return team;
        });
        when(teamMemberRepository.save(any(TeamMember.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // when
        teamService.create("Owners", createdBy);

        // then
        verify(teamMemberRepository).save(any(TeamMember.class));
    }

    @Test
    void createShouldRejectNameLongerThanOneHundredCharacters() {
        String longName = "a".repeat(101);

        assertThrows(InvalidTeamNameException.class, () -> teamService.create(longName, UUID.randomUUID()));

        verifyNoInteractions(userService, teamRepository, teamMemberRepository, teamTagRepository);
    }

    @Test
    void findByIdShouldReturnTeam() {
        // given
        Team expectedTeam = new Team();
        expectedTeam.setId(5L);
        expectedTeam.setName("Alpha");

        when(teamRepository.findById(5L)).thenReturn(expectedTeam);

        // when
        Team actualTeam = teamService.findById(5L);

        // then
        assertEquals(expectedTeam, actualTeam);
        verify(teamRepository).findById(5L);
    }

    @Test
    void findByIdShouldThrowTeamNotFound() {
        when(teamRepository.findById(5L)).thenReturn(null);

        assertThrows(TeamNotFoundException.class, () -> teamService.findById(5L));
        verify(teamRepository).findById(5L);
    }

    @Test
    void findByIdShouldThrowTeamNotFoundForDeletedTeam() {
        Team deletedTeam = new Team();
        deletedTeam.setId(5L);
        deletedTeam.setDeleted(true);
        when(teamRepository.findById(5L)).thenReturn(deletedTeam);

        assertThrows(TeamNotFoundException.class, () -> teamService.findById(5L));
    }

    @Test
    void findAllShouldReturnOnlyNotDeletedTeams() {
        // given
        Team activeTeam = new Team();
        activeTeam.setId(1L);
        activeTeam.setName("Active");
        activeTeam.setDeleted(false);

        Team deletedTeam = new Team();
        deletedTeam.setId(2L);
        deletedTeam.setName("Deleted");
        deletedTeam.setDeleted(true);

        when(teamRepository.findAll()).thenReturn(List.of(activeTeam, deletedTeam));

        // when
        List<Team> teams = teamService.findAll();

        // then
        assertEquals(1, teams.size());
        assertEquals("Active", teams.getFirst().getName());
        verify(teamRepository).findAll();
    }

    @Test
    void deleteShouldMarkTeamAsDeletedForOwner() {
        // given
        final UUID ownerId = UUID.randomUUID();
        Team existingTeam = new Team();
        existingTeam.setId(7L);
        existingTeam.setName("Delete Me");
        existingTeam.setDeleted(false);
        TeamMember owner = new TeamMember();
        owner.setRole(TeamMemberRole.OWNER);

        when(teamRepository.findById(7L)).thenReturn(existingTeam);
        when(teamMemberRepository.findByTeamIdAndUserId(7L, ownerId)).thenReturn(owner);
        when(teamRepository.update(any(Team.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // when
        teamService.delete(7L, ownerId);

        // then
        assertTrue(existingTeam.isDeleted());
        verify(teamRepository).findById(7L);
        verify(teamRepository).update(existingTeam);
    }

    @Test
    void deleteShouldRejectMember() {
        final UUID memberId = UUID.randomUUID();
        Team existingTeam = new Team();
        existingTeam.setId(7L);
        TeamMember member = new TeamMember();
        member.setRole(TeamMemberRole.MEMBER);

        when(teamRepository.findById(7L)).thenReturn(existingTeam);
        when(teamMemberRepository.findByTeamIdAndUserId(7L, memberId)).thenReturn(member);

        assertThrows(AccessDeniedForTaskException.class, () -> teamService.delete(7L, memberId));
        verify(teamRepository, never()).update(any());
    }

    @Test
    void createWithTagsShouldSaveTeamTags() {
        // given
        final UUID createdBy = UUID.randomUUID();
        User creator = new User();
        creator.setId(createdBy);

        when(userService.findById(createdBy)).thenReturn(creator);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocationOnMock -> {
            Team savedTeam = invocationOnMock.getArgument(0);
            savedTeam.setId(10L);

            return savedTeam;
        });

        when(teamMemberRepository.save(any(TeamMember.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        when(teamTagRepository.save(any(TeamTag.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // when
        teamService.create("Project Team", createdBy, List.of("Кинопоиск", "Плюс"));

        // then
        ArgumentCaptor<TeamTag> tagCaptor = ArgumentCaptor.forClass(TeamTag.class);
        verify(teamTagRepository, times(2)).save(tagCaptor.capture());

        List<TeamTag> savedTags = tagCaptor.getAllValues();
        final TeamTag firstTag = savedTags.get(0);
        final TeamTag secondTag = savedTags.get(1);

        assertEquals(10L, firstTag.getTeamId());
        assertEquals("Кинопоиск", firstTag.getName());
        assertEquals("кинопоиск", firstTag.getNormalizedName());
        assertFalse(firstTag.isDeleted());

        assertEquals(10L, secondTag.getTeamId());
        assertEquals("Плюс", secondTag.getName());
        assertEquals("плюс", secondTag.getNormalizedName());
        assertFalse(secondTag.isDeleted());

        verify(userService).findById(createdBy);
        verify(teamRepository).save(any(Team.class));
        verify(teamMemberRepository).save(any(TeamMember.class));
    }
}
