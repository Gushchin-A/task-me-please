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
    public static final int EXPIRATION_DAYS = 30;

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final InvitationEmailService invitationEmailService;
    private final TeamInvitationRepository teamInvitationRepository;
    private final TeamMemberService teamMemberService;
    private final TeamService teamService;
    private final UserService userService;

    public List<TeamInvitation> findByTeamId(Long teamId, UUID currentUserId) {
        TeamMember currentMember = teamMemberService.findById(teamId, currentUserId);

        if (currentMember.getRole() != TeamMemberRole.OWNER) {
            throw new AccessDeniedForTaskException();
        }

        return teamInvitationRepository.findByTeamId(teamId).stream()
                .map(this::cancelIfExpired)
                .toList();
    }

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

        return teamInvitationRepository.save(invitation);
    }

    @Transactional
    public TeamInvitation createAndSend(Long teamId, String invitedEmail, UUID currentUserId) {
        TeamInvitation invitation = create(teamId, invitedEmail, currentUserId);

        sendInvitation(invitation);

        return invitation;
    }

    public void resend(Long id, Long teamId, UUID currentUserId) {
        TeamInvitation invitation = findPendingById(id, teamId, currentUserId);

        sendInvitation(invitation);
    }

    public TeamInvitation findByToken(String token) {
        TeamInvitation invitation = teamInvitationRepository.findByToken(token);
        if (invitation == null) {
            throw new TeamInvitationNotFoundException(token);
        }

        return cancelIfExpired(invitation);
    }

    @Transactional
    public TeamInvitation accept(String token, UUID currentUserId) {
        TeamInvitation invitation = findPendingByToken(token);

        teamMemberService.addMember(invitation.getTeamId(), currentUserId);

        return teamInvitationRepository.updateStatus(invitation.getId(), TeamInvitationStatus.ACCEPTED, Instant.now());
    }

    public TeamInvitation decline(String token, UUID currentUserId) {
        TeamInvitation invitation = findPendingByToken(token);

        userService.findById(currentUserId);

        return teamInvitationRepository.updateStatus(invitation.getId(), TeamInvitationStatus.DECLINED, Instant.now());
    }

    public TeamInvitation cancel(Long id, Long teamId, UUID currentUserId) {
        TeamInvitation invitation = findPendingById(id, teamId, currentUserId);

        return teamInvitationRepository.updateStatus(invitation.getId(), TeamInvitationStatus.CANCELED, Instant.now());
    }

    public TeamInvitation findPendingByToken(String token) {
        TeamInvitation invitation = findByToken(token);
        ensurePending(invitation);

        return invitation;
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
            return teamInvitationRepository.updateStatus(
                    invitation.getId(), TeamInvitationStatus.CANCELED, Instant.now());
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
