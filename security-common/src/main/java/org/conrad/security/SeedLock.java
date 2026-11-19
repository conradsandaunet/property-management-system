package org.conrad.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Lets one instance at a time run start-up seeding. "Is the table empty? then fill it" is not safe
 * when several instances of a service start together: all of them see an empty table. This takes a
 * Postgres advisory lock for the duration of the action, so the second instance waits, then finds the
 * data and skips. The lock belongs to the connection, so it is released if the instance dies.
 *
 * Only created in services that have a database (the gateway doesn't, so it is skipped there).
 */
@Component
@ConditionalOnClass(name = "org.springframework.jdbc.core.JdbcTemplate")
public class SeedLock {

    // Any constant, as long as every instance of every seeder asks for the same one. Each service
    // has its own database, so the services don't share the lock.
    private static final long LOCK_ID = 74_200_001L;

    @FunctionalInterface
    public interface Action {
        void run() throws Exception;
    }

    private final DataSource dataSource;

    public SeedLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void runExclusively(Action action) {
        try (Connection connection = dataSource.getConnection()) {
            execute(connection, "select pg_advisory_lock(" + LOCK_ID + ")");
            try {
                action.run();
            } finally {
                execute(connection, "select pg_advisory_unlock(" + LOCK_ID + ")");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Seeding failed", e);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
