package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.InvitationEmailSendingException;
import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.view.EmailActionView;
import dev.gushchin.taskmanager.view.EmailContentView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvitationEmailService {
    private static final String INVITATION_PATH_PREFIX = "/invitations/";
    private static final String HEADING = "Новое приглашение в команду";

    private final EmailMessageSender messageSender;
    private final EmailLifetimeFormatter lifetimeFormatter;

    public void sendInvitation(TeamInvitation invitation, Team team, User invitedBy) {
        EmailContentView content = EmailContentView.builder()
                .heading(HEADING)
                .bodyParagraph(invitedBy.getEmail() + " пригласил вас в команду «" + team.getName()
                        + "» в сервисе TaskMePlease.")
                .action(new EmailActionView(
                        "Открыть приглашение",
                        messageSender.baseUrl() + INVITATION_PATH_PREFIX + invitation.getToken()))
                .note("Ссылка действует " + lifetimeFormatter.formatDays(TeamInvitationService.EXPIRATION_DAYS) + ".")
                .build();

        try {
            messageSender.send(
                    invitation.getInvitedEmail(), "Вас пригласили в команду «" + team.getName() + "»", content);
        } catch (TransactionalEmailSendingException ex) {
            throw new InvitationEmailSendingException(ex);
        }
    }
}
