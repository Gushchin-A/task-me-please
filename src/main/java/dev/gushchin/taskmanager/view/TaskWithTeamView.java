package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.User;
import java.util.List;

public record TaskWithTeamView(TaskView task, Long teamId, String teamName, List<User> members) {}
