package dev.gushchin.taskmanager.runner;

import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.service.TeamMemberService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataCheckRunner implements CommandLineRunner {
    private final UserService userService;
    private final TeamService teamService;
    private final TeamMemberService teamMemberService;

    private final String EMAIL = "emailTestTest@e.com";
    private final String PASSWORD = "qwerty";
    private final String TEAM_NAME = "Chukaripa";

    @Override
    public void run(String... args) {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        User user = userService.create(EMAIL, PASSWORD);
        System.out.println("Created user: " + userService.findById(user.getId()).getId());
        System.out.println("Is delete? " + userService.findById(user.getId()).isDeleted());
        userService.deleteById(user.getId());
        System.out.println("Is delete? " + userService.findById(user.getId()).isDeleted());

        Team team = teamService.create(TEAM_NAME, user.getId());
        System.out.println(
                "Created team, ID = " + teamService.findById(team.getId()).getId());
        System.out.println("Owner team " + "\'" + team.getName() + "\'" + " "
                + userService.findById(team.getCreatedBy()).getName());
        TeamMember teamMember = teamMemberService.findById(team.getId(), user.getId());
        System.out.println("His role = " + teamMember.getRole().name());

        List<TeamMember> memberList = teamMemberService.findByTeamId(team.getId());
        System.out.println("Size list members = " + memberList.size());
        System.out.println("Add 1 new member");

        User user2 = userService.create("testTestTest@eee.com", "zxcv");
        teamMemberService.addMember(team.getId(), user2.getId());
        System.out.println("Now list members = "
                + teamMemberService.findByTeamId(team.getId()).size());
    }
}
