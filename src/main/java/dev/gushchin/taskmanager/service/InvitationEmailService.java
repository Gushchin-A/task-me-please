package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.exception.InvitationEmailSendingException;
import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvitationEmailService {
    private static final String INVITATION_PATH_PREFIX = "/invitations/";

    private final TransactionalEmailSender emailSender;
    private final AppProperties appProperties;

    public void sendInvitation(TeamInvitation invitation, Team team, User invitedBy) {
        try {
            emailSender.send(
                    invitation.getInvitedEmail(),
                    "Приглашение в команду " + team.getName(),
                    buildText(invitation, team, invitedBy));
        } catch (TransactionalEmailSendingException ex) {
            throw new InvitationEmailSendingException(ex);
        }
    }

    private String buildText(TeamInvitation invitation, Team team, User invitedBy) {
        return "Вас пригласили в команду «" + team.getName() + "».\n\n"
                + "Пригласил: " + invitedBy.getEmail() + "\n"
                + "Ссылка действует " + TeamInvitationService.EXPIRATION_DAYS + " дней.\n\n"
                + "Перейти к приглашению: " + buildInvitationUrl(invitation);
    }

    private String buildInvitationUrl(TeamInvitation invitation) {
        return getBaseUrl() + INVITATION_PATH_PREFIX + invitation.getToken();
    }

    private String getBaseUrl() {
        String baseUrl = appProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl;
    }
}
