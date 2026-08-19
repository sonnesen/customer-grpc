package com.sonnesen;

import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * Builds the {@link Jdbi} instance used by the server, applying Flyway
 * migrations against the configured PostgreSQL database on startup.
 *
 * <p>Connection settings are read from environment variables, falling back
 * to sane defaults for local development (see {@code docker-compose.yml}).
 */
public final class Database {

    private Database() {
    }

    public static Jdbi bootstrap() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerNames(new String[] { env("DB_HOST", "localhost") });
        dataSource.setPortNumbers(new int[] { Integer.parseInt(env("DB_PORT", "5432")) });
        dataSource.setDatabaseName(env("DB_NAME", "customersdb"));
        dataSource.setUser(env("DB_USER", "user"));
        dataSource.setPassword(env("DB_PASSWORD", "password"));

        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate();

        return Jdbi.create(dataSource)
            .installPlugin(new PostgresPlugin());
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
