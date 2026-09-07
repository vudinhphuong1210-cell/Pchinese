package net.pchinese;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class MigrationIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void cleanPostgresDatabaseAppliesTheF01SchemaAndConstraints() throws Exception {
        Flyway flyway = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load();
        assertEquals(1, flyway.migrate().migrationsExecuted);
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var tables = connection.getMetaData().getTables(null, null, "auth_sessions", new String[] {"TABLE"})) {
            assertTrue(tables.next());
            try (var indexes = connection.getMetaData().getIndexInfo(null, null, "user_roles", false, false)) {
                boolean foundActiveRoleIndex = false;
                while (indexes.next()) foundActiveRoleIndex |= "user_roles_active_role_unique".equalsIgnoreCase(indexes.getString("INDEX_NAME"));
                assertTrue(foundActiveRoleIndex);
            }
        }
    }
}
