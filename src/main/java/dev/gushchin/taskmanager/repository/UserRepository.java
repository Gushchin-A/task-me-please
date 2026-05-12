package dev.gushchin.taskmanager.repository;

import static dev.gushchin.taskmanager.jooq.Tables.USERS;

import dev.gushchin.taskmanager.jooq.tables.records.UsersRecord;
import dev.gushchin.taskmanager.mapper.UserMapper;
import dev.gushchin.taskmanager.model.User;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepository {
    private final DSLContext dsl;

    public User findById(UUID id) {
        UsersRecord record = dsl.selectFrom(USERS).where(USERS.ID.eq(id)).fetchOne();

        return UserMapper.toModel(record);
    }

    public User findByEmail(String email) {
        UsersRecord record = dsl.selectFrom(USERS).where(USERS.EMAIL.eq(email)).fetchOne();

        return UserMapper.toModel(record);
    }

    public List<User> findAll() {
        return dsl.selectFrom(USERS).fetch().map(UserMapper::toModel);
    }

    public User save(User user) {
        UsersRecord record = dsl.insertInto(USERS)
                .set(USERS.ID, user.getId())
                .set(USERS.EMAIL, user.getEmail())
                .set(USERS.NAME, user.getName())
                .set(USERS.PASSWORD_HASH, user.getPasswordHash())
                .set(USERS.CREATED_AT, user.getCreatedAt().atOffset(ZoneOffset.UTC))
                .set(USERS.UPDATED_AT, user.getUpdatedAt().atOffset(ZoneOffset.UTC))
                .set(USERS.IS_DELETED, user.isDeleted())
                .returning()
                .fetchOne();

        return UserMapper.toModel(record);
    }
}
