package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.gushchin.taskmanager.exception.TeamNotFoundException;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamMemberRepository;
import dev.gushchin.taskmanager.repository.TeamRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamServiceTest {
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final UserService userService = mock(UserService.class);
    private final TeamMemberRepository teamMemberRepository = mock(TeamMemberRepository.class);

    private final TeamService teamService = new TeamService(teamRepository, userService, teamMemberRepository);

    @Test
    void createShouldReturnSavedTeam() {
        // given
        UUID createdBy = UUID.randomUUID();

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
    void deleteByIdShouldMarkTeamAsDeletedAndCallUpdate() {
        // given
        Team existingTeam = new Team();
        existingTeam.setId(7L);
        existingTeam.setName("Delete Me");
        existingTeam.setDeleted(false);

        when(teamRepository.findById(7L)).thenReturn(existingTeam);
        when(teamRepository.update(any(Team.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // when
        teamService.deleteById(7L);

        // then
        assertTrue(existingTeam.isDeleted());
        verify(teamRepository).findById(7L);
        verify(teamRepository).update(existingTeam);
    }
}
