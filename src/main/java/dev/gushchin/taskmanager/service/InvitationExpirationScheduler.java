package dev.gushchin.taskmanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "notifications.invitation-expiration-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InvitationExpirationScheduler {
    private final TeamInvitationService teamInvitationService;

    @Scheduled(
            fixedDelayString = "${notifications.invitation-expiration-delay-ms:60000}",
            initialDelayString = "${notifications.invitation-expiration-initial-delay-ms:60000}")
    public void expireInvitations() {
        teamInvitationService.expirePendingInvitations();
    }
}
