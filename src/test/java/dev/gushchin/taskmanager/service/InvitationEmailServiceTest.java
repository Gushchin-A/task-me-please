package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

class InvitationEmailServiceTest {
    private final TransactionalEmailSender emailSender = mock(TransactionalEmailSender.class);

    @Test
    void sendInvitationShouldSendExpectedMessage() {
        AppProperties appProperties = createAppProperties();
        InvitationEmailService invitationEmailService = new InvitationEmailService(emailSender, appProperties);
        TeamInvitation invitation = createInvitation();
        Team team = new Team(10L, "Invite Team", UUID.randomUUID(), Instant.now(), Instant.now(), false);
        User invitedBy = new User(
                UUID.randomUUID(),
                "owner@test.com",
                "Owner",
                "password",
                Instant.now(),
                Instant.now(),
                false,
                null,
                false);

        invitationEmailService.sendInvitation(invitation, team, invitedBy);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        org.mockito.ArgumentMatchers.eq("invited@test.com"),
                        org.mockito.ArgumentMatchers.eq("Приглашение в команду Invite Team"),
                        textCaptor.capture());
        String text = textCaptor.getValue();

        assertTrue(text.contains("Вас пригласили в команду «Invite Team»."));
        assertTrue(text.contains("Пригласил: owner@test.com"));
        assertFalse(text.contains("Owner"));
        assertTrue(text.contains("Ссылка действует 7 дней."));
        assertTrue(text.contains("https://task-me-please.test/invitations/token-123"));
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
