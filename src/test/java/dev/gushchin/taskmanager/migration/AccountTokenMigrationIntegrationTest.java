package dev.gushchin.taskmanager.migration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class AccountTokenMigrationIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("migration_test_db")
            .withUsername("migration_test_user")
            .withPassword("migration_test_password");

    @Test
    void migrationShouldVerifyExistingUsersAndKeepNewUsersUnverifiedByDefault() throws SQLException {
        migrateToVersionNine();
        UUID existingUserId = insertUser("existing@test.com");

        migrateToLatestVersion();
        UUID newUserId = insertUser("new@test.com");

        try (Connection connection = createConnection()) {
            assertTrue(findEmailVerified(connection, existingUserId));
            assertNotNull(findEmailVerifiedAt(connection, existingUserId));
            assertFalse(findEmailVerified(connection, newUserId));
        }
    }

    private void migrateToVersionNine() {
        Flyway.configure()
                .dataSource(
                        POSTGRES_CONTAINER.getJdbcUrl(),
                        POSTGRES_CONTAINER.getUsername(),
                        POSTGRES_CONTAINER.getPassword())
                .target("9")
                .load()
                .migrate();
    }

    private void migrateToLatestVersion() {
        Flyway.configure()
                .dataSource(
                        POSTGRES_CONTAINER.getJdbcUrl(),
                        POSTGRES_CONTAINER.getUsername(),
                        POSTGRES_CONTAINER.getPassword())
                .load()
                .migrate();
    }

    private UUID insertUser(String email) throws SQLException {
        UUID userId = UUID.randomUUID();
        String sql =
                """
                INSERT INTO users (id, email, name, password_hash, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        OffsetDateTime now = OffsetDateTime.now();

        try (Connection connection = createConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            statement.setString(2, email);
            statement.setString(3, email);
            statement.setString(4, "password-hash");
            statement.setObject(5, now);
            statement.setObject(6, now);
            statement.executeUpdate();
        }

        return userId;
    }

    private boolean findEmailVerified(Connection connection, UUID userId) throws SQLException {
        String sql = "SELECT email_verified FROM users WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("User not found");
                }
                return resultSet.getBoolean("email_verified");
            }
        }
    }

    private OffsetDateTime findEmailVerifiedAt(Connection connection, UUID userId) throws SQLException {
        String sql = "SELECT email_verified_at FROM users WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("User not found");
                }
                return resultSet.getObject("email_verified_at", OffsetDateTime.class);
            }
        }
    }

    private Connection createConnection() throws SQLException {
        return java.sql.DriverManager.getConnection(
                POSTGRES_CONTAINER.getJdbcUrl(), POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword());
    }
}
