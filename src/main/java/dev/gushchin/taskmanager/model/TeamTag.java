package dev.gushchin.taskmanager.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TeamTag {
    private Long id;
    private Long teamId;
    private String name;
    private String normalizedName;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean deleted;

    public TeamTag(
            Long teamId, String name, String normalizedName, Instant createdAt, Instant updatedAt, boolean deleted) {
        this.teamId = teamId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }
}
