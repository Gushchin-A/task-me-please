package dev.gushchin.taskmanager.mapper;

import dev.gushchin.taskmanager.dto.UserResponse;
import dev.gushchin.taskmanager.model.User;

public class UserResponseMapper {

    private UserResponseMapper() {
    }

    public static UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.isDeleted()
        );
    }
}
