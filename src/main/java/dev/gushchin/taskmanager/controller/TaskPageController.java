package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.model.TaskCategory;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.security.AuthUser;
import dev.gushchin.taskmanager.service.TaskService;
import dev.gushchin.taskmanager.service.TeamService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class TaskPageController {
    private final TeamService teamService;
    private final TaskService taskService;

    @GetMapping("/tasks")
    public String tasksPage(@AuthenticationPrincipal AuthUser authUser, Model model) {
        List<Team> teams = teamService.findByUserId(authUser.getId());
        model.addAttribute("teams", teams);

        return "tasks/index";
    }

    @GetMapping("/tasks/new")
    public String newTaskPage(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) Long teamId,
            Model model,
            CsrfToken csrfToken) {
        List<Team> teams = teamService.findByUserId(authUser.getId());

        model.addAttribute("teams", teams);
        model.addAttribute("selectedTeamId", teamId);
        model.addAttribute("_csrf", csrfToken);
        model.addAttribute("categories", TaskCategory.values());

        return "tasks/new";
    }

    @PostMapping("/tasks")
    public String createTask(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam Long teamId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) LocalDate deadlineDate,
            @RequestParam TaskCategory category) {
        taskService.create(teamId, authUser.getId(), authUser.getId(), title, description, deadlineDate, category);

        return "redirect:/teams/" + teamId;
    }
}
