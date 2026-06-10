package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.model.Task;
import dev.gushchin.taskmanager.model.TaskStatus;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.TaskService;
import dev.gushchin.taskmanager.service.TeamService;
import dev.gushchin.taskmanager.view.TeamTasksStats;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class TeamPageController {
    private final TeamService teamService;
    private final TaskService taskService;

    @GetMapping("/teams")
    public String teamsPage(@AuthenticationPrincipal AuthUser authUser, Model model) {
        List<Team> teams = teamService.findByUserId(authUser.getId());
        model.addAttribute("teams", teams);

        return "teams/index";
    }

    @GetMapping("/teams/new")
    public String newTeamPage(Model model, CsrfToken csrfToken) {
        model.addAttribute("_csrf", csrfToken);

        return "teams/new";
    }

    @GetMapping("/teams/{id}")
    public String showTeam(@PathVariable Long id, @RequestParam(required = false) TaskStatus status, Model model) {
        Team team = teamService.findById(id);

        List<Task> allTasks = taskService.findByTeamId(id);
        TeamTasksStats stats = taskService.getStats(allTasks);
        List<Task> filteredTasks = taskService.filterByStatus(allTasks, status);

        model.addAttribute("team", team);
        model.addAttribute("tasks", filteredTasks);
        model.addAttribute("stats", stats);
        model.addAttribute("selectedStatus", status);

        return "teams/show";
    }

    @PostMapping("/teams")
    public String createTeam(@AuthenticationPrincipal AuthUser authUser, @RequestParam String name) {
        teamService.create(name, authUser.getId());

        return "redirect:/teams";
    }
}
