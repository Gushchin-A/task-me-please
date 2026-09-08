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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamMemberService {
    private final TeamMemberRepository teamMemberRepository;
    private final TeamService teamService;
    private final UserService userService;
    private final NotificationPublisher notificationPublisher;
    private final TeamEmailService teamEmailService;

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

    public boolean isActiveMember(Long teamId, UUID userId) {
        teamService.findById(teamId);
        userService.findById(userId);

        TeamMember teamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);

        return teamMember != null && !teamMember.isDeleted();
    }

    public TeamMember addMember(Long teamId, UUID userId) {
        teamService.findById(teamId);
        userService.findById(userId);

        TeamMember existingTeamMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
        if (existingTeamMember != null && !existingTeamMember.isDeleted()) {
            throw new TeamMemberAlreadyExistsException(teamId, userId);
        }

        Instant now = Instant.now();

        if (existingTeamMember != null) {
            return teamMemberRepository.restoreMember(teamId, userId, now);
        }

        TeamMember teamMember =
                new TeamMember(teamId, userId, TeamMemberRole.MEMBER, TeamTaskVisibility.OWN_TASKS, now);

        return teamMemberRepository.save(teamMember);
    }

    @Transactional
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

        if (targetMember.getTaskVisibility() == taskVisibility) {
            return targetMember;
        }

        TeamMember updatedMember = teamMemberRepository.updateTaskVisibility(teamId, userId, taskVisibility);
        notificationPublisher.taskVisibilityChanged(targetMember, updatedMember, currentUserId);
        if (taskVisibility == TeamTaskVisibility.ALL_TASKS) {
            teamEmailService.sendAllTasksVisible(userId, teamService.findById(teamId));
        } else {
            teamEmailService.sendOwnTasksVisibleOnly(userId, teamService.findById(teamId));
        }

        return updatedMember;
    }

    @Transactional
    public TeamMember removeMember(Long teamId, UUID userId, UUID currentUserId) {
        TeamMember currentMember = findById(teamId, currentUserId);

        if (currentMember.getRole() != TeamMemberRole.OWNER) {
            throw new AccessDeniedForTaskException();
        }

        TeamMember targetMember = findById(teamId, userId);

        if (targetMember.getRole() == TeamMemberRole.OWNER) {
            throw new AccessDeniedForTaskException();
        }

        if (targetMember.getUserId().equals(currentUserId)) {
            throw new AccessDeniedForTaskException();
        }

        TeamMember removedMember = teamMemberRepository.softDelete(teamId, userId);
        notificationPublisher.teamMemberRemoved(removedMember, currentUserId);
        teamEmailService.sendMemberRemoved(userId, teamService.findById(teamId));

        return removedMember;
    }

    @Transactional
    public TeamMember leaveTeam(Long teamId, UUID currentUserId) {
        TeamMember currentMember = findById(teamId, currentUserId);

        if (currentMember.getRole() == TeamMemberRole.OWNER) {
            throw new AccessDeniedForTaskException();
        }

        TeamMember removedMember = teamMemberRepository.softDelete(teamId, currentUserId);
        notificationPublisher.teamMemberLeft(removedMember);
        teamEmailService.sendMemberLeft(
                userService.findById(currentUserId), teamService.findById(teamId), findOwnerId(teamId));

        return removedMember;
    }

    private UUID findOwnerId(Long teamId) {
        for (TeamMember member : teamMemberRepository.findByTeamId(teamId)) {
            if (!member.isDeleted() && member.getRole() == TeamMemberRole.OWNER) {
                return member.getUserId();
            }
        }

        return null;
    }
}
