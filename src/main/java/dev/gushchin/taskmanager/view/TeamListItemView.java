package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.Team;

public record TeamListItemView(Team team, boolean owner, int taskCount) {}
