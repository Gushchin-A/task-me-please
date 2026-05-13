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
public class TeamMember {
    private Long id;
    private Long teamId;
    private UUID userId;
    private TeamMemberRole role;
    private Instant joinedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean deleted;

    public TeamMember(
            Long teamId,
            UUID userId,
            TeamMemberRole role,
            Instant joinedAt,
            Instant createdAt,
            Instant updatedAt,
            boolean deleted) {
        this.teamId = teamId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = joinedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }
}
