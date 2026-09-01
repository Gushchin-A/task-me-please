package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.AccessDeniedForTaskException;
import dev.gushchin.taskmanager.exception.InvalidTeamNameException;
import dev.gushchin.taskmanager.exception.InvalidTeamNameException.Reason;
import dev.gushchin.taskmanager.exception.InvalidTeamTagException;
import dev.gushchin.taskmanager.exception.TeamNotFoundException;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamMemberRole;
import dev.gushchin.taskmanager.model.TeamTag;
import dev.gushchin.taskmanager.model.TeamTaskVisibility;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamMemberRepository;
import dev.gushchin.taskmanager.repository.TeamRepository;
import dev.gushchin.taskmanager.repository.TeamTagRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamService {
    private static final int MAX_TEAM_NAME_LENGTH = 100;
    private static final int MAX_TAGS_PER_TEAM = 20;
    private static final int MAX_TAG_NAME_LENGTH = 30;

    private final TeamRepository teamRepository;
    private final UserService userService;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamTagRepository teamTagRepository;
    private final NotificationPublisher notificationPublisher;

    public Team findById(Long id) {
        Team team = teamRepository.findById(id);
        if (team == null || team.isDeleted()) {
            throw new TeamNotFoundException(id);
        }

        return team;
    }

    public List<Team> findAll() {
        return teamRepository.findAll().stream()
                .filter(Predicate.not(Team::isDeleted))
                .toList();
    }

    public List<Team> findByUserId(UUID userId) {
        return teamMemberRepository.findByUserId(userId).stream()
                .filter(Predicate.not(TeamMember::isDeleted))
                .map(teamMember -> teamRepository.findById(teamMember.getTeamId()))
                .filter(team -> team != null && !team.isDeleted())
                .toList();
    }

    public List<Team> findOwnedByUserId(UUID userId) {
        return teamRepository.findByCreatedBy(userId).stream()
                .filter(Predicate.not(Team::isDeleted))
                .toList();
    }

    @Transactional
    public Team create(String name, UUID createdBy) {
        return create(name, createdBy, List.of());
    }

    @Transactional
    public Team create(String name, UUID createdBy, List<String> tagNames) {
        String preparedName = prepareName(name);
        User creator = userService.findById(createdBy);
        Instant now = Instant.now();

        Team team = new Team(null, preparedName, creator.getId(), now, now, false);

        Team savedTeam = teamRepository.save(team);

        TeamMember owner = new TeamMember(
                savedTeam.getId(), creator.getId(), TeamMemberRole.OWNER, TeamTaskVisibility.ALL_TASKS, now);

        teamMemberRepository.save(owner);

        List<TeamTag> teamTags = prepareTagNames(tagNames).stream()
                .map(tagName -> createTeamTag(savedTeam.getId(), tagName, now))
                .toList();

        teamTags.forEach(teamTagRepository::save);

        notificationPublisher.teamCreated(savedTeam, createdBy);

        return savedTeam;
    }

    private List<String> prepareTagNames(List<String> tagNames) {
        if (tagNames == null) {
            return List.of();
        }

        List<String> preparedNames = tagNames.stream()
                .filter(tagName -> tagName != null && !tagName.isBlank())
                .map(String::trim)
                .toList();

        if (preparedNames.size() > MAX_TAGS_PER_TEAM) {
            throw new InvalidTeamTagException("Team cannot contain more than " + MAX_TAGS_PER_TEAM + " tags");
        }

        for (String tagName : preparedNames) {
            if (tagName.length() > MAX_TAG_NAME_LENGTH) {
                throw new InvalidTeamTagException(
                        "Team tag name must not be longer than " + MAX_TAG_NAME_LENGTH + " characters");
            }
        }

        long normalizedNamesCount = preparedNames.stream()
                .map(tagName -> tagName.toLowerCase(Locale.ROOT))
                .distinct()
                .count();

        if (normalizedNamesCount != preparedNames.size()) {
            throw new InvalidTeamTagException("Team tag names must be unique");
        }

        return preparedNames;
    }

    @Transactional
    public void delete(Long id, UUID currentUserId) {
        Team team = findById(id);
        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(id, currentUserId);

        if (currentMember == null || currentMember.isDeleted() || currentMember.getRole() != TeamMemberRole.OWNER) {
            throw new AccessDeniedForTaskException();
        }

        team.setDeleted(true);
        team.setUpdatedAt(Instant.now());
        teamRepository.update(team);
        notificationPublisher.teamDeleted(team, currentUserId);
    }

    @Transactional
    public Team rename(Long id, String name, UUID currentUserId) {
        final Team team = findById(id);
        TeamMember currentMember = teamMemberRepository.findByTeamIdAndUserId(id, currentUserId);

        if (currentMember == null || currentMember.isDeleted() || currentMember.getRole() != TeamMemberRole.OWNER) {
            throw new AccessDeniedForTaskException();
        }

        String preparedName = prepareName(name);

        if (preparedName.equals(team.getName())) {
            throw new InvalidTeamNameException(Reason.UNCHANGED);
        }

        Team before = new Team(
                team.getId(), team.getName(), team.getCreatedBy(), team.getCreatedAt(), team.getUpdatedAt(), false);
        team.setName(preparedName);
        team.setUpdatedAt(Instant.now());

        Team renamedTeam = teamRepository.update(team);
        notificationPublisher.teamRenamed(before, renamedTeam, currentUserId);

        return renamedTeam;
    }

    private String prepareName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidTeamNameException(Reason.BLANK);
        }

        String preparedName = name.trim();

        if (preparedName.length() > MAX_TEAM_NAME_LENGTH) {
            throw new InvalidTeamNameException(Reason.TOO_LONG);
        }

        return preparedName;
    }

    private TeamTag createTeamTag(Long teamId, String tagName, Instant now) {
        return new TeamTag(teamId, tagName, tagName.toLowerCase(Locale.ROOT), now, now, false);
    }
}
