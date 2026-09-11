package net.pchinese;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class MigrationIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void cleanPostgresDatabaseAppliesTheCanonicalSupabaseSchemaAndConstraints() throws Exception {
        String migrations = Path.of(System.getProperty("user.dir"), "..", "supabase", "migrations")
                .normalize().toAbsolutePath().toString().replace('\\', '/');
        Flyway flyway = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("filesystem:" + migrations).sqlMigrationPrefix("").sqlMigrationSeparator("_").load();
        assertTrue(flyway.migrate().migrationsExecuted >= 2);
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var tables = connection.getMetaData().getTables(null, null, "auth_sessions", new String[] {"TABLE"})) {
            assertTrue(tables.next());
            try (var profileColumns = connection.getMetaData().getColumns(null, null, "users", "daily_goal_minutes")) {
                assertTrue(profileColumns.next());
            }
            try (var statement = connection.createStatement();
                 var constraints = statement.executeQuery("""
                         select pg_get_constraintdef(oid)
                         from pg_constraint
                         where conname = 'ck_users_target_hsk_level'
                         """)) {
                assertTrue(constraints.next());
                assertTrue(constraints.getString(1).contains("target_hsk_level BETWEEN 1 AND 6"));
            }
            try (var indexes = connection.getMetaData().getIndexInfo(null, null, "user_roles", false, false)) {
                boolean foundActiveRoleIndex = false;
                while (indexes.next()) foundActiveRoleIndex |= "uq_user_roles_active_grant".equalsIgnoreCase(indexes.getString("INDEX_NAME"));
                assertTrue(foundActiveRoleIndex);
            }
            try (var auditTables = connection.getMetaData().getTables(null, null, "content_audit_events", new String[] {"TABLE"})) {
                assertTrue(auditTables.next());
            }
            try (var statement = connection.createStatement();
                 var triggers = statement.executeQuery("""
                         select tgname
                         from pg_trigger
                         where tgrelid = 'content_audit_events'::regclass
                           and not tgisinternal
                         """)) {
                assertTrue(triggers.next());
                assertTrue("trg_content_audit_events_append_only".equals(triggers.getString(1)));
            }
        }
    }
}
