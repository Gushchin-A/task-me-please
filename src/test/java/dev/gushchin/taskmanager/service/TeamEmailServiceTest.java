package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamMemberRole;
import dev.gushchin.taskmanager.model.TeamTaskVisibility;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamMemberRepository;
import dev.gushchin.taskmanager.repository.UserRepository;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TeamEmailServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();

    private final TransactionalEmailSender emailSender = mock(TransactionalEmailSender.class);
    private final TeamMemberRepository teamMemberRepository = mock(TeamMemberRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private final TeamEmailService teamEmailService = new TeamEmailService(
            new EmailMessageSender(
                    emailSender,
                    new EmailTemplateRenderer(TemplateEngine.createPrecompiled(ContentType.Html)),
                    new EmailTextRenderer(),
                    createAppProperties()),
            teamMemberRepository,
            userRepository);

    @Test
    void sendInvitationAcceptedShouldWriteToOwner() {
        when(userRepository.findById(OWNER_ID)).thenReturn(createUser(OWNER_ID, "owner@test.com", "Владелец"));

        teamEmailService.sendInvitationAccepted("invited@test.com", createTeam(), OWNER_ID);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("owner@test.com"),
                        eq("invited@test.com принял приглашение в команду «Креативный заводиксвс»"),
                        htmlCaptor.capture(),
                        anyString());

        String html = htmlCaptor.getValue().replace('\u00A0', ' ');
        assertAll(
                () -> assertTrue(html.contains("Приглашение принято")),
                () -> assertTrue(
                        html.contains("Пользователь invited@test.com принял приглашение и присоединился к команде "
                                + "«Креативный заводиксвс».")),
                () -> assertTrue(html.contains("Все приглашения")),
                () -> assertTrue(html.contains("https://task-me-please.test/teams/7/invite")));
    }

    @Test
    void sendMemberRemovedShouldWriteToRemovedUserWithoutButton() {
        when(userRepository.findById(MEMBER_ID)).thenReturn(createUser(MEMBER_ID, "member@test.com", "Чубакка"));

        teamEmailService.sendMemberRemoved(MEMBER_ID, createTeam());

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("member@test.com"),
                        eq("Вы удалены из команды «Креативный заводиксвс»"),
                        htmlCaptor.capture(),
                        anyString());

        String html = htmlCaptor.getValue().replace('\u00A0', ' ');
        assertAll(
                () -> assertTrue(html.contains("Вас удалили из команды")),
                () -> assertTrue(html.contains("Владелец удалил вас из команды «Креативный заводиксвс».")),
                () -> assertTrue(html.contains("У вас больше нет доступа к задачам команды.")),
                () -> assertFalse(html.contains("bgcolor=\"#1f883d\"")));
    }

    @Test
    void sendTeamDeletedShouldSkipActorAndDeletedMembers() {
        when(teamMemberRepository.findByTeamId(7L))
                .thenReturn(List.of(
                        createMember(OWNER_ID, TeamMemberRole.OWNER, false),
                        createMember(MEMBER_ID, TeamMemberRole.MEMBER, false)));
        when(userRepository.findById(MEMBER_ID)).thenReturn(createUser(MEMBER_ID, "member@test.com", "Чубакка"));

        teamEmailService.sendTeamDeleted(createTeam(), OWNER_ID);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("member@test.com"),
                        eq("Команда «Креативный заводиксвс» удалена"),
                        htmlCaptor.capture(),
                        anyString());
        assertTrue(htmlCaptor.getValue().contains("владельцем. У\u00A0вас"));
        verify(emailSender, never()).send(eq("owner@test.com"), anyString(), anyString(), anyString());
    }

    @Test
    void sendMemberRemovedShouldNotFailWhenDeliveryFails() {
        when(userRepository.findById(MEMBER_ID)).thenReturn(createUser(MEMBER_ID, "member@test.com", "Чубакка"));
        doThrow(new TransactionalEmailSendingException(new IllegalStateException("no api key")))
                .when(emailSender)
                .send(anyString(), anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> teamEmailService.sendMemberRemoved(MEMBER_ID, createTeam()));
    }

    @Test
    void sendVisibilityEmailsShouldUseAgreedTexts() {
        when(userRepository.findById(MEMBER_ID)).thenReturn(createUser(MEMBER_ID, "member@test.com", "Чубакка"));

        teamEmailService.sendOwnTasksVisibleOnly(MEMBER_ID, createTeam());

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender)
                .send(
                        eq("member@test.com"),
                        eq("Видимость задач в команде «Креативный заводиксвс» ограничена"),
                        htmlCaptor.capture(),
                        anyString());

        assertTrue(htmlCaptor
                .getValue()
                .replace('\u00A0', ' ')
                .contains("Владелец ограничил видимость задач в команде «Креативный заводиксвс». "
                        + "Теперь вам видны только задачи, где вы автор или исполнитель."));
    }

    private AppProperties createAppProperties() {
        AppProperties appProperties = new AppProperties();
        appProperties.setBaseUrl("https://task-me-please.test");

        return appProperties;
    }

    private Team createTeam() {
        return new Team(7L, "Креативный заводиксвс", OWNER_ID, Instant.now(), Instant.now(), false);
    }

    private User createUser(UUID id, String email, String name) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);

        return user;
    }

    private TeamMember createMember(UUID userId, TeamMemberRole role, boolean deleted) {
        TeamMember member = new TeamMember(7L, userId, role, TeamTaskVisibility.OWN_TASKS, Instant.now());
        member.setDeleted(deleted);

        return member;
    }
}
