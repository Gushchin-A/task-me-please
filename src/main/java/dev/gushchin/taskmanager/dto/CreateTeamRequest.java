package dev.gushchin.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTeamRequest(
        @NotBlank
        String name,

        @NotNull
        UUID createdBy
) {
}
