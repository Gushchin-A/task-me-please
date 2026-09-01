package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.model.NotificationRecipients;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamMemberRole;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamMemberRepository;
import dev.gushchin.taskmanager.repository.UserRepository;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationRecipientPolicy {
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public NotificationRecipients forTask(Task task, UUID actorUserId) {
        Set<UUID> userIds = activeMemberIds(task.getTeamId());
        userIds.removeIf(userId -> !isTaskRecipient(userId, task, actorUserId));

        return new NotificationRecipients(userIds, Set.of());
    }

    public NotificationRecipients forTaskChange(Task before, Task after, UUID actorUserId) {
        Set<UUID> userIds = new LinkedHashSet<>(forTask(before, actorUserId).userIds());
        userIds.addAll(forTask(after, actorUserId).userIds());

        return new NotificationRecipients(userIds, Set.of());
    }

    public NotificationRecipients forAllTeamMembers(Long teamId, UUID... additionalUserIds) {
        Set<UUID> userIds = activeMemberIds(teamId);
        addUsers(userIds, additionalUserIds);

        return new NotificationRecipients(userIds, Set.of());
    }

    public NotificationRecipients forAllTeamMembersExcept(Long teamId, UUID... excludedUserIds) {
        Set<UUID> userIds = activeMemberIds(teamId);
        Arrays.stream(excludedUserIds).filter(java.util.Objects::nonNull).forEach(userIds::remove);

        return new NotificationRecipients(userIds, Set.of());
    }

    public NotificationRecipients forUsers(UUID... userIds) {
        Set<UUID> recipients = new LinkedHashSet<>();
        addUsers(recipients, userIds);

        return new NotificationRecipients(recipients, Set.of());
    }

    public NotificationRecipients forInvitation(UUID ownerId, String invitedEmail) {
        Set<UUID> userIds = new LinkedHashSet<>();
        userIds.add(ownerId);
        Set<String> emails = new LinkedHashSet<>();
        String normalizedEmail = normalizeEmail(invitedEmail);
        User invitedUser = userRepository.findByEmail(normalizedEmail);

        if (invitedUser == null || invitedUser.isDeleted()) {
            emails.add(normalizedEmail);
        } else {
            userIds.add(invitedUser.getId());
        }

        return new NotificationRecipients(userIds, emails);
    }

    private Set<UUID> activeMemberIds(Long teamId) {
        Set<UUID> userIds = new LinkedHashSet<>();
        teamMemberRepository.findByTeamId(teamId).stream()
                .filter(Predicate.not(TeamMember::isDeleted))
                .map(TeamMember::getUserId)
                .forEach(userIds::add);

        return userIds;
    }

    private boolean isTaskRecipient(UUID userId, Task task, UUID actorUserId) {
        return userId.equals(actorUserId)
                || userId.equals(task.getAuthorId())
                || userId.equals(task.getAssigneeId())
                || isOwner(task.getTeamId(), userId);
    }

    private boolean isOwner(Long teamId, UUID userId) {
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
        return member != null && member.getRole() == TeamMemberRole.OWNER;
    }

    private void addUsers(Set<UUID> recipients, UUID... userIds) {
        Arrays.stream(userIds).filter(java.util.Objects::nonNull).forEach(recipients::add);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
