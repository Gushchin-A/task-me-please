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
public class Team {
    private Long id;
    private String name;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean deleted;

    public Team(String name, UUID createdBy, Instant createdAt, Instant updatedAt, boolean deleted) {
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }
}
