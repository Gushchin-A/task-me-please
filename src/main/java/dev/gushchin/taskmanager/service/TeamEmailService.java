package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamMemberRepository;
import dev.gushchin.taskmanager.repository.UserRepository;
import dev.gushchin.taskmanager.view.EmailActionView;
import dev.gushchin.taskmanager.view.EmailContentView;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamEmailService {
    private static final String INVITATION_ACCEPTED_HEADING = "Приглашение принято";
    private static final String INVITATION_DECLINED_HEADING = "Приглашение отклонено";
    private static final String TASK_ACCESS_HEADING = "Изменение доступа к задачам";
    private static final String TEAM_MEMBERS_HEADING = "Состав команды обновлен";
    private static final String MEMBER_REMOVED_HEADING = "Вас удалили из команды";
    private static final String TEAM_DELETED_HEADING = "Команда удалена";

    private static final String ALL_INVITATIONS_LABEL = "Все приглашения";
    private static final String TEAM_MEMBERS_LABEL = "Участники команды";
    private static final String OPEN_TEAM_LABEL = "Открыть команду";
    private static final String MY_TEAMS_LABEL = "Мои команды";

    private static final String USER_PREFIX = "Пользователь ";
    private static final String TEAM_QUOTE_OPEN = "команду «";
    private static final String TEAM_PREFIX = "Команда «";
    private static final String DECLINED_INVITATION = " отклонил приглашение в ";
    private static final String LEFT_TEAM = " покинул ";

    private final EmailMessageSender messageSender;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public void sendInvitationAccepted(String invitedEmail, Team team, UUID ownerId) {
        User owner = userRepository.findById(ownerId);
        if (owner == null) {
            return;
        }

        EmailContentView content = EmailContentView.builder()
                .heading(INVITATION_ACCEPTED_HEADING)
                .bodyParagraph(USER_PREFIX + invitedEmail + " принял приглашение и присоединился к команде «"
                        + team.getName() + "».")
                .action(new EmailActionView(ALL_INVITATIONS_LABEL, teamInvitationsUrl(team)))
                .build();

        sendSafely(owner.getEmail(), invitedEmail + " принял приглашение в команду «" + team.getName() + "»", content);
    }

    public void sendInvitationDeclined(String invitedEmail, Team team, UUID ownerId) {
        User owner = userRepository.findById(ownerId);
        if (owner == null) {
            return;
        }

        EmailContentView content = EmailContentView.builder()
                .heading(INVITATION_DECLINED_HEADING)
                .bodyParagraph(
                        USER_PREFIX + invitedEmail + DECLINED_INVITATION + TEAM_QUOTE_OPEN + team.getName() + "».")
                .action(new EmailActionView(ALL_INVITATIONS_LABEL, teamInvitationsUrl(team)))
                .build();

        sendSafely(
                owner.getEmail(), invitedEmail + DECLINED_INVITATION + TEAM_QUOTE_OPEN + team.getName() + "»", content);
    }

    public void sendAllTasksVisible(UUID recipientId, Team team) {
        User recipient = userRepository.findById(recipientId);
        if (recipient == null) {
            return;
        }

        EmailContentView content = EmailContentView.builder()
                .heading(TASK_ACCESS_HEADING)
                .bodyParagraph("Теперь вы видите все задачи в команде «" + team.getName() + "».")
                .action(new EmailActionView(OPEN_TEAM_LABEL, teamUrl(team)))
                .build();

        sendSafely(
                recipient.getEmail(), "Вам открыли видимость всех задач в команде «" + team.getName() + "»", content);
    }

    public void sendOwnTasksVisibleOnly(UUID recipientId, Team team) {
        User recipient = userRepository.findById(recipientId);
        if (recipient == null) {
            return;
        }

        EmailContentView content = EmailContentView.builder()
                .heading(TASK_ACCESS_HEADING)
                .bodyParagraph("Владелец ограничил видимость задач в команде «" + team.getName()
                        + "». Теперь вам видны только задачи, где вы автор или исполнитель.")
                .action(new EmailActionView(OPEN_TEAM_LABEL, teamUrl(team)))
                .build();

        sendSafely(recipient.getEmail(), "Видимость задач в команде «" + team.getName() + "» ограничена", content);
    }

    public void sendMemberLeft(User member, Team team, UUID ownerId) {
        User owner = ownerId == null ? null : userRepository.findById(ownerId);
        if (owner == null) {
            return;
        }

        String memberName = displayName(member);
        EmailContentView content = EmailContentView.builder()
                .heading(TEAM_MEMBERS_HEADING)
                .bodyParagraph(memberName + LEFT_TEAM + TEAM_QUOTE_OPEN + team.getName() + "».")
                .action(new EmailActionView(TEAM_MEMBERS_LABEL, teamMembersUrl(team)))
                .build();

        sendSafely(owner.getEmail(), memberName + LEFT_TEAM + TEAM_QUOTE_OPEN + team.getName() + "»", content);
    }

    public void sendMemberRemoved(UUID removedUserId, Team team) {
        User removed = userRepository.findById(removedUserId);
        if (removed == null) {
            return;
        }

        EmailContentView content = EmailContentView.builder()
                .heading(MEMBER_REMOVED_HEADING)
                .bodyParagraph("Владелец удалил вас из команды «" + team.getName()
                        + "».\nУ вас больше нет доступа к задачам команды.")
                .build();

        sendSafely(removed.getEmail(), "Вы удалены из команды «" + team.getName() + "»", content);
    }

    public void sendTeamDeleted(Team team, UUID actorUserId) {
        EmailContentView content = EmailContentView.builder()
                .heading(TEAM_DELETED_HEADING)
                .bodyParagraph(TEAM_PREFIX + team.getName() + "» была удалена владельцем. "
                        + "У вас больше нет доступа к задачам и данным этой команды.")
                .action(new EmailActionView(MY_TEAMS_LABEL, messageSender.baseUrl() + "/teams"))
                .build();

        String subject = TEAM_PREFIX + team.getName() + "» удалена";
        for (TeamMember member : teamMemberRepository.findByTeamId(team.getId())) {
            if (member.isDeleted() || member.getUserId().equals(actorUserId)) {
                continue;
            }

            User recipient = userRepository.findById(member.getUserId());
            if (recipient != null) {
                sendSafely(recipient.getEmail(), subject, content);
            }
        }
    }

    private void sendSafely(String recipient, String subject, EmailContentView content) {
        try {
            messageSender.send(recipient, subject, content);
        } catch (TransactionalEmailSendingException ex) {
            if (log.isWarnEnabled()) {
                log.warn(
                        "Не удалось отправить письмо об изменении в команде: errorType={}",
                        ex.getClass().getSimpleName());
            }
        }
    }

    private String displayName(User user) {
        String name = user.getName();

        return name == null || name.isBlank() ? user.getEmail() : name;
    }

    private String teamUrl(Team team) {
        return messageSender.baseUrl() + "/teams/" + team.getId();
    }

    private String teamMembersUrl(Team team) {
        return teamUrl(team) + "/members";
    }

    private String teamInvitationsUrl(Team team) {
        return teamUrl(team) + "/invite";
    }
}
