package dev.gushchin.taskmanager.model;

import java.util.UUID;

public record TaskParticipantIds(UUID authorUserId, UUID assigneeUserId) {}
