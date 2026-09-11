package net.pchinese.vocabulary;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class F09SchemaMigrationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void migrationCreatesF09TablesIndexesAndConstraintsCleanly() throws Exception {
        String migrations = Path.of(System.getProperty("user.dir"), "..", "supabase", "migrations")
                .normalize().toAbsolutePath().toString().replace('\\', '/');
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("filesystem:" + migrations)
                .sqlMigrationPrefix("")
                .sqlMigrationSeparator("_")
                .load();

        assertTrue(flyway.migrate().migrationsExecuted >= 1);

        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            // Verify dictionary_search_keys table exists
            try (ResultSet tables = connection.getMetaData().getTables(null, null, "dictionary_search_keys", new String[]{"TABLE"})) {
                assertTrue(tables.next(), "Table dictionary_search_keys must exist");
            }

            // Verify uq_saved_words_user_entry constraint exists
            try (Statement statement = connection.createStatement();
                 ResultSet constraints = statement.executeQuery("""
                         SELECT conname FROM pg_constraint WHERE conname = 'uq_saved_words_user_entry'
                         """)) {
                assertTrue(constraints.next(), "Constraint uq_saved_words_user_entry must exist");
            }

            // Verify ix_saved_words_capacity index exists
            try (ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, "saved_words", false, false)) {
                boolean foundCapacityIndex = false;
                while (indexes.next()) {
                    if ("ix_saved_words_capacity".equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                        foundCapacityIndex = true;
                        break;
                    }
                }
                assertTrue(foundCapacityIndex, "Index ix_saved_words_capacity must exist");
            }

            // Verify uq_user_entitlements_active_user partial index exists
            try (ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, "user_entitlements", false, false)) {
                boolean foundActiveEntitlementIndex = false;
                while (indexes.next()) {
                    if ("uq_user_entitlements_active_user".equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                        foundActiveEntitlementIndex = true;
                        break;
                    }
                }
                assertTrue(foundActiveEntitlementIndex, "Index uq_user_entitlements_active_user must exist");
            }
        }
    }
}
