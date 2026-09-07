package dev.gushchin.taskmanager.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskCreateRequest {
    @NotNull
    private Long teamId;

    @NotNull
    private UUID assigneeId;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private LocalDate deadlineDate;

    @NotNull
    private Long tagId;
}
