package dev.gushchin.taskmanager.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Comment {
    private Long id;
    private Long taskId;
    private UUID userId;
    private String message;
    private boolean deleted;
    private Instant createdAt;
    private Instant updatedAt;
}
