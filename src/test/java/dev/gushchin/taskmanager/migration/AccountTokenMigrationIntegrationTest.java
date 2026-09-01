package dev.gushchin.taskmanager.migration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        migrateToVersionThirteen();
        UUID consumedTokenUserId = insertUser("consumed-token@test.com");
        UUID replacedTokenUserId = insertUser("replaced-token@test.com");
        OffsetDateTime now = OffsetDateTime.now();
        insertVerificationToken(consumedTokenUserId, "consumed-token", now.minusHours(3), now.minusHours(2));
        insertVerificationToken(replacedTokenUserId, "replaced-token", now.minusHours(3), now.minusHours(2));
        insertVerificationToken(replacedTokenUserId, "newer-token", now.minusHours(1), null);

        migrateToLatestVersion();
        UUID newUserId = insertUser("new@test.com");

        try (Connection connection = createConnection()) {
            assertTrue(findEmailVerified(connection, existingUserId));
            assertNotNull(findEmailVerifiedAt(connection, existingUserId));
            assertFalse(findEmailVerified(connection, newUserId));
            TokenTimestamps consumedToken = findTokenTimestamps(connection, "consumed-token");
            TokenTimestamps replacedToken = findTokenTimestamps(connection, "replaced-token");
            assertNotNull(consumedToken.usedAt());
            assertNull(consumedToken.invalidatedAt());
            assertNull(replacedToken.usedAt());
            assertNotNull(replacedToken.invalidatedAt());
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

    private void migrateToVersionThirteen() {
        Flyway.configure()
                .dataSource(
                        POSTGRES_CONTAINER.getJdbcUrl(),
                        POSTGRES_CONTAINER.getUsername(),
                        POSTGRES_CONTAINER.getPassword())
                .target("13")
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

    private void insertVerificationToken(UUID userId, String tokenHash, OffsetDateTime createdAt, OffsetDateTime usedAt)
            throws SQLException {
        String sql =
                """
                INSERT INTO account_tokens (user_id, type, token_hash, expires_at, used_at, created_at)
                VALUES (?, 'EMAIL_VERIFICATION', ?, ?, ?, ?)
                """;

        try (Connection connection = createConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            statement.setString(2, tokenHash);
            statement.setObject(3, createdAt.plusDays(1));
            statement.setObject(4, usedAt);
            statement.setObject(5, createdAt);
            statement.executeUpdate();
        }
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

    private TokenTimestamps findTokenTimestamps(Connection connection, String tokenHash) throws SQLException {
        String sql = "SELECT used_at, invalidated_at FROM account_tokens WHERE token_hash = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Account token not found");
                }
                return new TokenTimestamps(
                        resultSet.getObject("used_at", OffsetDateTime.class),
                        resultSet.getObject("invalidated_at", OffsetDateTime.class));
            }
        }
    }

    private Connection createConnection() throws SQLException {
        return java.sql.DriverManager.getConnection(
                POSTGRES_CONTAINER.getJdbcUrl(), POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword());
    }

    private record TokenTimestamps(OffsetDateTime usedAt, OffsetDateTime invalidatedAt) {}
}
