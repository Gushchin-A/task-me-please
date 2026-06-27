package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.AccessDeniedForTaskException;
import dev.gushchin.taskmanager.exception.TeamMemberAlreadyExistsException;
import dev.gushchin.taskmanager.exception.TeamMemberNotFoundException;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamMemberRole;
import dev.gushchin.taskmanager.model.TeamTaskVisibility;
import dev.gushchin.taskmanager.repository.TeamMemberRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamMemberService {
    private final TeamMemberRepository teamMemberRepository;
    private final TeamService teamService;
    private final UserService userService;

    public List<TeamMember> findByTeamId(Long teamId) {
        teamService.findById(teamId);

        return teamMemberRepository.findByTeamId(teamId).stream()
                .filter(Predicate.not(TeamMember::isDeleted))
                .toList();
    }

    public TeamMember findById(Long teamId, UUID userId) {
        teamService.findById(teamId);
        userService.findById(userId);

        TeamMember teamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
        if (teamMember == null || teamMember.isDeleted()) {
            throw new TeamMemberNotFoundException(teamId, userId);
        }

        return teamMember;
    }

    public TeamMember addMember(Long teamId, UUID userId) {
        teamService.findById(teamId);
        userService.findById(userId);

        TeamMember existingTeamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
        if (existingTeamMember != null && !existingTeamMember.isDeleted()) {
            throw new TeamMemberAlreadyExistsException(teamId, userId);
        }

        Instant now = Instant.now();

        TeamMember teamMember =
                new TeamMember(teamId, userId, TeamMemberRole.MEMBER, TeamTaskVisibility.OWN_TASKS, now);

        return teamMemberRepository.save(teamMember);
    }

    public TeamMember updateTaskVisibility(
            Long teamId, UUID userId, TeamTaskVisibility taskVisibility, UUID currentUserId) {
        TeamMember currentMember = findById(teamId, currentUserId);

        if (currentMember.getRole() != TeamMemberRole.OWNER) {
            throw new AccessDeniedForTaskException();
        }

        TeamMember targetMember = findById(teamId, userId);

        if (targetMember.getRole() == TeamMemberRole.OWNER) {
            throw new AccessDeniedForTaskException();
        }

        return teamMemberRepository.updateTaskVisibility(teamId, userId, taskVisibility);
    }
}
