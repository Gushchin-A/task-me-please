package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.dto.CreateUserRequest;
import dev.gushchin.taskmanager.dto.UserResponse;
import dev.gushchin.taskmanager.mapper.UserResponseMapper;
import dev.gushchin.taskmanager.model.User;
import dev.gushchin.taskmanager.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public UserResponse create(@RequestBody @Valid CreateUserRequest request) {
        User user = userService.create(request.email(), request.name(), request.password());
        return UserResponseMapper.toResponse(user);
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable UUID id) {
        User user = userService.findById(id);
        return UserResponseMapper.toResponse(user);
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll().stream()
                .map(UserResponseMapper::toResponse)
                .toList();
    }
}
