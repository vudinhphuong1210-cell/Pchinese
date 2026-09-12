package net.pchinese;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
            try (var f12Tables = connection.getMetaData().getTables(null, null, "ai_operational_measurements", new String[] {"TABLE"})) {
                assertTrue(f12Tables.next());
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
            try (var statement = connection.createStatement();
                 var policies = statement.executeQuery("select count(*) from plan_policy_versions where status = 'PUBLISHED' and subscription_plan_id = (select subscription_plan_id from subscription_plans where plan_code = 'FREE')")) {
                assertTrue(policies.next());
                assertEquals(1, policies.getInt(1));
            }
            try (var statement = connection.createStatement();
                 var cycles = statement.executeQuery("select count(*) from entitlement_allowance_cycles where status = 'CURRENT'")) {
                assertTrue(cycles.next());
                assertTrue(cycles.getInt(1) >= 1);
            }
            try (var statement = connection.createStatement();
                 var actor = statement.executeQuery("select user_id from users limit 1")) {
                assertTrue(actor.next());
                String actorId = actor.getString(1);
                String auditId;
                try (var insert = connection.prepareStatement("insert into ai_admin_audit_events (actor_user_id, event_type, target_type, target_id, outcome, occurred_at) values (?, 'POLICY_PUBLISHED', 'PLAN_POLICY', gen_random_uuid(), 'SUCCESS', now()) returning ai_admin_audit_event_id")) {
                    insert.setObject(1, java.util.UUID.fromString(actorId));
                    try (var inserted = insert.executeQuery()) {
                        assertTrue(inserted.next());
                        auditId = inserted.getString(1);
                    }
                }
                try (var update = connection.prepareStatement("update ai_admin_audit_events set outcome = 'SUCCESS' where ai_admin_audit_event_id = ?")) {
                    update.setObject(1, java.util.UUID.fromString(auditId));
                    assertThrows(java.sql.SQLException.class, update::executeUpdate);
                }
            }
        }
    }
}
