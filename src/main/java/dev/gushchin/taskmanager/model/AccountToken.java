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
public class AccountToken {
    private Long id;
    private UUID userId;
    private AccountTokenType type;
    private String tokenHash;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant invalidatedAt;
    private Instant createdAt;
}
