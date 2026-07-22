package dev.gushchin.taskmanager.view;

import dev.gushchin.taskmanager.model.TeamTag;
import java.util.List;

public record TaskWithTeamView(
        TaskView task, Long teamId, String teamName, List<TaskParticipantView> members, List<TeamTag> tags) {}
