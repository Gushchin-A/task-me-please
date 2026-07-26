package dev.gushchin.taskmanager.repository;

import static dev.gushchin.taskmanager.jooq.Tables.PERSISTENT_LOGINS;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PersistentLoginRepository {
    private final DSLContext dsl;

    public void deleteByUsername(String username) {
        dsl.deleteFrom(PERSISTENT_LOGINS)
                .where(PERSISTENT_LOGINS.USERNAME.eq(username))
                .execute();
    }
}
