package dev.gushchin.taskmanager.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TeamInvitation {
    private Long id;
    private Long teamId;
    private UUID invitedBy;
    private String invitedEmail;
    private String token;
    private TeamInvitationStatus status;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean deleted;
}
