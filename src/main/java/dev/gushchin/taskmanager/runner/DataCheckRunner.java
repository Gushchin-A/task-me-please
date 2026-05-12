package dev.gushchin.taskmanager.runner;

import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.model.TeamMember;
import dev.gushchin.taskmanager.model.TeamMemberRole;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.repository.TeamMemberRepository;
import dev.gushchin.taskmanager.repository.TeamRepository;
import dev.gushchin.taskmanager.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataCheckRunner implements CommandLineRunner {
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Override
    public void run(String... args) {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        User user = new User(
                userId, "test@example-test12.com", "test@example-test12.com", "hash-password", now, now, false);

        User savedUser = userRepository.save(user);
        System.out.println("Saved user: " + savedUser.getId());

        Team team = new Team(null, "My First Team Chukaripa", savedUser.getId(), now, now, false);

        Team savedTeam = teamRepository.save(team);
        System.out.println("Saved team: " + savedTeam.getId());

        TeamMember teamMember =
                new TeamMember(null, savedTeam.getId(), savedUser.getId(), TeamMemberRole.OWNER, now, now, now, false);

        TeamMember savedTeamMember = teamMemberRepository.save(teamMember);
        System.out.println("Saved team member: " + savedTeamMember.getId());

        User foundUser = userRepository.findById(savedUser.getId());
        Team foundTeam = teamRepository.findById(savedTeam.getId());

        System.out.println("Found user email: " + foundUser.getEmail());
        System.out.println("Found team name: " + foundTeam.getName());
        System.out.println("Team members count: "
                + teamMemberRepository.findByTeamId(savedTeam.getId()).size());
    }
}
