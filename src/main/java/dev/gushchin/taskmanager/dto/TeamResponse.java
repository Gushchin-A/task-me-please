package dev.gushchin.taskmanager.dto;

import java.time.Instant;
import java.util.UUID;

public record TeamResponse(
        Long id,
        String name,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        boolean deleted
) {
}
