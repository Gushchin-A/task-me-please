package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.model.Comment;
import dev.gushchin.taskmanager.model.CommentNotificationPayload;
import dev.gushchin.taskmanager.model.InvitationNotificationPayload;
import dev.gushchin.taskmanager.model.NotificationEventCommand;
import dev.gushchin.taskmanager.model.NotificationEventType;
import dev.gushchin.taskmanager.model.NotificationRecipients;
import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskNotificationPayload;
import dev.gushchin.taskmanager.model.TaskParticipantIds;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamNotificationPayload;
import dev.gushchin.taskmanager.model.TeamTag;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamRepository;
import dev.gushchin.taskmanager.repository.TeamTagRepository;
import dev.gushchin.taskmanager.repository.UserRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationPublisher {
    private static final DateTimeFormatter DEADLINE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"));

    private final NotificationRecipientPolicy notificationRecipientPolicy;
    private final NotificationService notificationService;
    private final TeamRepository teamRepository;
    private final TeamTagRepository teamTagRepository;
    private final UserRepository userRepository;

    public void taskCreated(Task task, UUID actorUserId) {
        publishTaskEvent(NotificationEventType.TASK_CREATED, task, task, actorUserId, TaskEventDetails.empty());
    }

    public void taskTitleChanged(Task before, Task after, UUID actorUserId) {
        publishTaskEvent(
                NotificationEventType.TASK_TITLE_CHANGED,
                before,
                after,
                actorUserId,
                new TaskEventDetails(before.getTitle(), after.getTitle(), null, null));
    }

    public void taskDescriptionChanged(Task before, Task after, UUID actorUserId) {
        publishTaskEvent(
                NotificationEventType.TASK_DESCRIPTION_CHANGED, before, after, actorUserId, TaskEventDetails.empty());
    }

    public void taskDeadlineChanged(Task before, Task after, UUID actorUserId) {
        publishTaskEvent(
                NotificationEventType.TASK_DEADLINE_CHANGED,
                before,
                after,
                actorUserId,
                new TaskEventDetails(
                        formatDeadline(before.getDeadlineAt()), formatDeadline(after.getDeadlineAt()), null, null));
    }

    public void taskTagChanged(Task before, Task after, UUID actorUserId) {
        publishTaskEvent(
                NotificationEventType.TASK_TAG_CHANGED,
                before,
                after,
                actorUserId,
                new TaskEventDetails(
                        teamTagRepository.findById(before.getTagId()).getName(),
                        teamTagRepository.findById(after.getTagId()).getName(),
                        null,
                        null));
    }

    public void taskStatusChanged(Task before, Task after, UUID actorUserId) {
        publishTaskEvent(
                NotificationEventType.TASK_STATUS_CHANGED,
                before,
                after,
                actorUserId,
                new TaskEventDetails(
                        before.getStatus().getDisplayName(), after.getStatus().getDisplayName(), null, null));
    }

    public void taskAuthorChanged(Task before, Task after, UUID actorUserId) {
        publishTaskEvent(
                NotificationEventType.TASK_AUTHOR_CHANGED,
                before,
                after,
                actorUserId,
                new TaskEventDetails(
                        userRepository.findById(before.getAuthorId()).getName(),
                        userRepository.findById(after.getAuthorId()).getName(),
                        before.getAuthorId(),
                        after.getAuthorId()));
    }

    public void taskAssigneeChanged(Task before, Task after, UUID actorUserId) {
        publishTaskEvent(
                NotificationEventType.TASK_ASSIGNEE_CHANGED,
                before,
                after,
                actorUserId,
                new TaskEventDetails(
                        userRepository.findById(before.getAssigneeId()).getName(),
                        userRepository.findById(after.getAssigneeId()).getName(),
                        before.getAssigneeId(),
                        after.getAssigneeId()));
    }

    public void taskArchived(Task before, Task after, UUID actorUserId) {
        publishTaskEvent(
                NotificationEventType.TASK_ARCHIVED,
                before,
                after,
                actorUserId,
                new TaskEventDetails(before.getStatus().getDisplayName(), null, null, null));
    }

    public void taskRestored(Task before, Task after, UUID actorUserId) {
        publishTaskEvent(NotificationEventType.TASK_RESTORED, before, after, actorUserId, TaskEventDetails.empty());
    }

    public void commentCreated(Comment comment, Task task, UUID actorUserId) {
        publishCommentEvent(NotificationEventType.COMMENT_CREATED, comment, task, actorUserId);
    }

    public void commentUpdated(Comment comment, Task task, UUID actorUserId) {
        publishCommentEvent(NotificationEventType.COMMENT_UPDATED, comment, task, actorUserId);
    }

    public void commentDeleted(Comment comment, Task task, UUID actorUserId) {
        publishCommentEvent(NotificationEventType.COMMENT_DELETED, comment, task, actorUserId);
    }

    public void taskVisibilityChanged(TeamMember before, TeamMember after, UUID actorUserId) {
        NotificationEventType type =
                after.getTaskVisibility() == dev.gushchin.taskmanager.model.TeamTaskVisibility.ALL_TASKS
                        ? NotificationEventType.TEAM_TASK_VISIBILITY_GRANTED
                        : NotificationEventType.TEAM_TASK_VISIBILITY_RESTRICTED;
        publishTeamMemberEvent(
                type,
                after.getTeamId(),
                after.getUserId(),
                actorUserId,
                before.getTaskVisibility().name(),
                after.getTaskVisibility().name(),
                notificationRecipientPolicy.forUsers(actorUserId, after.getUserId()));
    }

    public void teamMemberRemoved(TeamMember member, UUID actorUserId) {
        publishTeamMemberEvent(
                NotificationEventType.TEAM_MEMBER_REMOVED,
                member.getTeamId(),
                member.getUserId(),
                actorUserId,
                null,
                null,
                notificationRecipientPolicy.forAllTeamMembers(member.getTeamId(), member.getUserId()));
    }

    public void teamMemberLeft(TeamMember member) {
        publishTeamMemberEvent(
                NotificationEventType.TEAM_MEMBER_LEFT,
                member.getTeamId(),
                member.getUserId(),
                member.getUserId(),
                null,
                null,
                notificationRecipientPolicy.forAllTeamMembers(member.getTeamId(), member.getUserId()));
    }

    public void teamMemberJoined(TeamMember member, UUID actorUserId) {
        publishTeamMemberEvent(
                NotificationEventType.TEAM_MEMBER_JOINED,
                member.getTeamId(),
                member.getUserId(),
                actorUserId,
                null,
                null,
                notificationRecipientPolicy.forAllTeamMembers(member.getTeamId()));
    }

    public void teamMemberJoinedAfterInvitation(TeamMember member, UUID actorUserId, UUID ownerUserId) {
        publishTeamMemberEvent(
                NotificationEventType.TEAM_MEMBER_JOINED,
                member.getTeamId(),
                member.getUserId(),
                actorUserId,
                null,
                null,
                notificationRecipientPolicy.forAllTeamMembersExcept(member.getTeamId(), ownerUserId));
    }

    public void teamCreated(Team team, UUID actorUserId) {
        publishTeamEvent(
                NotificationEventType.TEAM_CREATED,
                team,
                actorUserId,
                null,
                null,
                null,
                notificationRecipientPolicy.forUsers(actorUserId));
    }

    public void teamRenamed(Team before, Team after, UUID actorUserId) {
        publishTeamEvent(
                NotificationEventType.TEAM_RENAMED,
                after,
                actorUserId,
                null,
                before.getName(),
                after.getName(),
                notificationRecipientPolicy.forAllTeamMembers(after.getId()));
    }

    public void teamDeleted(Team team, UUID actorUserId) {
        publishTeamEvent(
                NotificationEventType.TEAM_DELETED,
                team,
                actorUserId,
                null,
                null,
                null,
                notificationRecipientPolicy.forAllTeamMembers(team.getId()));
    }

    public void teamTagCreated(TeamTag tag, UUID actorUserId) {
        publishTeamTagEvent(NotificationEventType.TEAM_TAG_CREATED, tag, actorUserId, null, tag.getName());
    }

    public void teamTagRenamed(TeamTag before, TeamTag after, UUID actorUserId) {
        publishTeamTagEvent(
                NotificationEventType.TEAM_TAG_RENAMED, after, actorUserId, before.getName(), after.getName());
    }

    public void teamTagDeleted(TeamTag tag, UUID actorUserId) {
        publishTeamTagEvent(NotificationEventType.TEAM_TAG_DELETED, tag, actorUserId, tag.getName(), null);
    }

    public void teamInvitationCreated(TeamInvitation invitation) {
        publishInvitationEvent(
                NotificationEventType.TEAM_INVITATION_CREATED,
                invitation,
                invitation.getInvitedBy(),
                notificationRecipientPolicy.forInvitation(invitation.getInvitedBy(), invitation.getInvitedEmail()));
    }

    public void teamInvitationResent(TeamInvitation invitation, UUID actorUserId) {
        publishInvitationEvent(
                NotificationEventType.TEAM_INVITATION_RESENT,
                invitation,
                actorUserId,
                notificationRecipientPolicy.forUsers(actorUserId));
    }

    public void teamInvitationAccepted(TeamInvitation invitation, UUID actorUserId) {
        publishInvitationEvent(
                NotificationEventType.TEAM_INVITATION_ACCEPTED,
                invitation,
                actorUserId,
                notificationRecipientPolicy.forUsers(invitation.getInvitedBy()));
    }

    public void teamInvitationDeclined(TeamInvitation invitation, UUID actorUserId) {
        publishInvitationEvent(
                NotificationEventType.TEAM_INVITATION_DECLINED,
                invitation,
                actorUserId,
                notificationRecipientPolicy.forUsers(invitation.getInvitedBy(), actorUserId));
    }

    public void teamInvitationCanceled(TeamInvitation invitation, UUID actorUserId) {
        publishInvitationEvent(
                NotificationEventType.TEAM_INVITATION_CANCELED,
                invitation,
                actorUserId,
                notificationRecipientPolicy.forUsers(actorUserId));
    }

    public void teamInvitationExpired(TeamInvitation invitation) {
        publishInvitationEvent(
                NotificationEventType.TEAM_INVITATION_EXPIRED,
                invitation,
                invitation.getInvitedBy(),
                notificationRecipientPolicy.forUsers(invitation.getInvitedBy()));
    }

    private void publishTaskEvent(
            NotificationEventType type, Task before, Task after, UUID actorUserId, TaskEventDetails details) {
        Team team = teamRepository.findById(after.getTeamId());
        User actor = userRepository.findById(actorUserId);
        NotificationEventCommand command = NotificationEventCommand.builder()
                .type(type)
                .actorUserId(actorUserId)
                .teamId(team.getId())
                .taskId(after.getId())
                .payload(new TaskNotificationPayload(
                        actor.getName(),
                        team.getName(),
                        after.getTitle(),
                        details.previousValue(),
                        details.newValue(),
                        details.previousUserId(),
                        details.newUserId(),
                        new TaskParticipantIds(after.getAuthorId(), after.getAssigneeId())))
                .createdAt(Instant.now())
                .build();
        NotificationRecipients recipients = notificationRecipientPolicy.forTaskChange(before, after, actorUserId);

        notificationService.publish(command, recipients);
    }

    private String formatDeadline(Instant deadlineAt) {
        return deadlineAt == null ? null : DEADLINE_FORMATTER.format(deadlineAt.atOffset(ZoneOffset.UTC));
    }

    private void publishCommentEvent(NotificationEventType type, Comment comment, Task task, UUID actorUserId) {
        Team team = teamRepository.findById(task.getTeamId());
        User actor = userRepository.findById(actorUserId);
        NotificationEventCommand command = NotificationEventCommand.builder()
                .type(type)
                .actorUserId(actorUserId)
                .teamId(team.getId())
                .taskId(task.getId())
                .commentId(comment.getId())
                .payload(new CommentNotificationPayload(actor.getName(), team.getName(), task.getTitle()))
                .createdAt(Instant.now())
                .build();

        notificationService.publish(command, notificationRecipientPolicy.forTask(task, actorUserId));
    }

    private void publishTeamMemberEvent(
            NotificationEventType type,
            Long teamId,
            UUID subjectUserId,
            UUID actorUserId,
            String previousValue,
            String newValue,
            NotificationRecipients recipients) {
        Team team = teamRepository.findById(teamId);
        User actor = userRepository.findById(actorUserId);
        User subject = userRepository.findById(subjectUserId);
        NotificationEventCommand command = NotificationEventCommand.builder()
                .type(type)
                .actorUserId(actorUserId)
                .teamId(teamId)
                .subjectUserId(subjectUserId)
                .payload(new TeamNotificationPayload(
                        actor.getName(), team.getName(), subject.getName(), previousValue, newValue))
                .createdAt(Instant.now())
                .build();

        notificationService.publish(command, recipients);
    }

    private void publishTeamTagEvent(
            NotificationEventType type, TeamTag tag, UUID actorUserId, String previousValue, String newValue) {
        Team team = teamRepository.findById(tag.getTeamId());
        publishTeamEvent(
                type,
                team,
                actorUserId,
                tag.getName(),
                previousValue,
                newValue,
                notificationRecipientPolicy.forAllTeamMembers(team.getId()));
    }

    private void publishTeamEvent(
            NotificationEventType type,
            Team team,
            UUID actorUserId,
            String subjectName,
            String previousValue,
            String newValue,
            NotificationRecipients recipients) {
        User actor = userRepository.findById(actorUserId);
        NotificationEventCommand command = NotificationEventCommand.builder()
                .type(type)
                .actorUserId(actorUserId)
                .teamId(team.getId())
                .payload(new TeamNotificationPayload(
                        actor.getName(), team.getName(), subjectName, previousValue, newValue))
                .createdAt(Instant.now())
                .build();

        notificationService.publish(command, recipients);
    }

    private void publishInvitationEvent(
            NotificationEventType type,
            TeamInvitation invitation,
            UUID actorUserId,
            NotificationRecipients recipients) {
        Team team = teamRepository.findById(invitation.getTeamId());
        User actor = userRepository.findById(actorUserId);
        NotificationEventCommand command = NotificationEventCommand.builder()
                .type(type)
                .actorUserId(actorUserId)
                .teamId(team.getId())
                .invitationId(invitation.getId())
                .payload(new InvitationNotificationPayload(
                        actor.getName(), team.getName(), invitation.getInvitedEmail()))
                .createdAt(Instant.now())
                .build();

        notificationService.publish(command, recipients);
    }

    private record TaskEventDetails(String previousValue, String newValue, UUID previousUserId, UUID newUserId) {
        private static TaskEventDetails empty() {
            return new TaskEventDetails(null, null, null, null);
        }
    }
}
