package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TeamMemberRole;
import dev.gushchin.taskmanager.model.TeamTaskVisibility;
import java.util.UUID;

public record TeamMemberView(
        UUID userId,
        String name,
        String email,
        TeamMemberRole role,
        long authorTasksCount,
        long assigneeTasksCount,
        TeamTaskVisibility taskVisibility) {}
