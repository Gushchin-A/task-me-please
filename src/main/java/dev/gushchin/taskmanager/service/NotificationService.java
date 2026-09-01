package dev.gushchin.taskmanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gushchin.taskmanager.model.CommentNotificationPayload;
import dev.gushchin.taskmanager.model.InvitationNotificationPayload;
import dev.gushchin.taskmanager.model.NotificationEvent;
import dev.gushchin.taskmanager.model.NotificationEventCommand;
import dev.gushchin.taskmanager.model.NotificationRecipients;
import dev.gushchin.taskmanager.model.NotificationSort;
import dev.gushchin.taskmanager.model.TaskNotificationPayload;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;
import dev.gushchin.taskmanager.model.TeamNotificationPayload;
import dev.gushchin.taskmanager.model.UserNotification;
import dev.gushchin.taskmanager.repository.NotificationEventRepository;
import dev.gushchin.taskmanager.repository.TeamInvitationRepository;
import dev.gushchin.taskmanager.repository.UserNotificationRepository;
import dev.gushchin.taskmanager.view.NotificationCounts;
import dev.gushchin.taskmanager.view.NotificationFeedItem;
import dev.gushchin.taskmanager.view.NotificationInvitationAction;
import dev.gushchin.taskmanager.view.NotificationPage;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    public static final int PAGE_SIZE = 25;

    private final NotificationEventRepository notificationEventRepository;
    private final NotificationMessageFormatter notificationMessageFormatter;
    private final ObjectMapper objectMapper;
    private final TeamInvitationRepository teamInvitationRepository;
    private final UserNotificationRepository userNotificationRepository;

    public NotificationPage findPage(UUID userId, boolean unreadOnly, NotificationSort sort, Long cursor) {
        NotificationSort resolvedSort = sort == null ? NotificationSort.NEWEST : sort;
        List<NotificationFeedItem> fetchedItems =
                userNotificationRepository.findPage(userId, unreadOnly, resolvedSort, cursor, PAGE_SIZE + 1);
        boolean hasNextPage = fetchedItems.size() > PAGE_SIZE;
        List<NotificationFeedItem> items = (hasNextPage ? fetchedItems.subList(0, PAGE_SIZE) : fetchedItems)
                .stream()
                        .map(item -> new NotificationFeedItem(
                                item.notification(),
                                item.event(),
                                notificationMessageFormatter.format(item.event(), userId),
                                getInvitationAction(item, userId)))
                        .toList();
        Long nextCursor = hasNextPage ? items.getLast().notification().getId() : null;

        return new NotificationPage(List.copyOf(items), nextCursor);
    }

    public NotificationCounts getCounts(UUID userId) {
        return new NotificationCounts(
                userNotificationRepository.countByUserId(userId, false),
                userNotificationRepository.countByUserId(userId, true));
    }

    public int markAsRead(UUID userId, List<Long> notificationIds) {
        return userNotificationRepository.markAsRead(userId, notificationIds, Instant.now());
    }

    @Transactional
    public void claimInvitations(UUID userId, String email) {
        userNotificationRepository.claimByEmail(userId, normalizeEmail(email));
    }

    @Transactional
    public NotificationEvent publish(NotificationEventCommand command, NotificationRecipients recipients) {
        validatePayload(command);
        NotificationEvent event = new NotificationEvent(
                null,
                command.getType(),
                command.getActorUserId(),
                command.getTeamId(),
                command.getTaskId(),
                command.getCommentId(),
                command.getInvitationId(),
                command.getSubjectUserId(),
                serializePayload(command),
                command.getCreatedAt());
        NotificationEvent savedEvent = notificationEventRepository.save(event);
        Instant createdAt = savedEvent.getCreatedAt();

        new LinkedHashSet<>(recipients.userIds())
                .forEach(recipientUserId -> userNotificationRepository.save(
                        new UserNotification(null, savedEvent.getId(), recipientUserId, null, null, createdAt)));

        recipients.emails().stream()
                .map(this::normalizeEmail)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .forEach(recipientEmail -> userNotificationRepository.save(
                        new UserNotification(null, savedEvent.getId(), null, recipientEmail, null, createdAt)));

        return savedEvent;
    }

    private String serializePayload(NotificationEventCommand command) {
        try {
            return objectMapper.writeValueAsString(command.getPayload());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Notification payload cannot be serialized", ex);
        }
    }

    private void validatePayload(NotificationEventCommand command) {
        Class<?> expectedType =
                switch (command.getType()) {
                    case COMMENT_CREATED, COMMENT_UPDATED, COMMENT_DELETED -> CommentNotificationPayload.class;
                    case TEAM_INVITATION_CREATED,
                            TEAM_INVITATION_RESENT,
                            TEAM_INVITATION_ACCEPTED,
                            TEAM_INVITATION_DECLINED,
                            TEAM_INVITATION_CANCELED,
                            TEAM_INVITATION_EXPIRED -> InvitationNotificationPayload.class;
                    case TASK_CREATED,
                            TASK_STATUS_CHANGED,
                            TASK_AUTHOR_CHANGED,
                            TASK_ASSIGNEE_CHANGED,
                            TASK_TITLE_CHANGED,
                            TASK_DESCRIPTION_CHANGED,
                            TASK_DEADLINE_CHANGED,
                            TASK_TAG_CHANGED,
                            TASK_ARCHIVED,
                            TASK_RESTORED -> TaskNotificationPayload.class;
                    default -> TeamNotificationPayload.class;
                };

        if (command.getPayload() == null || !expectedType.isInstance(command.getPayload())) {
            throw new IllegalArgumentException("Notification payload does not match event type "
                    + command.getType().name());
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private NotificationInvitationAction getInvitationAction(NotificationFeedItem item, UUID userId) {
        NotificationInvitationAction action = null;
        if (item.event().getType() != dev.gushchin.taskmanager.model.NotificationEventType.TEAM_INVITATION_CREATED
                || item.event().getInvitationId() == null
                || userId.equals(item.event().getActorUserId())) {
            return null;
        }

        TeamInvitation invitation =
                teamInvitationRepository.findById(item.event().getInvitationId());
        if (invitation != null
                && invitation.getStatus() == TeamInvitationStatus.PENDING
                && !invitation.getExpiresAt().isBefore(Instant.now())) {
            action = NotificationInvitationAction.active(invitation.getToken());
        } else if (invitation != null
                && (invitation.getStatus() == TeamInvitationStatus.EXPIRED
                        || invitation.getExpiresAt().isBefore(Instant.now()))) {
            action = NotificationInvitationAction.expiredState();
        }

        return action;
    }
}
