package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.InvalidTeamTagException;
import dev.gushchin.taskmanager.exception.TeamTagAlreadyExistsException;
import dev.gushchin.taskmanager.exception.TeamTagNotFoundException;
import dev.gushchin.taskmanager.model.TeamTag;
import dev.gushchin.taskmanager.repository.TeamTagRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamTagService {
    private static final int MAX_TAGS_PER_TEAM = 20;
    private static final int MAX_TAG_NAME_LENGTH = 30;

    private final TeamTagRepository teamTagRepository;
    private final TeamService teamService;

    public TeamTag findById(Long id) {
        TeamTag teamTag = teamTagRepository.findById(id);

        if (teamTag == null || teamTag.isDeleted()) {
            throw new TeamTagNotFoundException(id);
        }

        return teamTag;
    }

    public List<TeamTag> findByTeamId(Long teamId) {
        teamService.findById(teamId);

        return findActiveByTeamId(teamId);
    }

    public Set<Long> findUsedIdsByTeamId(Long teamId) {
        teamService.findById(teamId);

        return teamTagRepository.findUsedIdsByTeamId(teamId);
    }

    @Transactional
    public TeamTag create(Long teamId, String name) {
        teamService.findById(teamId);

        String preparedName = prepareName(name);
        String normalizedName = normalizeName(preparedName);

        TeamTag existingTag = teamTagRepository.findActiveByTeamIdAndNormalizedName(teamId, normalizedName);

        if (existingTag != null) {
            throw new TeamTagAlreadyExistsException(teamId, preparedName);
        }

        if (findActiveByTeamId(teamId).size() >= MAX_TAGS_PER_TEAM) {
            throw new InvalidTeamTagException("Team cannot contain more than " + MAX_TAGS_PER_TEAM + " tags");
        }

        Instant now = Instant.now();
        TeamTag teamTag = new TeamTag(teamId, preparedName, normalizedName, now, now, false);

        return teamTagRepository.save(teamTag);
    }

    private List<TeamTag> findActiveByTeamId(Long teamId) {
        return teamTagRepository.findByTeamId(teamId).stream()
                .filter(Predicate.not(TeamTag::isDeleted))
                .toList();
    }

    private String prepareName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidTeamTagException("Team tag name must not be blank");
        }

        String preparedName = name.trim();

        if (preparedName.length() > MAX_TAG_NAME_LENGTH) {
            throw new InvalidTeamTagException(
                    "Team tag name must not be longer than " + MAX_TAG_NAME_LENGTH + " characters");
        }

        return preparedName;
    }

    private String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public TeamTag findByIdForTeam(Long tagId, Long teamId) {
        TeamTag teamTag = findById(tagId);

        if (!teamId.equals(teamTag.getTeamId())) {
            throw new InvalidTeamTagException(
                    "Team tag does not belong to team. Tag id = " + tagId + ", team id = " + teamId);
        }

        return teamTag;
    }

    @Transactional
    public TeamTag rename(Long tagId, Long teamId, String name) {
        TeamTag teamTag = findByIdForTeam(tagId, teamId);
        String preparedName = prepareName(name);
        String normalizedName = normalizeName(preparedName);
        TeamTag existingTag = teamTagRepository.findActiveByTeamIdAndNormalizedName(teamId, normalizedName);

        if (existingTag != null && !existingTag.getId().equals(tagId)) {
            throw new TeamTagAlreadyExistsException(teamId, preparedName);
        }

        teamTag.setName(preparedName);
        teamTag.setNormalizedName(normalizedName);
        teamTag.setUpdatedAt(Instant.now());

        return teamTagRepository.update(teamTag);
    }

    @Transactional
    public void delete(Long tagId, Long teamId) {
        TeamTag teamTag = findByIdForTeam(tagId, teamId);

        if (teamTagRepository.isUsed(tagId)) {
            throw new InvalidTeamTagException("Assigned team tag cannot be deleted");
        }

        teamTag.setDeleted(true);
        teamTag.setUpdatedAt(Instant.now());
        teamTagRepository.update(teamTag);
    }
}
