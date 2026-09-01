package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.model.Team;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final TeamService teamService;
    private final UserService userService;

    public boolean hasOwnedTeams(UUID userId) {
        return !teamService.findOwnedByUserId(userId).isEmpty();
    }

    @Transactional
    public void delete(UUID userId) {
        List<Team> ownedTeams = teamService.findOwnedByUserId(userId);
        ownedTeams.forEach(team -> teamService.delete(team.getId(), userId));
        userService.deleteById(userId);
    }
}
