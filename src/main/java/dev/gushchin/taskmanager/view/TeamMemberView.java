package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TeamMemberRole;

public record TeamMemberView(
        String name, String email, TeamMemberRole role, long authorTasksCount, long assigneeTasksCount) {}
