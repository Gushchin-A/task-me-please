package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.exception.AccessDeniedForTaskException;
import dev.gushchin.taskmanager.exception.InvitationEmailSendingException;
import dev.gushchin.taskmanager.exception.TeamInvitationAlreadyPendingException;
import dev.gushchin.taskmanager.exception.TeamInvitationNotFoundException;
import dev.gushchin.taskmanager.exception.TeamInvitationNotPendingException;
import dev.gushchin.taskmanager.exception.TeamMemberAlreadyExistsException;
import dev.gushchin.taskmanager.exception.TeamMemberNotFoundException;
import dev.gushchin.taskmanager.exception.UserNotFoundByEmailException;
import dev.gushchin.taskmanager.model.TeamInvitation;
import dev.gushchin.taskmanager.model.TeamInvitationStatus;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamMemberRole;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamInvitationRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamInvitationService {
    public static final int EXPIRATION_DAYS = 7;

    private static final int TOKEN_BYTES = 32;
    private static final int EXPIRATION_BATCH_SIZE = 100;

    private final SecureRandom secureRandom = new SecureRandom();
    private final InvitationEmailService invitationEmailService;
    private final TeamInvitationRepository teamInvitationRepository;
    private final TeamMemberService teamMemberService;
    private final TeamService teamService;
    private final UserService userService;
    private final NotificationPublisher notificationPublisher;

    @Transactional
    public List<TeamInvitation> findByTeamId(Long teamId, UUID currentUserId) {
        TeamMember currentMember = teamMemberService.findById(teamId, currentUserId);

        if (currentMember.getRole() != TeamMemberRole.OWNER) {
            throw new AccessDeniedForTaskException();
        }

        return teamInvitationRepository.findByTeamId(teamId).stream()
                .map(this::cancelIfExpired)
                .toList();
    }

    @Transactional
    public TeamInvitation create(Long teamId, String invitedEmail, UUID currentUserId) {
        TeamMember currentMember = teamMemberService.findById(teamId, currentUserId);

        if (currentMember.getRole() != TeamMemberRole.OWNER) {
            throw new AccessDeniedForTaskException();
        }

        String normalizedEmail = normalizeEmail(invitedEmail);
        checkActiveMember(teamId, normalizedEmail);

        TeamInvitation pendingInvitation =
                teamInvitationRepository.findPendingByTeamIdAndEmail(teamId, normalizedEmail);
        if (pendingInvitation != null
                && cancelIfExpired(pendingInvitation).getStatus() == TeamInvitationStatus.PENDING) {
            throw new TeamInvitationAlreadyPendingException(teamId, normalizedEmail);
        }

        Instant now = Instant.now();
        TeamInvitation invitation = new TeamInvitation(
                null,
                teamId,
                currentUserId,
                normalizedEmail,
                generateToken(),
                TeamInvitationStatus.PENDING,
                now.plus(Duration.ofDays(EXPIRATION_DAYS)),
                now,
                now,
                false);

        TeamInvitation savedInvitation = teamInvitationRepository.save(invitation);
        notificationPublisher.teamInvitationCreated(savedInvitation);

        return savedInvitation;
    }

    @Transactional
    public TeamInvitation createAndSend(Long teamId, String invitedEmail, UUID currentUserId) {
        TeamInvitation invitation = create(teamId, invitedEmail, currentUserId);

        sendInvitation(invitation);

        return invitation;
    }

    @Transactional
    public void resend(Long id, Long teamId, UUID currentUserId) {
        TeamInvitation invitation = findPendingById(id, teamId, currentUserId);
        Instant now = Instant.now();
        TeamInvitation updatedInvitation = teamInvitationRepository.updateDelivery(
                invitation.getId(), generateToken(), now.plus(Duration.ofDays(EXPIRATION_DAYS)), now);

        sendInvitation(updatedInvitation);
        notificationPublisher.teamInvitationResent(updatedInvitation, currentUserId);
    }

    @Transactional
    public TeamInvitation findByToken(String token) {
        TeamInvitation invitation = teamInvitationRepository.findByToken(token);
        if (invitation == null) {
            throw new TeamInvitationNotFoundException(token);
        }

        teamService.findById(invitation.getTeamId());

        return cancelIfExpired(invitation);
    }

    @Transactional
    public TeamInvitation accept(String token, UUID currentUserId) {
        TeamInvitation invitation = findPendingByToken(token);

        TeamMember member = teamMemberService.addMember(invitation.getTeamId(), currentUserId);
        TeamInvitation acceptedInvitation =
                teamInvitationRepository.updateStatus(invitation.getId(), TeamInvitationStatus.ACCEPTED, Instant.now());
        notificationPublisher.teamMemberJoinedAfterInvitation(member, currentUserId, invitation.getInvitedBy());
        notificationPublisher.teamInvitationAccepted(acceptedInvitation, currentUserId);

        return acceptedInvitation;
    }

    @Transactional
    public TeamInvitation decline(String token, UUID currentUserId) {
        TeamInvitation invitation = findPendingByToken(token);

        userService.findById(currentUserId);

        TeamInvitation declinedInvitation =
                teamInvitationRepository.updateStatus(invitation.getId(), TeamInvitationStatus.DECLINED, Instant.now());
        notificationPublisher.teamInvitationDeclined(declinedInvitation, currentUserId);

        return declinedInvitation;
    }

    @Transactional
    public TeamInvitation cancel(Long id, Long teamId, UUID currentUserId) {
        TeamInvitation invitation = findPendingById(id, teamId, currentUserId);
        TeamInvitation canceledInvitation =
                teamInvitationRepository.updateStatus(invitation.getId(), TeamInvitationStatus.CANCELED, Instant.now());
        notificationPublisher.teamInvitationCanceled(canceledInvitation, currentUserId);

        return canceledInvitation;
    }

    @Transactional(noRollbackFor = TeamInvitationNotPendingException.class)
    public TeamInvitation findPendingByToken(String token) {
        TeamInvitation invitation = findByToken(token);
        ensurePending(invitation);

        return invitation;
    }

    @Transactional
    public int expirePendingInvitations() {
        List<TeamInvitation> invitations =
                teamInvitationRepository.findExpiredPending(Instant.now(), EXPIRATION_BATCH_SIZE);
        int expiredCount = 0;

        for (TeamInvitation invitation : invitations) {
            if (cancelIfExpired(invitation).getStatus() == TeamInvitationStatus.EXPIRED) {
                expiredCount++;
            }
        }

        return expiredCount;
    }

    private TeamInvitation findPendingById(Long id, Long teamId, UUID currentUserId) {
        TeamMember currentMember = teamMemberService.findById(teamId, currentUserId);

        if (currentMember.getRole() != TeamMemberRole.OWNER) {
            throw new AccessDeniedForTaskException();
        }

        TeamInvitation invitation = teamInvitationRepository.findByTeamId(teamId).stream()
                .filter(teamInvitation -> teamInvitation.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new TeamInvitationNotFoundException(id.toString()));

        ensurePending(cancelIfExpired(invitation));

        return invitation;
    }

    private void sendInvitation(TeamInvitation invitation) {
        try {
            invitationEmailService.sendInvitation(
                    invitation,
                    teamService.findById(invitation.getTeamId()),
                    userService.findById(invitation.getInvitedBy()));
        } catch (InvitationEmailSendingException ex) {
            if (log.isWarnEnabled()) {
                log.warn("Invitation email could not be sent for invitation {}", invitation.getId(), ex);
            }
        }
    }

    private void checkActiveMember(Long teamId, String invitedEmail) {
        if (!isActiveMember(teamId, invitedEmail)) {
            return;
        }

        User invitedUser = userService.findByEmail(invitedEmail);
        throw new TeamMemberAlreadyExistsException(teamId, invitedUser.getId());
    }

    private boolean isActiveMember(Long teamId, String invitedEmail) {
        try {
            User invitedUser = userService.findByEmail(invitedEmail);
            teamMemberService.findById(teamId, invitedUser.getId());

            return true;
        } catch (UserNotFoundByEmailException ex) {
            return false;
        } catch (TeamMemberNotFoundException ex) {
            return false;
        }
    }

    private TeamInvitation cancelIfExpired(TeamInvitation invitation) {
        if (invitation.getStatus() == TeamInvitationStatus.PENDING
                && invitation.getExpiresAt().isBefore(Instant.now())) {
            TeamInvitation expiredInvitation = teamInvitationRepository.updateStatusIfPending(
                    invitation.getId(), TeamInvitationStatus.EXPIRED, Instant.now());
            if (expiredInvitation != null) {
                notificationPublisher.teamInvitationExpired(expiredInvitation);
                return expiredInvitation;
            }

            return teamInvitationRepository.findByToken(invitation.getToken());
        }

        return invitation;
    }

    private void ensurePending(TeamInvitation invitation) {
        if (invitation.getStatus() != TeamInvitationStatus.PENDING) {
            throw new TeamInvitationNotPendingException(invitation.getId());
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateToken() {
        String token = generateRandomToken();

        while (teamInvitationRepository.existsByToken(token)) {
            token = generateRandomToken();
        }

        return token;
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
