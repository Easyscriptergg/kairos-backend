package com.kairos.backend.dev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

import com.kairos.backend.KairosBackendApplication;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Runs the API locally without installing PostgreSQL: starts a real
 * PostgreSQL server from the embedded binaries, keeping its data in
 * {@code .local-dev/postgres} so records survive restarts, then starts the
 * application with the {@code local} profile (which seeds a demo account).
 *
 * <pre>./mvnw spring-boot:test-run</pre>
 *
 * Lives in the test sources so the embedded database never ships in the production jar.
 */
public final class LocalDevServer {

    private static final int POSTGRES_PORT = 54329;

    private LocalDevServer() {
    }

    public static void main(String[] args) throws IOException {
        Path home = Path.of(".local-dev");
        Files.createDirectories(home);

        EmbeddedPostgres postgres = EmbeddedPostgres.builder()
            .setDataDirectory(home.resolve("postgres"))
            .setCleanDataDirectory(false)
            .setPort(POSTGRES_PORT)
            .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                postgres.close();
            } catch (IOException ignored) {
                // shutting down anyway
            }
        }));

        System.setProperty("spring.datasource.url", postgres.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "");
        System.setProperty("spring.profiles.active", "local");
        if (System.getenv("KAIROS_JWT_SECRET") == null) {
            System.setProperty("kairos.jwt-secret", localSigningKey(home));
        }

        KairosBackendApplication.main(args);
    }

    /** A random key generated once per machine and kept with the local database, so sign-ins survive restarts. */
    private static String localSigningKey(Path home) throws IOException {
        Path file = home.resolve("jwt-secret");
        if (!Files.exists(file)) {
            byte[] bytes = new byte[48];
            new SecureRandom().nextBytes(bytes);
            Files.writeString(file, Base64.getEncoder().encodeToString(bytes));
        }
        return Files.readString(file).trim();
    }
}
