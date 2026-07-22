package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;
import dev.gushchin.taskmanager.model.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class InvitationEmailServiceTest {
    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    void sendInvitationShouldSendExpectedMessage() {
        AppProperties appProperties = createAppProperties();
        InvitationEmailService invitationEmailService = new InvitationEmailService(mailSender, appProperties);
        TeamInvitation invitation = createInvitation();
        Team team = new Team(10L, "Invite Team", UUID.randomUUID(), Instant.now(), Instant.now(), false);
        User invitedBy =
                new User(UUID.randomUUID(), "owner@test.com", "Owner", "password", Instant.now(), Instant.now(), false);

        invitationEmailService.sendInvitation(invitation, team, invitedBy);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertEquals("no-reply@test.com", message.getFrom());
        assertEquals("invited@test.com", message.getTo()[0]);
        assertEquals("Приглашение в команду Invite Team", message.getSubject());
        assertTrue(message.getText().contains("Вас пригласили в команду «Invite Team»."));
        assertTrue(message.getText().contains("Пригласил: Owner, owner@test.com"));
        assertTrue(message.getText().contains("Ссылка действует 30 дней."));
        assertTrue(message.getText().contains("https://task-me-please.test/invitations/token-123"));
    }

    private AppProperties createAppProperties() {
        AppProperties appProperties = new AppProperties();
        appProperties.setBaseUrl("https://task-me-please.test");
        appProperties.getMail().setFrom("no-reply@test.com");

        return appProperties;
    }

    private TeamInvitation createInvitation() {
        Instant now = Instant.now();

        return new TeamInvitation(
                1L,
                10L,
                UUID.randomUUID(),
                "invited@test.com",
                "token-123",
                TeamInvitationStatus.PENDING,
                now.plusSeconds(60),
                now,
                now,
                false);
    }
}
