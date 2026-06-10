package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.dto.CreateTeamRequest;
import dev.gushchin.taskmanager.dto.TeamResponse;
import dev.gushchin.taskmanager.mapper.TeamResponseMapper;
import dev.gushchin.taskmanager.model.Team;
import dev.gushchin.taskmanager.service.TeamService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;

    @PostMapping
    public TeamResponse create(@RequestBody @Valid CreateTeamRequest request) {
        Team team = teamService.create(request.name(), request.createdBy());
        return TeamResponseMapper.toResponse(team);
    }

    @GetMapping("/{id}")
    public TeamResponse findById(@PathVariable Long id) {
        Team team = teamService.findById(id);
        return TeamResponseMapper.toResponse(team);
    }

    @GetMapping
    public List<TeamResponse> findAll() {
        return teamService.findAll().stream()
                .map(TeamResponseMapper::toResponse)
                .toList();
    }
}
