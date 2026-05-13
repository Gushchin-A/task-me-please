package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.TeamNotFoundException;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamMemberRole;
import dev.gushchin.taskmanager.repository.TeamMemberRepository;
import dev.gushchin.taskmanager.repository.TeamRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final UserService userService;
    private final TeamMemberRepository teamMemberRepository;

    public Team findById(Long id) {
        Team team = teamRepository.findById(id);
        if (team == null) {
            throw new TeamNotFoundException(id);
        }

        return team;
    }

    public List<Team> findAll() {
        return teamRepository.findAll().stream()
                .filter(Predicate.not(Team::isDeleted))
                .toList();
    }

    @Transactional
    public Team create(String name, UUID createdBy) {
        userService.findById(createdBy);

        Instant now = Instant.now();

        Team team = new Team(name, createdBy, now, now, false);
        Team savedTeam = teamRepository.save(team);

        TeamMember owner = new TeamMember(savedTeam.getId(), createdBy, TeamMemberRole.OWNER, now, now, now, false);
        teamMemberRepository.save(owner);

        return savedTeam;
    }

    public void deleteById(Long id) {
        Team team = findById(id);
        team.setDeleted(true);
        team.setUpdatedAt(Instant.now());
        teamRepository.update(team);
    }
}
