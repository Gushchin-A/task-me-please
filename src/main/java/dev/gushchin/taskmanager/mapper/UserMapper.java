package dev.gushchin.taskmanager.mapper;

import dev.gushchin.taskmanager.jooq.tables.records.UsersRecord;
import dev.gushchin.taskmanager.model.User;

public final class UserMapper {

    private UserMapper() {}

    public static User toModel(UsersRecord record) {
        if (record == null) {
            return null;
        }

        return new User(
                record.getId(),
                record.getEmail(),
                record.getName(),
                record.getPasswordHash(),
                record.getCreatedAt().toInstant(),
                record.getUpdatedAt().toInstant(),
                Boolean.TRUE.equals(record.getIsDeleted()));
    }
}
