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
public class Comment {
    private Long id;
    private Long taskId;
    private UUID userId;
    private String message;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean deleted;
}
