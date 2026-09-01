package dev.gushchin.taskmanager.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamInvitationViewTest {
    @Test
    void pendingInvitationShouldShowWaitingStatusAndAllowActions() {
        TeamInvitationView invitation = createView(TeamInvitationStatus.PENDING);

        assertEquals("Ожидание", invitation.statusText());
        assertEquals("Скопируйте ссылку и отправьте ее лично", invitation.statusTooltip());
        assertTrue(invitation.canCancel());
    }

    @Test
    void terminalInvitationsShouldExplainStatusAndHideActions() {
        TeamInvitationView accepted = createView(TeamInvitationStatus.ACCEPTED);
        assertEquals("Пользователь принял приглашение", accepted.statusTooltip());
        assertFalse(accepted.canCancel());

        TeamInvitationView declined = createView(TeamInvitationStatus.DECLINED);
        assertEquals("Пользователь отклонил приглашение", declined.statusTooltip());
        assertFalse(declined.canCancel());

        TeamInvitationView canceled = createView(TeamInvitationStatus.CANCELED);
        assertEquals("Вы отменили приглашение", canceled.statusTooltip());
        assertFalse(canceled.canCancel());

        TeamInvitationView expired = createView(TeamInvitationStatus.EXPIRED);
        assertEquals("Истекло", expired.statusText());
        assertEquals("Срок действия ссылки истек", expired.statusTooltip());
        assertFalse(expired.canCancel());
    }

    private TeamInvitationView createView(TeamInvitationStatus status) {
        Instant now = Instant.now();
        TeamInvitation invitation = new TeamInvitation(
                1L,
                14L,
                UUID.randomUUID(),
                "member@example.com",
                "invitation-token",
                status,
                now.plusSeconds(60),
                now,
                now,
                false);

        return TeamInvitationView.from(invitation);
    }
}
