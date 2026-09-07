package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;
import dev.gushchin.taskmanager.model.User;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InvitationEmailServiceTest {
    private final TransactionalEmailSender emailSender = mock(TransactionalEmailSender.class);
    private final EmailLifetimeFormatter lifetimeFormatter = new EmailLifetimeFormatter();

    @Test
    void sendInvitationShouldSendAgreedHtmlAndText() {
        EmailMessageSender messageSender = new EmailMessageSender(
                emailSender,
                new EmailTemplateRenderer(TemplateEngine.createPrecompiled(ContentType.Html)),
                new EmailTextRenderer(),
                createAppProperties());
        InvitationEmailService invitationEmailService = new InvitationEmailService(messageSender, lifetimeFormatter);
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

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("invited@test.com"),
                        eq("Вас пригласили в команду «Invite Team»"),
                        htmlCaptor.capture(),
                        textCaptor.capture());

        String html = htmlCaptor.getValue();
        assertTrue(html.contains("Новое приглашение в команду"));
        assertTrue(html.contains("owner@test.com пригласил вас в команду «Invite Team» в сервисе TaskMePlease."));
        assertTrue(html.contains("Открыть приглашение"));
        assertTrue(html.contains("https://task-me-please.test/invitations/token-123"));
        assertTrue(html.contains("Ссылка действует 7 дней."));
        assertFalse(html.contains(">Owner<"));

        String text = textCaptor.getValue();
        assertTrue(text.contains("owner@test.com пригласил вас в команду «Invite Team» в сервисе TaskMePlease."));
        assertTrue(text.contains("https://task-me-please.test/invitations/token-123"));
        assertTrue(text.contains("Ссылка действует 7 дней."));
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
